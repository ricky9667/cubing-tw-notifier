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
            event
                .reply("❌ You must have the \"Manage Server\" permission to use this command.")
                .setEphemeral(true)
                .queue()
            return
        }

        val guild = event.guild
        if (guild == null) {
            event
                .reply("❌ This command can only be used inside a server.")
                .setEphemeral(true)
                .queue()
            return
        }

        val guildId = guild.id
        val channelId = event.channel.id
        when (event.name) {
            DiscordCommand.SUBSCRIBE.eventName -> {
                val role = event.getOption("role")?.asRole
                if (role != null) {
                    val botCanMentionAll =
                        guild.selfMember.hasPermission(
                            event.channel.asGuildMessageChannel(),
                            Permission.MESSAGE_MENTION_EVERYONE,
                        )
                    if (role.isPublicRole || (!role.isMentionable && !botCanMentionAll)) {
                        event
                            .reply("❌ The bot cannot tag that role in this channel. Choose a mentionable role.")
                            .setEphemeral(true)
                            .queue()
                        return
                    }
                }

                val subscription = DiscordSubscription(guildId = guildId, channelId = channelId, roleId = role?.id)
                val roleTagStatus = if (role == null) "without role tags" else "with the selected role tagged"

                if (subscriptionRepository.existsById(guildId)) {
                    subscriptionRepository.save(subscription)
                    logger.info("🔁 Updated subscription for guild $guildId to channel $channelId and role ${role?.id}.")
                    event
                        .reply(
                            "✅ Subscription updated. This channel will receive Cubing TW alerts $roleTagStatus.",
                        ).setEphemeral(true)
                        .queue()
                } else {
                    subscriptionRepository.save(subscription)
                    logger.info("✅ Discord service with guild: $guildId and channel: $channelId subscribed to Cubing TW Notifier.")
                    event
                        .reply("✅ This channel will receive Cubing TW alerts $roleTagStatus.")
                        .queue()
                }
            }

            DiscordCommand.UNSUBSCRIBE.eventName -> {
                if (subscriptionRepository.existsById(guildId)) {
                    subscriptionRepository.deleteById(guildId)

                    logger.info("🗑️ Removed subscription for guild $guildId")
                    event
                        .reply("✅ Successfully unsubscribed. This server will no longer receive WCA alerts.")
                        .queue()
                } else {
                    event
                        .reply("⚠️ This server is not currently subscribed to any alerts.")
                        .setEphemeral(true)
                        .queue()
                }
            }

            else -> {
                logger.warn("⚠️ Unknown command ${event.name}")
                event
                    .reply("❌ Unknown command: `${event.name}`.")
                    .setEphemeral(true)
                    .queue()
            }
        }
    }
}
