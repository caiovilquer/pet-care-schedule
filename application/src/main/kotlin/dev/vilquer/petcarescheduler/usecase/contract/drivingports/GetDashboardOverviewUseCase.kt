package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.DashboardOverviewResult

interface GetDashboardOverviewUseCase {
    fun getOverview(tutorId: TutorId): DashboardOverviewResult
}
