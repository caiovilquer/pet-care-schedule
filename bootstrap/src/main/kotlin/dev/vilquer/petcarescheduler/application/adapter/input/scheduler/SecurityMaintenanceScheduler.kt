package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SecurityMaintenanceUseCase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SecurityMaintenanceScheduler(
    private val securityMaintenance: SecurityMaintenanceUseCase
) {
    @Scheduled(cron = "0 30 3 * * *", zone = "\${app.timezone:America/Sao_Paulo}")
    @SchedulerLock(name = "cleanupSecurityArtifacts", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    fun cleanupSecurityArtifacts() {
        securityMaintenance.cleanupSecurityArtifacts()
    }
}
