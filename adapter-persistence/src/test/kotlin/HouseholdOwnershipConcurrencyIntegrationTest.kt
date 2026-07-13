package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import dev.vilquer.petcarescheduler.infra.AbstractPostgresIntegrationTest
import dev.vilquer.petcarescheduler.infra.PersistenceTestApplication
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HouseholdJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HouseholdMemberJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdMemberJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [PersistenceTestApplication::class])
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HouseholdOwnershipConcurrencyIntegrationTest : AbstractPostgresIntegrationTest() {

    @Autowired lateinit var tutors: TutorJpaRepository
    @Autowired lateinit var households: HouseholdJpaRepository
    @Autowired lateinit var members: HouseholdMemberJpaRepository
    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `household lock prevents two owners from being demoted concurrently`() {
        val suffix = UUID.randomUUID()
        val firstTutor = tutor("owner-a-$suffix@example.com")
        val secondTutor = tutor("owner-b-$suffix@example.com")
        val now = Instant.parse("2026-07-13T12:00:00Z")
        val household = households.saveAndFlush(HouseholdJpa().also {
            it.id = UUID.randomUUID()
            it.name = "Casa compartilhada"
            it.createdByTutorId = firstTutor.id!!
            it.createdAt = now
            it.updatedAt = now
        })
        val ownerIds = listOf(firstTutor, secondTutor).map { tutor ->
            members.saveAndFlush(HouseholdMemberJpa().also {
                it.id = UUID.randomUUID()
                it.householdId = household.id
                it.tutorId = tutor.id!!
                it.role = HouseholdRole.OWNER
                it.joinedAt = now
            }).id
        }

        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val attempts = ownerIds.map { memberId ->
                CompletableFuture.supplyAsync({
                    ready.countDown()
                    start.await()
                    TransactionTemplate(transactionManager).execute {
                        households.findForUpdate(household.id)
                        val member = members.findOwnedForUpdate(memberId, household.id)!!
                        if (members.countByHouseholdIdAndRole(household.id, HouseholdRole.OWNER) <= 1) {
                            false
                        } else {
                            member.role = HouseholdRole.CAREGIVER
                            members.saveAndFlush(member)
                            true
                        }
                    } ?: false
                }, pool)
            }
            ready.await(5, TimeUnit.SECONDS)
            start.countDown()
            val results = attempts.map { it.get(20, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it })
            assertEquals(1L, members.countByHouseholdIdAndRole(household.id, HouseholdRole.OWNER))
        } finally {
            pool.shutdownNow()
        }
    }

    private fun tutor(email: String) = tutors.saveAndFlush(TutorJpa().also {
        it.firstName = "Tutor"
        it.email = email
        it.passwordHash = "hash"
    })
}
