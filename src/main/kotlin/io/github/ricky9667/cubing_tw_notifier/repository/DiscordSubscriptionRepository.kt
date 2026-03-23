package io.github.ricky9667.cubing_tw_notifier.repository

import io.github.ricky9667.cubing_tw_notifier.domain.DiscordSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscordSubscriptionRepository : JpaRepository<DiscordSubscription, String>
