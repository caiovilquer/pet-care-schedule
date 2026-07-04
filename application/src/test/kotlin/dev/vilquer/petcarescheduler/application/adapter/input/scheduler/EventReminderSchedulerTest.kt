package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SendDailyRemindersUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class EventReminderSchedulerTest {
    private val useCase: SendDailyRemindersUseCase = mock(SendDailyRemindersUseCase::class.java)
    private val scheduler = EventReminderScheduler(useCase)

    @Test
    fun `scheduler delegates to use case`() {
        scheduler.sendDailyReminders()
        verify(useCase).sendRemindersForToday()
    }
}
