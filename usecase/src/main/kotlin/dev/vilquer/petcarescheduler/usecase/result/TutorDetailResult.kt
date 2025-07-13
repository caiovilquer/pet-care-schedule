package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

data class TutorDetailResult(
    val id: TutorId,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val phoneNumber: String,
    val avatar: String?,
    val pets: List<PetInfo>
) {
    data class PetInfo(
        val id: PetId,
        val name: String,
        val specie: String
    )
}