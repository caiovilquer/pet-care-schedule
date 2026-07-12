package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetDashboardOverviewUseCase
import dev.vilquer.petcarescheduler.usecase.result.DashboardOverviewResult
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val dashboard: GetDashboardOverviewUseCase,
) {
    @GetMapping
    fun overview(@AuthenticationPrincipal jwt: CurrentJwt): DashboardOverviewResult =
        dashboard.getOverview(TutorId(jwt.tutorId()))
}
