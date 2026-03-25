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
    private val logger = LoggerFactory.getLogger(EventCrawlerService::class.java)
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

                if (!eventRepository.existsByUrl(eventUrl)) {
                    logger.info("Found new event: $name. Fetching registration details...")

                    var registrationTime: LocalDateTime? = null
                    if (externalUrls.none { eventUrl.contains(it) }) {
                        registrationTime = fetchRegistrationTime(eventUrl)
                    }

                    val startDate = extractStartDate(rawEventDate)
                    if (startDate == null) {
                        logger.error("Skipping event $name due to unparseable date: $rawEventDate")
                        continue // Skip to the next event
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
                            isCreatedNotified = isPastEvent,
                            isRegistrationNotified = isRegistrationPassed,
                            isStartNotified = isPastEvent,
                        )

                    eventRepository.save(newEvent)
                    logger.info("Saved new event to database: $name (Past Event: $isPastEvent)")

                    if (shouldNotifyNewEvent) {
                        logger.info("Dispatching Telegram notification for new event: $name")
                        try {
                            notificationServices.forEach { service ->
                                service.notifyNewEvent(newEvent)
                            }
                            newEvent.isCreatedNotified = true
                            eventRepository.save(newEvent)
                            logger.info("Set event as created-notified after successful notification: $name")
                        } catch (e: Exception) {
                            logger.error("Failed to send Telegram notification for new event: $name", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while crawling events: ${e.message}", e)
        } finally {
            isCrawling.store(false)
        }
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

    private fun fetchRegistrationTime(eventUrl: String): LocalDateTime? {
        val registrationUrl = "$eventUrl/registration"
        return try {
            val document = Jsoup.connect(registrationUrl).get()
            parseRegistrationTimeFromDocument(document)
        } catch (e: Exception) {
            logger.error("Failed to load registration page: $registrationUrl")
            null
        }
    }

    private fun parseRegistrationTimeFromDocument(document: org.jsoup.nodes.Document): LocalDateTime? {
        // Step 1: Check for a valid Reopen Registration Date (not wrapped in <s>)
        val reopenElements = document.select("p:contains(重新開放報名時間：)")
        val validReopenElement = reopenElements.firstOrNull { it.parent()?.tagName() != "s" }

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
                LocalDateTime.parse("$datePart $timePart", formatter)
            } else {
                logger.warn("Found time text but it didn't match the expected Regex format: $timeText")
                null
            }
        } catch (e: DateTimeParseException) {
            logger.warn("Failed to parse extracted time string: $timeText")
            null
        }
    }
}
