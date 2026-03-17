package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.repository.CubingEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationScheduler(
    private val crawlerService: EventCrawlerService,
    private val notificationService: TelegramNotificationService,
    private val eventRepository: CubingEventRepository
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

            notificationService.sendRegistrationOpenNotification(event)

            event.isRegistrationNotified = true
            eventRepository.save(event)
        }
    }
}