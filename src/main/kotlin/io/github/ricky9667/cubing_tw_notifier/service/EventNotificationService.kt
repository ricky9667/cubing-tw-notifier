package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import java.time.format.DateTimeFormatter

private val registrationTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

interface EventNotificationService {
    fun sendNotification(message: String)

    fun notifyNewEvent(event: CubingEvent) {
        sendNotification(
            """
            📢 有新的比賽了! New Competition Announced!

            - 🏆 ${event.name}
            - 📅 ${event.eventDate}
            - ${registrationTime(event)}

            🔗 比賽資訊 Details: ${event.url}
            """.trimIndent(),
        )
    }

    fun notifyRegistrationOpen(event: CubingEvent) {
        sendNotification(
            """
            🚨 報名即將開始! Registration will begin soon!

            - 🏆 ${event.name}
            - ${registrationTime(event)}

            🔗 馬上報名 Register Now: ${event.url}/registration
            """.trimIndent(),
        )
    }

    fun notifyEventStart(event: CubingEvent) {
        sendNotification(
            """
            🎉 比賽即將開始! Event is starting soon!

            - 🏆 ${event.name}
            - 📅 ${event.eventDate}

            🔗 比賽資訊 Details: ${event.url}
            """.trimIndent(),
        )
    }

    private fun registrationTime(event: CubingEvent) =
        "🚨 報名 Registration: ${event.registrationTime?.format(registrationTimeFormat) ?: "待公布"}"
}
