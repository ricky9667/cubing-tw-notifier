package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.DiscordSubscription
import io.github.ricky9667.cubing_tw_notifier.repository.DiscordSubscriptionRepository
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.SelfMember
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class DiscordCommandListenerTest {
    private val repository = mock(DiscordSubscriptionRepository::class.java)
    private val listener = DiscordCommandListener(repository)

    @Test
    fun `subscribe stores the selected mention role`() {
        val event = subscribeEvent()
        val role = mock(Role::class.java)
        val option = mock(OptionMapping::class.java)
        `when`(event.getOption("role")).thenReturn(option)
        `when`(option.asRole).thenReturn(role)
        `when`(role.id).thenReturn("role-1")
        `when`(role.isMentionable).thenReturn(true)

        listener.onSlashCommandInteraction(event)

        val saved = ArgumentCaptor.forClass(DiscordSubscription::class.java)
        verify(repository).save(saved.capture())
        assertEquals("role-1", saved.value.roleId)
    }

    @Test
    fun `resubscribing without a role clears the previous role`() {
        val event = subscribeEvent()
        `when`(repository.existsById("guild-1")).thenReturn(true)

        listener.onSlashCommandInteraction(event)

        val saved = ArgumentCaptor.forClass(DiscordSubscription::class.java)
        verify(repository).save(saved.capture())
        assertEquals(null, saved.value.roleId)
    }

    @Test
    fun `subscribe rejects a role the bot cannot tag`() {
        val event = subscribeEvent()
        val role = mock(Role::class.java)
        val option = mock(OptionMapping::class.java)
        `when`(event.getOption("role")).thenReturn(option)
        `when`(option.asRole).thenReturn(role)

        listener.onSlashCommandInteraction(event)

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(DiscordSubscription::class.java))
    }

    private fun subscribeEvent(): SlashCommandInteractionEvent {
        val event = mock(SlashCommandInteractionEvent::class.java)
        val member = mock(Member::class.java)
        val guild = mock(Guild::class.java)
        val channel = mock(MessageChannelUnion::class.java)
        val selfMember = mock(SelfMember::class.java)
        val guildChannel = mock(GuildMessageChannel::class.java)
        val reply = mock(ReplyCallbackAction::class.java)
        `when`(event.member).thenReturn(member)
        `when`(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true)
        `when`(event.guild).thenReturn(guild)
        `when`(guild.id).thenReturn("guild-1")
        `when`(guild.selfMember).thenReturn(selfMember)
        `when`(event.channel).thenReturn(channel)
        `when`(channel.id).thenReturn("channel-1")
        `when`(channel.asGuildMessageChannel()).thenReturn(guildChannel)
        `when`(event.name).thenReturn("subscribe")
        `when`(event.reply(org.mockito.ArgumentMatchers.anyString())).thenReturn(reply)
        `when`(reply.setEphemeral(true)).thenReturn(reply)
        return event
    }
}
