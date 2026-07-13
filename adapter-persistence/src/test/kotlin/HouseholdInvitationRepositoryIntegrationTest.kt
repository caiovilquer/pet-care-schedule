package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import dev.vilquer.petcarescheduler.infra.AbstractPostgresIntegrationTest
import dev.vilquer.petcarescheduler.infra.PersistenceTestApplication
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HouseholdInvitationJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HouseholdJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdInvitationJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [PersistenceTestApplication::class])
class HouseholdInvitationRepositoryIntegrationTest : AbstractPostgresIntegrationTest() {

    @Autowired lateinit var tutors: TutorJpaRepository
    @Autowired lateinit var households: HouseholdJpaRepository
    @Autowired lateinit var invitations: HouseholdInvitationJpaRepository

    @Test
    fun `migration allows persisting an owner invitation`() {
        val suffix = UUID.randomUUID()
        val tutor = tutors.saveAndFlush(TutorJpa().also {
            it.firstName = "Ana"
            it.email = "ana-$suffix@example.com"
            it.passwordHash = "hash"
        })
        val now = Instant.parse("2026-07-13T12:00:00Z")
        val household = households.saveAndFlush(HouseholdJpa().also {
            it.id = UUID.randomUUID()
            it.name = "Casa da Ana"
            it.createdByTutorId = tutor.id!!
            it.createdAt = now
            it.updatedAt = now
        })
        val saved = invitations.saveAndFlush(HouseholdInvitationJpa().also {
            it.id = UUID.randomUUID()
            it.householdId = household.id
            it.email = "bia-$suffix@example.com"
            it.role = HouseholdRole.OWNER
            it.tokenHash = "a".repeat(64)
            it.activeKey = "${household.id}:bia-$suffix@example.com"
            it.invitedByTutorId = tutor.id!!
            it.expiresAt = now.plusSeconds(604_800)
            it.createdAt = now
        })

        assertEquals(HouseholdRole.OWNER, invitations.findById(saved.id).orElseThrow().role)
    }
}
