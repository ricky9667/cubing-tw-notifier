package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.DiscordCommand
import io.github.ricky9667.cubing_tw_notifier.domain.DiscordSubscription
import io.github.ricky9667.cubing_tw_notifier.repository.DiscordSubscriptionRepository
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DiscordCommandListener(
    private val subscriptionRepository: DiscordSubscriptionRepository,
) : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(DiscordCommandListener::class.java)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.member?.hasPermission(Permission.MANAGE_SERVER) != true) {
            event.reply("❌ You must be an Administrator to use this command.").setEphemeral(true).queue()
            return
        }

        val guildId = event.guild?.id
        if (guildId == null) {
            event.reply("❌ This command can only be used inside a server.").setEphemeral(true).queue()
            return
        }

        val channelId = event.channel.id
        when (event.name) {
            DiscordCommand.SUBSCRIBE.eventName -> {
                val subscription = DiscordSubscription(guildId = guildId, channelId = channelId)

                if (subscriptionRepository.existsById(guildId)) {
                    event
                        .reply("⚠️ Cubing TW Notifier is currently used in another channel in this server.")
                        .setEphemeral(true)
                        .queue()
                }
                subscriptionRepository.save(subscription)

                logger.info("✅ Discord service with guild: $guildId and channel: $channelId subscribed to Cubing TW Notifier.")

                event
                    .reply("✅ Your channel will receive updates from Cubing TW.")
                    .queue()
            }
            DiscordCommand.UNSUBSCRIBE.eventName -> {
                if (subscriptionRepository.existsById(guildId)) {
                    subscriptionRepository.deleteById(guildId)

                    logger.info("🗑️ Removed subscription for guild $guildId")
                    event.reply("✅ Successfully unsubscribed. This server will no longer receive WCA alerts.").queue()
                } else {
                    event.reply("⚠️ This server is not currently subscribed to any alerts.").setEphemeral(true).queue()
                }
            }
            else -> {
                logger.warn("⚠️ Unknown command ${event.name}")
            }
        }
    }
}
