package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.notification

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import org.hibernate.query.sqm.tree.SqmNode.log
import org.springframework.stereotype.Component

@Component
class LogNotificationAdapter : NotificationPort {
    override fun sendEventReminder(event: Event) {
        log.info("Sending event: $event")
    }
}