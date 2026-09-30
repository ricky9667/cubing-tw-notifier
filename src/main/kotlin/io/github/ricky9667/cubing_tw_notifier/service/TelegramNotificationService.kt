package io.github.ricky9667.cubing_tw_notifier.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Duration

@Service
class TelegramNotificationService(
    @Value("\${telegram.bot.token:}") private val botToken: String,
    @Value("\${telegram.chat.id:}") private val chatId: String,
) : EventNotificationService {
    private val logger = LoggerFactory.getLogger(TelegramNotificationService::class.java)

    private val requestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(5))
            setReadTimeout(Duration.ofSeconds(5))
        }

    private val restClient =
        RestClient
            .builder()
            .baseUrl("https://api.telegram.org")
            .requestFactory(requestFactory)
            .build()

    override fun sendNotification(message: String) {
        if (botToken.isBlank() || chatId.isBlank()) {
            logger.warn("Telegram notification skipped: bot token or chat id is not configured.")
            return
        }

        try {
            val payload =
                mapOf(
                    "chat_id" to chatId,
                    "text" to message,
                )

            restClient
                .post()
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
