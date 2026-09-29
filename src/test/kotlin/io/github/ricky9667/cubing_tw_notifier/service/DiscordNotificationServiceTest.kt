package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import io.github.ricky9667.cubing_tw_notifier.domain.DiscordSubscription
import io.github.ricky9667.cubing_tw_notifier.repository.DiscordSubscriptionRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import kotlin.test.assertTrue

class DiscordNotificationServiceTest {
    private val repository = mock(DiscordSubscriptionRepository::class.java)
    private val service = DiscordNotificationService("", repository, mock(DiscordCommandListener::class.java))
    private val jda = mock(JDA::class.java)
    private val channel = mock(TextChannel::class.java)
    private val action = mock(MessageCreateAction::class.java)

    @Test
    fun `alert tags only the selected mention role`() {
        val role = mock(Role::class.java)
        val guild = mock(Guild::class.java)
        `when`(repository.findAll()).thenReturn(listOf(DiscordSubscription("guild-1", "channel-1", "role-1")))
        `when`(jda.getTextChannelById("channel-1")).thenReturn(channel)
        `when`(channel.guild).thenReturn(guild)
        `when`(guild.getRoleById("role-1")).thenReturn(role)
        `when`(role.id).thenReturn("role-1")
        `when`(role.isMentionable).thenReturn(true)
        `when`(channel.sendMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn(action)
        `when`(action.setAllowedMentions(emptySet())).thenReturn(action)
        `when`(action.mentionRoles("role-1")).thenReturn(action)
        ReflectionTestUtils.setField(service, "jda", jda)

        service.notifyNewEvent(event())

        val content = ArgumentCaptor.forClass(String::class.java)
        verify(channel).sendMessage(content.capture())
        assertTrue(content.value.startsWith("<@&role-1>\n"))
        verify(action).setAllowedMentions(emptySet<Message.MentionType>())
        verify(action).mentionRoles("role-1")
    }

    @Test
    fun `alert is sent without a tag when the saved role is gone`() {
        `when`(repository.findAll()).thenReturn(listOf(DiscordSubscription("guild-1", "channel-1", "role-1")))
        `when`(jda.getTextChannelById("channel-1")).thenReturn(channel)
        `when`(channel.guild).thenReturn(mock(Guild::class.java))
        `when`(channel.sendMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn(action)
        `when`(action.setAllowedMentions(emptySet())).thenReturn(action)
        ReflectionTestUtils.setField(service, "jda", jda)

        service.notifyNewEvent(event())

        val content = ArgumentCaptor.forClass(String::class.java)
        verify(channel).sendMessage(content.capture())
        assertTrue(content.value.startsWith("📢"))
        verify(action).setAllowedMentions(emptySet<Message.MentionType>())
        verify(action, never()).mentionRoles(org.mockito.ArgumentMatchers.anyString())
    }

    private fun event() =
        CubingEvent(
            url = "https://cubing-tw.net/event/123",
            name = "Example Competition",
            eventDate = "2026/10/01",
            startDate = LocalDate.of(2026, 10, 1),
            registrationTime = null,
        )
}
