package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.repository.CubingEventRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class NotificationScheduler(
    private val crawlerService: EventCrawlerService,
    private val notificationServices: List<EventNotificationService>,
    private val eventRepository: CubingEventRepository,
    @Value("\${notification.start.zone:Asia/Taipei}") private val startNotificationZone: String,
) {
    private val logger = LoggerFactory.getLogger(NotificationScheduler::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun runOnStartup() {
        logger.info("🚀 Application fully booted! Running initial cold-start crawl...")
        crawlerService.crawlNewEvents()
    }

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
        val pendingEvents = eventRepository.findByRegistrationTimeLessThanEqualAndIsRegistrationNotifiedFalse(now.plusMinutes(5))

        if (pendingEvents.isEmpty()) {
            logger.info("No new registrations to announce right now.")
            return
        }

        for (event in pendingEvents) {
            logger.info("Registration is open for: ${event.name}! Sending notification...")

            try {
                notificationServices.forEach { service ->
                    service.notifyRegistrationOpen(event)
                }

                event.isRegistrationNotified = true
                eventRepository.save(event)
            } catch (exception: Exception) {
                logger.error(
                    "Failed to send registration open notification for event '${event.name}' (id=${event.id}). " +
                        "Will retry on next scheduler run.",
                    exception,
                )
            }
        }
    }

    // Notify at 8 AM local time: one day before and on the day of the event.
    @Scheduled(cron = "0 0 8 * * *", zone = "\${notification.start.zone:Asia/Taipei}")
    fun checkEventStarts() {
        val today = currentDateInNotificationZone()
        val tomorrow = today.plusDays(1)
        logger.info("⏰ Checking for events starting today ($today) or tomorrow ($tomorrow) in zone $startNotificationZone...")

        val dayBeforeEvents = eventRepository.findByStartDateAndIsOneDayBeforeStartNotifiedFalse(tomorrow)
        val todayEvents = eventRepository.findByStartDateAndIsStartNotifiedFalse(today)

        if (dayBeforeEvents.isEmpty() && todayEvents.isEmpty()) {
            logger.info("No starting events to announce right now.")
            return
        }

        for (event in dayBeforeEvents) {
            logger.info("Event starts tomorrow: ${event.name}. Sending one-day-before notification...")
            try {
                notificationServices.forEach { service ->
                    service.notifyEventStart(event)
                }
                event.isOneDayBeforeStartNotified = true
                eventRepository.save(event)
            } catch (exception: Exception) {
                logger.error(
                    "Failed to send one-day-before start notification for event '${event.name}' (id=${event.id}). " +
                        "Will retry on next scheduler run.",
                    exception,
                )
            }
        }

        for (event in todayEvents) {
            logger.info("Event starts today: ${event.name}. Sending start notification...")

            try {
                notificationServices.forEach { service ->
                    service.notifyEventStart(event)
                }
                event.isStartNotified = true
                eventRepository.save(event)
            } catch (exception: Exception) {
                logger.error(
                    "Failed to send event start notification for event '${event.name}' (id=${event.id}). " +
                        "Will retry on next scheduler run.",
                    exception,
                )
            }
        }
    }

    private fun currentDateInNotificationZone(): LocalDate =
        try {
            LocalDate.now(ZoneId.of(startNotificationZone))
        } catch (exception: Exception) {
            logger.warn("Invalid notification.start.zone '$startNotificationZone'. Falling back to system default zone.")
            LocalDate.now()
        }
}
