package io.github.ricky9667.cubing_tw_notifier.service

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
) {
    private val logger = LoggerFactory.getLogger(DiscordNotificationService::class.java)
    private lateinit var jda: JDA

    @PostConstruct
    fun init() {
        if (botToken.isBlank()) {
            logger.warn("⚠️ Discord token missing. Bot will NOT start.")
            return
        }

        try {
            // Boot the bot and attach our listener
            jda =
                JDABuilder
                    .createDefault(botToken)
                    .addEventListeners(commandListener)
                    .build()
                    .awaitReady()

            // Tell Discord's API that this bot has a "/setchannel" command
            jda
                .updateCommands()
                .addCommands(
                    Commands.slash("setchannel", "Set this channel to receive cubing competition alerts"),
                ).queue()

            logger.info("✅ Discord Bot connected and commands registered!")
        } catch (e: Exception) {
            logger.error("❌ Failed to connect to Discord", e)
        }
    }

    // The new Multi-Tenant Broadcast function
    fun broadcastEventNotification(message: String) {
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
                    null, // Success callback (optional)
                    { error -> logger.error("❌ Failed to send to channel ${sub.channelId}", error) },
                )
            } else {
                logger.warn("⚠️ Could not find channel ${sub.channelId}. The bot might lack permissions or the channel was deleted.")
            }
        }
    }
}
