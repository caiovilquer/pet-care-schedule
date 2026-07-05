package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DispatchPendingRemindersUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReminderRelayScheduler(
    private val dispatchPendingReminders: DispatchPendingRemindersUseCase
) {
    /**
     * Roda a cada 5 minutos (não uma vez por dia como a detecção): um lembrete
     * que falhou às 8h porque a API de e-mail estava fora não deve esperar até
     * o dia seguinte para ser tentado de novo.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    fun dispatchPendingReminders() {
        dispatchPendingReminders.dispatchPendingReminders()
    }
}
