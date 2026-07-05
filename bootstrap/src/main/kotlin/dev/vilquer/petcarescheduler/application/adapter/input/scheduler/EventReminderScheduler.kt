package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SendDailyRemindersUseCase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventReminderScheduler(
    private val sendDailyReminders: SendDailyRemindersUseCase
) {
    /** Every day at 8 o'clock */
    @Scheduled(cron = "0 0 8 * * *", zone = "\${app.timezone:America/Sao_Paulo}")
    @SchedulerLock(name = "sendDailyReminders", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    fun sendDailyReminders() {
        sendDailyReminders.sendRemindersForToday()
    }
}
