package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

data class TutorSummary(
    val id: TutorId,
    val fullName: String,
    val email: String,
    val petsCount: Int
)