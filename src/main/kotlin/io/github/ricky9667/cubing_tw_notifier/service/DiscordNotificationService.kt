package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import io.github.ricky9667.cubing_tw_notifier.domain.DiscordCommand
import io.github.ricky9667.cubing_tw_notifier.repository.DiscordSubscriptionRepository
import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DiscordNotificationService(
    @Value("\${discord.bot.token}") private val botToken: String,
    private val subscriptionRepository: DiscordSubscriptionRepository,
    private val commandListener: DiscordCommandListener, // Inject our new listener
) : EventNotificationService {
    private val logger = LoggerFactory.getLogger(DiscordNotificationService::class.java)
    private lateinit var jda: JDA

    @PostConstruct
    fun init() {
        if (botToken.isBlank()) {
            logger.warn("⚠️ Discord token missing. Bot will NOT start.")
            return
        }

        try {
            jda =
                JDABuilder
                    .createDefault(botToken)
                    .addEventListeners(commandListener)
                    .build()
                    .awaitReady()

            val commands = DiscordCommand.entries.map { Commands.slash(it.eventName, it.description) }
            jda
                .updateCommands()
                .addCommands(commands)
                .queue()

            logger.info("✅ Discord Bot connected and commands registered!")
        } catch (e: Exception) {
            logger.error("❌ Failed to connect to Discord", e)
        }
    }


    override fun notifyNewEvent(event: CubingEvent) {
        val text =
            """
            📢 **有新的比賽了! New Competition Announced!**
            
            🏆 **比賽名稱 Name**: ${event.name}
            📅 **比賽日期 Date**: ${event.eventDate}
            
            🔗 [查看比賽資訊 View Event Details](${event.url})
            """.trimIndent()

        broadcastMessage(text)
    }

    override fun notifyRegistrationOpen(event: CubingEvent) {
        val text =
            """
            🚨 **報名開始了! Registration is Open!**
            🏆 **比賽名稱 Name**: ${event.name}            
            
            快點開始報名不然要來不及了!
            Hurry up and register before spots fill up!
            🔗 [馬上報名 Register Now](${event.url}/registration)
            """.trimIndent()

        broadcastMessage(text)
    }

    override fun notifyEventStart(event: CubingEvent) {
        val text =
            """
            🎉 **比賽開始了! Event Started!**

            🏆 **比賽名稱 Name**: ${event.name}
            📅 **比賽日期 Date**: ${event.eventDate}

            🔗 [查看比賽資訊 View Event Details](${event.url})
            """.trimIndent()

        broadcastMessage(text)
    }

    private fun broadcastMessage(message: String) {
        if (!this::jda.isInitialized) return

        val subscriptions = subscriptionRepository.findAll()

        if (subscriptions.isEmpty()) {
            logger.info("No Discord servers subscribed yet. Skipping broadcast.")
            return
        }

        logger.info("📢 Broadcasting Discord message to ${subscriptions.size} servers...")

        for (sub in subscriptions) {
            val channel = jda.getTextChannelById(sub.channelId)

            if (channel != null) {
                channel.sendMessage(message).queue(
                    null,
                    { error -> logger.error("❌ Failed to send to channel ${sub.channelId}", error) },
                )
            } else {
                logger.warn("⚠️ Could not find channel ${sub.channelId}. The bot might lack permissions or the channel was deleted.")
            }
        }
    }
}
