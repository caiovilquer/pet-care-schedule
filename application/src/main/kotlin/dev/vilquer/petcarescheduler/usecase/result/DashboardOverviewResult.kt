package dev.vilquer.petcarescheduler.usecase.result

import java.util.UUID

data class DashboardOverviewResult(
    val firstName: String,
    val lastName: String?,
    val email: String,
    val avatar: String?,
    val avatarAssetId: UUID? = null,
    val totalPets: Long,
    val totalEvents: Long,
    val pets: List<PetSummary>,
    val upcomingEvents: List<EventSummary>,
)
