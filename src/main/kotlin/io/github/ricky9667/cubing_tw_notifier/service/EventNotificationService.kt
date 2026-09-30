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
        val headline =
            if (event.reopen == 0) {
                "🚨 報名即將開始! Registration will begin soon!"
            } else {
                "🚨 第${chineseOrdinal(event.reopen)}次重新報名即將開始! ${ordinal(event.reopen)} reopened registration will begin soon!"
            }

        sendNotification(
            """
            $headline

            - 🏆 ${event.name}
            - ${registrationTime(event)}

            🔗 馬上報名 Register Now: ${event.url}/registration
            """.trimIndent(),
        )
    }

    fun notifyCompetitionStart(event: CubingEvent) {
        sendNotification(
            """
            🎒 比賽即將開始! Competition is starting soon!

            - 🏆 ${event.name}
            - 📅 ${event.eventDate}

            🔗 比賽資訊 Details: ${event.url}
            """.trimIndent(),
        )
    }

    private fun registrationTime(event: CubingEvent) =
        "🚨 報名 Registration: ${event.registrationTime?.format(registrationTimeFormat) ?: "待公布"}"

    private fun chineseOrdinal(number: Int): String = if (number in 1..9) "一二三四五六七八九"[number - 1].toString() else number.toString()

    private fun ordinal(number: Int) =
        "$number${if (number % 100 in 11..13) {
            "th"
        } else {
            when (number % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }}"
}
