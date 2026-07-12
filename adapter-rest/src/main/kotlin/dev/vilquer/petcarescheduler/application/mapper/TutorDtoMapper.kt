package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email as EmailVO
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component

@Component
class TutorDtoMapper {

    data class CreateRequest(
        @field:NotBlank @field:Size(max = 80) val firstName:   String,
        @field:Size(max = 100) val lastName:    String?,
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank @field:Size(min = 8, max = 72) val rawPassword: String,
        val phoneNumber: PhoneNumber?,
        @field:Size(max = 512)
        @field:Pattern(regexp = "^(https?://\\S+)?$", message = "deve ser uma URL HTTP(S) válida")
        val avatar: String? = null
    )

    data class UpdateRequest(
        @field:NotBlank @field:Size(max = 80) val firstName: String,
        @field:Size(max = 100) val lastName: String? = null,
        val phoneNumber: String? = null,
        @field:Size(max = 512)
        @field:Pattern(regexp = "^(https?://\\S+)?$", message = "deve ser uma URL HTTP(S) válida")
        val avatar: String? = null
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
            phoneNumber = dto.phoneNumber?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { PhoneNumber.of(it).getOrThrow() },
            avatar      = dto.avatar
        )
}
