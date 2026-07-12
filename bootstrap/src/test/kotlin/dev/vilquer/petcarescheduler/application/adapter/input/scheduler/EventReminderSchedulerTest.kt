package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CareScheduleMaintenanceUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class CareScheduleSchedulerTest {
    private val useCase: CareScheduleMaintenanceUseCase = mock(CareScheduleMaintenanceUseCase::class.java)
    private val scheduler = CareScheduleScheduler(useCase)

    @Test
    fun `scheduler delegates to use case`() {
        scheduler.maintainSchedule()
        verify(useCase).materializeAndEnqueueReminders()
    }
}
