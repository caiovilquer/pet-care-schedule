package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.health.*
import dev.vilquer.petcarescheduler.core.domain.household.*
import dev.vilquer.petcarescheduler.core.domain.report.*
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.*

class VeterinaryReportAppServiceTest {
    private val owner = TutorId(1)
    private val petId = PetId(10)
    private val access = HouseholdAccess(TEST_HOUSEHOLD_ID, owner, HouseholdRole.OWNER)
    private val clock = FakeClock(ZonedDateTime.of(2026, 7, 12, 12, 0, 0, 0, ZoneId.of("America/Sao_Paulo")))
    private lateinit var records: InMemoryHealthRecordRepo
    private lateinit var shares: Shares
    private lateinit var service: VeterinaryReportAppService

    @BeforeEach
    fun setup() {
        records = InMemoryHealthRecordRepo()
        shares = Shares()
        val pets = InMemoryPetRepo(mapOf(petId to Pet(petId, 0, "Nina", "Gato", "SRD", LocalDate.of(2022, 3, 2), tutorId = owner, householdId = TEST_HOUSEHOLD_ID)))
        records.save(HealthRecord(
            householdId = TEST_HOUSEHOLD_ID, tutorId = owner, petId = petId, type = HealthRecordType.MEDICATION,
            occurredAt = Instant.parse("2026-07-10T14:00:00Z"), title = "Antibiótico", notes = "Resposta observada",
            productName = "Produto X", dosage = "1 comprimido", costAmount = BigDecimal("42.50"), currency = "BRL",
            createdByTutorId = owner, createdAt = clock.now().toInstant(), updatedAt = clock.now().toInstant(),
        ))
        service = VeterinaryReportAppService(
            records, InMemoryHealthMeasurementRepo(), InMemoryHealthAttachmentRepo(), InMemoryCareOccurrenceRepo(), pets,
            shares, InMemoryMediaAssetRepo(), FakeObjectStorage(), FakeTransactionPort(), clock,
        )
    }

    @Test
    fun `share stores only hash and public defaults minimize notes costs and documents`() {
        val created = service.createShare(
            CreateVeterinaryShareCommand(petId, "Consulta", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 12), 24), access,
        )

        val stored = shares.items.single()
        assertNotEquals(created.token, stored.tokenHash)
        assertEquals(64, stored.tokenHash.length)
        assertFalse(stored.tokenHash.contains(created.token))

        val public = service.publicSummary(ResolveVeterinaryShareCommand(created.token))
        val medication = public.summary.records.single()
        assertNull(medication.notes)
        assertNull(medication.costAmount)
        assertTrue(public.summary.documents.isEmpty())
        assertEquals(1, shares.items.single().accessCount)
    }

    @Test
    fun `expired token and viewer share creation are rejected without revealing data`() {
        assertThrows(ForbiddenException::class.java) {
            service.createShare(
                CreateVeterinaryShareCommand(petId, "Consulta", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 12), 24),
                HouseholdAccess(TEST_HOUSEHOLD_ID, TutorId(2), HouseholdRole.VIEWER),
            )
        }
        val created = service.createShare(
            CreateVeterinaryShareCommand(petId, "Consulta", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 12), 1), access,
        )
        clock.fixed = clock.fixed.plusHours(2)
        assertThrows(NotFoundException::class.java) { service.publicSummary(ResolveVeterinaryShareCommand(created.token)) }
    }

    private class Shares : VeterinaryShareRepositoryPort {
        val items = mutableListOf<VeterinaryShare>()
        override fun save(share: VeterinaryShare): VeterinaryShare {
            val saved = share.copy(version = (items.firstOrNull { it.id == share.id }?.version ?: -1) + 1)
            items.removeIf { it.id == saved.id }; items += saved; return saved
        }
        override fun findActiveByHashForUpdate(tokenHash: String) = items.firstOrNull { it.tokenHash == tokenHash && it.revokedAt == null }
        override fun findByIdAndHouseholdForUpdate(id: VeterinaryShareId, householdId: HouseholdId) = items.firstOrNull { it.id == id && it.householdId == householdId }
        override fun list(householdId: HouseholdId, petId: PetId?, limit: Int) = items.filter { it.householdId == householdId && (petId == null || it.petId == petId) }.take(limit)
        override fun countActive(householdId: HouseholdId, now: Instant) = items.count { it.householdId == householdId && it.revokedAt == null && it.expiresAt > now }.toLong()
    }
}
