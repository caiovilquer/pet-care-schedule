package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email as EmailVO
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component

@Component
class TutorDtoMapper {

    data class CreateRequest(
        @field:NotBlank val firstName:   String,
        val lastName:    String?,
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank val rawPassword: String,
        val phoneNumber: PhoneNumber?,
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
            email       = EmailVO.of(dto.email).getOrThrow(),
            rawPassword = dto.rawPassword,
            phoneNumber = dto.phoneNumber,
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
