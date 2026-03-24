package io.github.ricky9667.cubing_tw_notifier.service

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent

interface EventNotificationService {
    fun notifyNewEvent(event: CubingEvent)
    fun notifyRegistrationOpen(event: CubingEvent)
    fun notifyEventStart(event: CubingEvent)
}
