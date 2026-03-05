package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.repository.CubingEventRepository
import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
@Service
class EventCrawlerService(
    private val eventRepository: CubingEventRepository
) {
    private val logger = LoggerFactory.getLogger(EventCrawlerService::class.java)
    private val baseUrl = "https://cubing-tw.net/event"
    private val externalUrls = listOf("worldcubeassociation.org", "cubingchina.com", "maru.tw")

    fun crawlNewEvents() {
        logger.info("Starting crawler pass for cubing-tw events...")

        try {
            val document = Jsoup.connect(baseUrl).get()
            val eventElements = document.select("div#nav-tabContent div.d-none.d-sm-block tbody tr")

            for (element in eventElements) {
                val tds = element.select("td")
                if (tds.size < 3) continue

                val rawEventDate = tds[0].text()
                val aTag = tds[1].selectFirst("a") ?: continue
                val name = aTag.text()
                val relativeLink = aTag.attr("href")
                val eventUrl = if (relativeLink.startsWith("http")) relativeLink else "$baseUrl$relativeLink"

                if (!eventRepository.existsByUrl(eventUrl)) {
                    logger.info("Found new event: $name. Fetching registration details...")

                    var registrationTime: LocalDateTime? = null
                    if (externalUrls.none { eventUrl.contains(it) }) {
                        registrationTime = fetchRegistrationTime(eventUrl)
                    }

                    // Extract the start date for database sorting
                    val startDate = extractStartDate(rawEventDate)

                    val newEvent = CubingEvent(
                        url = eventUrl,
                        name = name,
                        eventDate = rawEventDate,
                        startDate = startDate,
                        registrationTime = registrationTime
                    )

                    eventRepository.save(newEvent)
                    logger.info("Saved new event to database: $name")
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while crawling events: ${e.message}")
        }
    }

    private fun extractStartDate(rawDate: String): LocalDate {
        return try {
            // Looks for exactly 4 digits, a slash, 2 digits, a slash, 2 digits (e.g., 2026/03/14)
            val regex = "^(\\d{4}/\\d{2}/\\d{2})".toRegex()
            val match = regex.find(rawDate)

            if (match != null) {
                val dateStr = match.value.replace("/", "-")
                LocalDate.parse(dateStr)
            } else {
                LocalDate.now() // Fallback if parsing completely fails
            }
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    private fun fetchRegistrationTime(eventUrl: String): LocalDateTime? {
        val registrationUrl = "$eventUrl/registration"
        return try {
            val document = Jsoup.connect(registrationUrl).get()
            val timeText = document.select("div.registration-time").text()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime.parse(timeText, formatter)
        } catch (e: DateTimeParseException) {
            // We expect this to fail right now since the CSS selector is a placeholder!
            logger.warn("Could not parse date format for URL (Expected until CSS is fixed): $registrationUrl")
            null
        } catch (e: Exception) {
            logger.error("Failed to load registration page: $registrationUrl")
            null
        }
    }
}
