package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DispatchPendingCareEscalationsUseCase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CareEscalationRelayScheduler(private val relay: DispatchPendingCareEscalationsUseCase) {
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 90 * 1000L)
    @SchedulerLock(name = "dispatchPendingCareEscalations", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    fun dispatch() = relay.dispatchPendingCareEscalations()
}
