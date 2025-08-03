package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.*
import org.springframework.stereotype.Component

@Component
class TutorDtoMapper {

    data class CreateRequest(
        val firstName:   String,
        val lastName:    String?,
        val email:       String,
        val rawPassword: String,
        val phoneNumber: String? = null,
        val avatar:      String? = null
    )

    data class UpdateRequest(
        val firstName:   String? = null,
        val lastName:    String? = null,
        val phoneNumber: String? = null,
        val avatar:      String? = null
    )

    /* ---------- mapping ---------- */

    fun toCreateCommand(dto: CreateRequest): CreateTutorCommand =
        CreateTutorCommand(
            firstName   = dto.firstName,
            lastName    = dto.lastName,
            email       = Email.of(dto.email).getOrThrow(),
            rawPassword = dto.rawPassword,
            phoneNumber = dto.phoneNumber?.let { PhoneNumber.of(it).getOrThrow() },
            avatar      = dto.avatar
        )

    fun toUpdateCommand(id: Long, dto: UpdateRequest): UpdateTutorCommand =
        UpdateTutorCommand(
            tutorId     = TutorId(id),
            firstName   = dto.firstName,
            lastName    = dto.lastName,
            phoneNumber = dto.phoneNumber?.let { PhoneNumber.of(it).getOrThrow() },
            avatar      = dto.avatar
        )
}
