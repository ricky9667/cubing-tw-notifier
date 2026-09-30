package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import io.github.ricky9667.cubing_tw_notifier.repository.CubingEventRepository
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Service
class EventCrawlerService(
    private val eventRepository: CubingEventRepository,
    private val notificationServices: List<EventNotificationService>,
    @Value("\${notification.start.zone}") private val startNotificationZoneId: String,
) {
    private val baseUrl = "https://cubing-tw.net/event"
    private val externalUrls = listOf("worldcubeassociation.org", "cubingchina.com", "maru.tw")
    private val startNotificationZone: ZoneId = ZoneId.of(startNotificationZoneId)

    private val isCrawling = AtomicBoolean(false)

    fun crawlNewEvents() {
        if (!isCrawling.compareAndSet(expectedValue = false, newValue = true)) {
            logger.info("⚠️ Crawl skipped: Another crawl operation is currently in progress.")
            return
        }

        logger.info("Starting crawler pass for cubing-tw events...")

        try {
            val document =
                Jsoup
                    .connect(baseUrl)
                    .userAgent("cubing-tw-notifier/1.0")
                    .timeout(10_000)
                    .get()

            val eventElements = document.select("div#nav-tabContent div.d-none.d-sm-block tbody tr")

            for (element in eventElements) {
                val tds = element.select("td")
                if (tds.size < 3) continue

                val rawEventDate = tds[0].text()
                val aTag = tds[1].selectFirst("a") ?: continue
                val name = aTag.text()
                val relativeLink = aTag.attr("href")
                val eventUrl = if (relativeLink.startsWith("http")) relativeLink else "$baseUrl/$relativeLink"

                val cubingEvent = eventRepository.findByUrl(eventUrl)
                if (cubingEvent == null) {
                    processNewEvent(name, eventUrl, rawEventDate)
                } else if (externalUrls.none { eventUrl.contains(it) }) {
                    checkForRegistrationTimeUpdate(cubingEvent)
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while crawling events: ${e.message}", e)
        } finally {
            isCrawling.store(false)
        }
    }

    private fun processNewEvent(
        name: String,
        eventUrl: String,
        rawEventDate: String,
    ) {
        logger.info("Found new event: $name. Fetching registration details...")

        val registration =
            if (externalUrls.none { eventUrl.contains(it) }) fetchRegistration(eventUrl) else null
        val registrationTime = registration?.time

        val startDate = extractStartDate(rawEventDate)
        if (startDate == null) {
            logger.error("Skipping event $name due to unparseable date: $rawEventDate")
            return
        }

        val currentDateAtStartZone = LocalDate.now(startNotificationZone)
        val shouldNotifyNewEvent = !startDate.isBefore(currentDateAtStartZone)
        val isPastEvent = !shouldNotifyNewEvent
        val isRegistrationPassed = registrationTime?.isBefore(LocalDateTime.now()) ?: isPastEvent
        val newEvent =
            CubingEvent(
                url = eventUrl,
                name = name,
                eventDate = rawEventDate,
                startDate = startDate,
                registrationTime = registrationTime,
                reopen = registration?.reopen ?: 0,
                isCreatedNotified = isPastEvent,
                isRegistrationNotified = isRegistrationPassed,
                isStartNotified = isPastEvent,
            )

        eventRepository.save(newEvent)
        logger.info("Saved new event to database: $name (Past Event: $isPastEvent)")

        if (shouldNotifyNewEvent) {
            logger.info("Dispatching notifications for new event: $name")
            try {
                notificationServices.forEach { service ->
                    service.notifyNewEvent(newEvent)
                }
                newEvent.isCreatedNotified = true
                eventRepository.save(newEvent)
                logger.info("Set event as created-notified after successful notification: $name")
            } catch (e: Exception) {
                logger.error("Failed to send notification for new event: $name", e)
            }
        }
    }

    private fun checkForRegistrationTimeUpdate(cubingEvent: CubingEvent) {
        val updatedRegistration = fetchRegistration(cubingEvent.url) ?: return
        if (cubingEvent.registrationTime == updatedRegistration.time && cubingEvent.reopen == updatedRegistration.reopen) return

        logger.info(
            "Registration updated for '${cubingEvent.name}': ${cubingEvent.registrationTime} -> ${updatedRegistration.time}, " +
                "reopen ${cubingEvent.reopen} -> ${updatedRegistration.reopen}",
        )

        val now = LocalDateTime.now()
        cubingEvent.apply {
            if (registrationTime != updatedRegistration.time) {
                isRegistrationNotified = updatedRegistration.time.isBefore(now)
            }
            registrationTime = updatedRegistration.time
            reopen = updatedRegistration.reopen
        }
        eventRepository.save(cubingEvent)
        logger.info("Saved updated registration time for event: ${cubingEvent.name}")
    }

    private fun extractStartDate(rawDate: String): LocalDate? =
        try {
            // Looks for exactly 4 digits, a slash, 2 digits, a slash, 2 digits (e.g., 2026/03/14)
            val regex = "^(\\d{4}/\\d{2}/\\d{2})".toRegex()
            val match = regex.find(rawDate)

            if (match != null) {
                val dateStr = match.value.replace("/", "-")
                LocalDate.parse(dateStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

    private fun fetchRegistration(eventUrl: String): RegistrationOpening? {
        val registrationUrl = "$eventUrl/registration"
        return try {
            val document = Jsoup.connect(registrationUrl).get()
            parseRegistrationFromDocument(document)
        } catch (e: Exception) {
            logger.error("Failed to load registration page: $registrationUrl")
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventCrawlerService::class.java)

        internal fun parseRegistrationFromDocument(document: org.jsoup.nodes.Document): RegistrationOpening? {
            // Step 1: Check for a valid Reopen Registration Date (not wrapped in <s>)
            val reopenElements = document.select("p:contains(重新開放報名時間：)")
            val validReopenElement = reopenElements.firstOrNull { it.parent()?.tagName() != "s" }

            val reopen =
                if (validReopenElement != null) {
                    val ordinal = Regex("第([一二三四五六七八九十]+|[0-9]+)次重新開放報名時間").find(validReopenElement.text())?.groupValues?.get(1)
                    ordinal?.toIntOrNull()?.takeIf { it > 0 } ?: ordinal?.let(::chineseNumber) ?: return null
                } else {
                    0
                }
            val timeText =
                if (validReopenElement != null) {
                    validReopenElement.text() // e.g., "第二次重新開放報名時間：2025/12/04 (四) 20:00:00 ~ ..."
                } else {
                    // Step 2: Fallback to the standard Registration Date
                    // Find the <h3> header containing "報名時間", and grab the next <p> sibling
                    val headerElement = document.selectFirst("h3:contains(報名時間)")
                    headerElement?.nextElementSibling()?.text() // e.g., "2025/11/25 (二) 20:00:00 ~ ..."
                }

            if (timeText.isNullOrBlank()) {
                logger.warn("Could not find any registration time text on the page.")
                return null
            }

            // Step 3: Extract the first Date/Time using Regex
            // This matches patterns like "2025/11/25 (二) 20:00:00" and captures the date and time groups separately
            val regex = """(\d{4}/\d{2}/\d{2})\s*\([^)]+\)\s*(\d{2}:\d{2}:\d{2})""".toRegex()
            val matchResult = regex.find(timeText)

            return try {
                if (matchResult != null) {
                    // Group 1 is "2025/11/25", Group 2 is "20:00:00"
                    val datePart = matchResult.groupValues[1].replace("/", "-")
                    val timePart = matchResult.groupValues[2]

                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    RegistrationOpening(LocalDateTime.parse("$datePart $timePart", formatter), reopen)
                } else {
                    logger.warn("Found time text but it didn't match the expected Regex format: $timeText")
                    null
                }
            } catch (e: DateTimeParseException) {
                logger.warn("Failed to parse extracted time string: $timeText")
                null
            }
        }

        private fun chineseNumber(value: String): Int? {
            val digits = "一二三四五六七八九"
            val tens = value.split('十')
            if (tens.size > 2) return null
            val high = if (tens.size == 2) tens[0].singleOrNull()?.let { digits.indexOf(it) + 1 } ?: 1 else 0
            val low = tens.last().singleOrNull()?.let { digits.indexOf(it) + 1 } ?: 0
            return (high * 10 + low).takeIf { it > 0 }
        }
    }
}

internal data class RegistrationOpening(
    val time: LocalDateTime,
    val reopen: Int,
)
