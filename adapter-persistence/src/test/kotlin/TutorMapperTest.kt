package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.TutorMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TutorMapperTest {

    private companion object {
        const val TUTOR_ID = 10L
    }

    @Test
    fun `should correctly map TutorJpa to domain entity`() {
        val tutorJpa = createSampleTutorJpa()

        val domain = TutorMapper.toDomain(tutorJpa)

        with(domain) {
            assertEquals(TutorId(TUTOR_ID), id)
            assertEquals("Carlos", firstName)
            assertEquals("Silva", lastName)
            assertEquals("carlos@ex.com", email.toString())
            assertEquals("abc123", passwordHash)
            assertEquals(PhoneNumber.of("55599990000").getOrNull(), phoneNumber)
            assertEquals("avatar.png", avatar)
        }
    }

    @Test
    fun `should create new TutorJpa instance when mapping from domain without existing entity`() {
        val domain = createNewTutorDomain()

        val jpa = TutorMapper.toJpa(domain)

        with(jpa) {
            assertNull(id)
            assertEquals("Mariana", firstName)
            assertEquals("Costa", lastName)
            assertEquals("mari@ex.com", email)
            assertEquals("senhaX", passwordHash)
            assertEquals(PhoneNumber.of("55588881111").getOrNull()?.e164, phoneNumber)
            assertNull(avatar)
        }
    }

    @Test
    fun `should update existing TutorJpa when mapping from domain with existing entity`() {
        val existing = createExistingTutorJpa()
        val domain = createUpdatedTutorDomain()

        val jpa = TutorMapper.toJpa(domain, existing)

        with(jpa) {
            assertEquals(5L, id)
            assertEquals("Ana Paula", firstName)
            assertEquals("Lima", lastName)
            assertEquals("ana.paula@ex.com", email)
            assertEquals("newHash", passwordHash)
            assertEquals(PhoneNumber.of("55577773333").getOrNull()?.e164, phoneNumber)
            assertEquals("new.png", avatar)
        }
    }

    private fun createSampleTutorJpa(): TutorJpa =
        TutorJpa().apply {
            id = TUTOR_ID
            firstName = "Carlos"
            lastName = "Silva"
            email = "carlos@ex.com"
            passwordHash = "abc123"
            phoneNumber = "55599990000"
            avatar = "avatar.png"
        }

    private fun createNewTutorDomain(): Tutor =
        Tutor(
            id = null,
            firstName = "Mariana",
            lastName = "Costa",
            email = Email.of("mari@ex.com").getOrThrow(),
            passwordHash = "senhaX",
            phoneNumber = PhoneNumber.of("55588881111").getOrNull(),
            avatar = null
        )

    private fun createExistingTutorJpa(): TutorJpa =
        TutorJpa().apply {
            id = 5
            firstName = "Ana"
            lastName = "Lima"
            email = "ana@ex.com"
            phoneNumber = "55577772222"
            avatar = "old.png"
        }

    private fun createUpdatedTutorDomain(): Tutor =
        Tutor(
            id = TutorId(5),
            firstName = "Ana Paula",
            lastName = "Lima",
            email = Email.of("ana.paula@ex.com").getOrThrow(),
            passwordHash = "newHash",
            phoneNumber = PhoneNumber.of("55577773333").getOrNull(),
            avatar = "new.png"
        )
}
