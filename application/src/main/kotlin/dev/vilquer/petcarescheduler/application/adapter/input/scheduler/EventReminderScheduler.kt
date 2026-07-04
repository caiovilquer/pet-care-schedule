package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SendDailyRemindersUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventReminderScheduler(
    private val sendDailyReminders: SendDailyRemindersUseCase
) {
    /** Every day at 8 o'clock */
    @Scheduled(cron = "0 0 8 * * *", zone = "\${app.timezone:America/Sao_Paulo}")
    fun sendDailyReminders() {
        sendDailyReminders.sendRemindersForToday()
    }
}
