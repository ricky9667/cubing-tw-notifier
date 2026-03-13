package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class TelegramNotificationService(
    @Value("\${telegram.bot.token:}") private val botToken: String,
    @Value("\${telegram.chat.id:}") private val chatId: String
) {
    private val logger = LoggerFactory.getLogger(TelegramNotificationService::class.java)

    private val restClient = RestClient.create("https://api.telegram.org")

    fun sendNewEventNotification(event: CubingEvent) {
        val text = """
            📢 <b>有新的比賽了! New Competition Announced!</b>
            
            🏆 <b>比賽名稱 Name</b>: ${event.name}
            📅 <b>比賽日期 Date</b>: ${event.eventDate}
            
            🔗 <a href="${event.url}">查看比賽資訊 View Event Details</a>
        """.trimIndent()

        sendMessage(text)
    }

    fun sendRegistrationOpenNotification(event: CubingEvent) {
        val text = """
            🚨 <b>報名開始了! Registration is Open!</b>
            
            🏆 <b>比賽名稱 Name</b>: ${event.name}            
            
            快點開始報名不然要來不及了!
            Hurry up and register before spots fill up!
            🔗 <a href="${event.url}/registration">馬上報名 Register Now</a>
        """.trimIndent()

        sendMessage(text)
    }

    private fun sendMessage(text: String) {
        if (botToken.isBlank() || chatId.isBlank()) {
            logger.warn("Telegram notification skipped: bot token or chat id is not configured.")
            return
        }

        try {
            val payload = mapOf(
                "chat_id" to chatId,
                "text" to text,
                "parse_mode" to "HTML"
            )

            restClient.post()
                .uri("/bot$botToken/sendMessage")
                .body(payload)
                .retrieve()
                .toBodilessEntity()

            logger.info("Successfully sent Telegram notification.")
        } catch (e: Exception) {
            logger.error("Failed to send Telegram notification: ${e.message}", e)
        }
    }
}
