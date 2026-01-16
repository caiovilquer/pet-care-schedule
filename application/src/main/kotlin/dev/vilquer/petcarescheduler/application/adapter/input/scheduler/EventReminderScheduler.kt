package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.application.service.EventAppService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventReminderScheduler(
    private val eventService: EventAppService
) {
    /** Every day at 8 o'clock */
    @Scheduled(cron = "0 0 8 * * *", zone = "\${app.timezone:America/Sao_Paulo}")
    fun sendDailyReminders() {
        eventService.sendRemindersForToday()
    }
}
