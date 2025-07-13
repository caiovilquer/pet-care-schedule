package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.application.service.EventAppService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class EventReminderSchedulerTest {
    private val service: EventAppService = mock(EventAppService::class.java)
    private val scheduler = EventReminderScheduler(service)

    @Test
    fun `scheduler delegates to service`() {
        scheduler.sendDailyReminders()
        verify(service).sendRemindersForToday()
    }
}
