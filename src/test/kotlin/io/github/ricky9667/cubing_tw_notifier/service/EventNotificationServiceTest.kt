package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventNotificationServiceTest {
    @Test
    fun `shared notification methods send the same plain text to each platform`() {
        val messages = mutableListOf<String>()
        val service =
            object : EventNotificationService {
                override fun sendNotification(message: String) {
                    messages += message
                }
            }
        val event =
            CubingEvent(
                url = "https://cubing-tw.net/event/123",
                name = "Example Competition",
                eventDate = "2026/10/01",
                startDate = LocalDate.of(2026, 10, 1),
                registrationTime = LocalDateTime.of(2026, 9, 30, 12, 0),
            )

        service.notifyNewEvent(event)
        service.notifyRegistrationOpen(event)
        service.notifyEventStart(event)

        assertEquals(
            listOf(
                """
                📢 有新的比賽了! New Competition Announced!

                - 🏆 Example Competition
                - 📅 2026/10/01
                - 🚨 報名 Registration: 2026/09/30 12:00

                🔗 查看比賽資訊 View Event Details: https://cubing-tw.net/event/123
                """.trimIndent(),
                """
                🚨 報名即將開始! Registration will begin soon!

                - 🏆 Example Competition
                - 🚨 報名 Registration: 2026/09/30 12:00

                🔗 馬上報名 Register Now: https://cubing-tw.net/event/123/registration
                """.trimIndent(),
                """
                🎉 比賽即將開始! Event is starting soon!

                - 🏆 Example Competition
                - 📅 2026/10/01

                🔗 查看比賽資訊 View Event Details: https://cubing-tw.net/event/123
                """.trimIndent(),
            ),
            messages,
        )

        event.registrationTime = null
        service.notifyNewEvent(event)
        assertTrue(messages.last().contains("🚨 報名 Registration: 待公布"))
    }
}
