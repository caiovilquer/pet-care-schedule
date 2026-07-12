package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa

/**
 * Mapper responsible for converting between Tutor domain entities and JPA entities.
 */
object TutorMapper {
    /**
     * Maps JPA entity to domain entity.
     *
     * @param jpa The JPA entity to convert
     * @return The corresponding domain entity
     */
    fun toDomain(jpa: TutorJpa): Tutor {
        val tutorId =
            TutorId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping TutorJpa to domain"))
        return Tutor(
            id = tutorId,
            version = jpa.version,
            firstName = jpa.firstName,
            lastName = jpa.lastName,
            email = Email.of(jpa.email).getOrThrow(),
            passwordHash = jpa.passwordHash,
            passwordChangedAt = jpa.passwordChangedAt,
            phoneNumber = jpa.phoneNumber?.let { PhoneNumber.of(it).getOrThrow()},
            avatar = jpa.avatar,
            avatarAssetId = jpa.avatarAssetId,
        )
    }

    /**
     * Maps domain entity to JPA entity.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @return The corresponding JPA entity
     */
    fun toJpa(
        domain: Tutor,
        existing: TutorJpa? = null
    ): TutorJpa {
        val jpa = existing ?: TutorJpa()
        with(domain) {
            jpa.id = id?.value
            jpa.version = version
            jpa.firstName = firstName
            jpa.lastName = lastName
            jpa.email = email.value
            jpa.passwordHash = passwordHash
            jpa.passwordChangedAt = passwordChangedAt ?: jpa.passwordChangedAt
            jpa.phoneNumber = phoneNumber?.e164
            jpa.avatar = avatar
            jpa.avatarAssetId = avatarAssetId
        }
        return jpa
    }
}

// Extension functions for more convenient use within the codebase
fun Tutor.toJpa(): TutorJpa = TutorMapper.toJpa(this)
fun TutorJpa.toDomain(): Tutor = TutorMapper.toDomain(this)
