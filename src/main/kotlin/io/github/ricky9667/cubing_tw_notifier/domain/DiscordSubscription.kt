package io.github.ricky9667.cubing_tw_notifier.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "discord_subscriptions")
class DiscordSubscription(
    @Id
    @Column(name = "guild_id", nullable = false)
    val guildId: String,
    @Column(name = "channel_id", nullable = false)
    var channelId: String,
)
