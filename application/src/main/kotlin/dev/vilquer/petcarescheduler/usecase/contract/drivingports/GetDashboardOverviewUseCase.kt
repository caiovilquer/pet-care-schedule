package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.result.DashboardOverviewResult

interface GetDashboardOverviewUseCase {
    fun getOverview(access: HouseholdAccess): DashboardOverviewResult
}
