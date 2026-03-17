package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.repository.CubingEventRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class NotificationScheduler(
    private val crawlerService: EventCrawlerService,
    private val notificationService: TelegramNotificationService,
    private val eventRepository: CubingEventRepository,
    @Value("\${notification.start.zone:Asia/Taipei}") private val startNotificationZone: String
) {
    private val logger = LoggerFactory.getLogger(NotificationScheduler::class.java)

    // Run the crawler every hour (Cron format: Top of every hour)
    @Scheduled(cron = "0 0 * * * *")
    fun scheduleCrawl() {
        logger.info("⏰ Triggering scheduled hourly crawl...")
        crawlerService.crawlNewEvents()
    }

    // Check for open registrations every 5 minutes (300,000 milliseconds)
    @Scheduled(fixedRate = 300000)
    fun checkRegistrationOpenings() {
        logger.info("⏰ Checking for newly opened registrations...")

        val now = LocalDateTime.now()
        val pendingEvents = eventRepository.findByRegistrationTimeBeforeAndIsRegistrationNotifiedFalse(now)

        if (pendingEvents.isEmpty()) {
            logger.info("No new registrations to announce right now.")
            return
        }

        for (event in pendingEvents) {
            logger.info("Registration is open for: ${event.name}! Sending notification...")

            try {
                notificationService.sendRegistrationOpenNotification(event)

                event.isRegistrationNotified = true
                eventRepository.save(event)
            } catch (exception: Exception) {
                logger.error(
                    "Failed to send registration open notification for event '${event.name}' (id=${event.id}). " +
                            "Will retry on next scheduler run.",
                    exception
                )
            }
        }
    }

    // Notify at 9 AM local time on each event's first start date.
    @Scheduled(cron = "0 0 9 * * *", zone = "\${notification.start.zone:Asia/Taipei}")
    fun checkEventStarts() {
        val notificationDate = currentDateInNotificationZone()
        logger.info("⏰ Checking for events starting today ($notificationDate) in zone $startNotificationZone...")

        val pendingEvents = eventRepository.findByStartDateAndIsStartNotifiedFalse(notificationDate)

        if (pendingEvents.isEmpty()) {
            logger.info("No starting events to announce right now.")
            return
        }

        for (event in pendingEvents) {
            logger.info("Event starts today: ${event.name}. Sending start notification...")

            try {
                notificationService.sendEventStartedNotification(event)

                event.isStartNotified = true
                eventRepository.save(event)
            } catch (exception: Exception) {
                logger.error(
                    "Failed to send event start notification for event '${event.name}' (id=${event.id}). " +
                            "Will retry on next scheduler run.",
                    exception
                )
            }
        }
    }

    private fun currentDateInNotificationZone(): LocalDate {
        return try {
            LocalDate.now(ZoneId.of(startNotificationZone))
        } catch (exception: Exception) {
            logger.warn("Invalid notification.start.zone '$startNotificationZone'. Falling back to system default zone.")
            LocalDate.now()
        }
    }
}
