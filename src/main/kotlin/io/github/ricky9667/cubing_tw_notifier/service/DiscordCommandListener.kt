package io.github.ricky9667.cubing_tw_notifier.service

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
        if (event.name != "setchannel") return

        if (event.member?.hasPermission(Permission.MANAGE_SERVER) != true) {
            event.reply("❌ You must be an Administrator to use this command.").setEphemeral(true).queue()
            return
        }

        val guildId = event.guild?.id
        val channelId = event.channel.id

        if (guildId != null) {
            // Save or update the database
            val subscription = DiscordSubscription(guildId = guildId, channelId = channelId)
            subscriptionRepository.save(subscription)

            logger.info("✅ Bound bot to channel $channelId in guild $guildId")

            // Reply to the user so they know it worked
            event
                .reply("✅ Awesome! I will send all future WCA Taiwan competition alerts to this channel.")
                .queue()
        } else {
            event.reply("❌ This command can only be used inside a server.").setEphemeral(true).queue()
        }
    }
}
