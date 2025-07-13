package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email

data class TutorSummary(
    val id: TutorId,
    val fullName: String,
    val email: Email,
    val petsCount: Int
)