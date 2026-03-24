package io.github.ricky9667.cubing_tw_notifier.domain

enum class DiscordCommand(
    val eventName: String,
    val description: String,
) {
    SUBSCRIBE("subscribe", "Subscribe to receive Cubing TW updates."),
    UNSUBSCRIBE("unsubscribe", "Unsubscribe to stop sending Cubing TW updates."),
}
