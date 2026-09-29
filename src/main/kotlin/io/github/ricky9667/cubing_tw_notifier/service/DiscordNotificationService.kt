package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.DiscordCommand
import io.github.ricky9667.cubing_tw_notifier.repository.DiscordSubscriptionRepository
import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType
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
            commands
                .first { it.name == DiscordCommand.SUBSCRIBE.eventName }
                .addOption(OptionType.ROLE, "role", "Role to tag in competition alerts", false)
            jda
                .updateCommands()
                .addCommands(commands)
                .queue()

            logger.info("✅ Discord Bot connected and commands registered!")
        } catch (e: Exception) {
            logger.error("❌ Failed to connect to Discord", e)
        }
    }

    override fun sendNotification(message: String) {
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
                val role = sub.roleId?.let { channel.guild.getRoleById(it) }
                val mentionRoleId =
                    role
                        ?.takeIf {
                            !it.isPublicRole &&
                                (it.isMentionable || channel.guild.selfMember.hasPermission(channel, Permission.MESSAGE_MENTION_EVERYONE))
                        }?.id
                val content = if (mentionRoleId != null) "<@&$mentionRoleId>\n\n$message" else message
                val action = channel.sendMessage(content).setAllowedMentions(emptySet())
                if (mentionRoleId != null) action.mentionRoles(mentionRoleId)
                action.queue(
                    null,
                    { error -> logger.error("❌ Failed to send to channel ${sub.channelId}", error) },
                )
            } else {
                logger.warn(
                    "⚠️ Could not find channel ${sub.channelId}. The bot might lack permissions or the channel was deleted. Removing stale subscription.",
                )
                subscriptionRepository.delete(sub)
            }
        }
    }
}
