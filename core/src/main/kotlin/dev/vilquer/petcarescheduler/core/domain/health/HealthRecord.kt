package dev.vilquer.petcarescheduler.core.domain.health

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId

@JvmInline value class HealthRecordId(val value: UUID)

enum class HealthRecordType {
    VACCINE,
    MEDICATION,
    CONSULTATION,
    EXAM,
    SYMPTOM,
    DAILY_CARE,
}

data class HealthRecord(
    val id: HealthRecordId = HealthRecordId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val tutorId: TutorId,
    val petId: PetId,
    val type: HealthRecordType,
    val occurredAt: Instant,
    val title: String,
    val notes: String? = null,
    val productName: String? = null,
    val dosage: String? = null,
    val batchNumber: String? = null,
    val professionalName: String? = null,
    val clinicName: String? = null,
    val costAmount: BigDecimal? = null,
    val currency: String? = null,
    val createdByTutorId: TutorId,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(title.isNotBlank() && title.length <= 120) { "health_record_title_invalid" }
        require(notes == null || notes.length <= 4_000) { "health_record_notes_invalid" }
        require(productName == null || productName.length <= 160) { "health_record_product_invalid" }
        require(dosage == null || dosage.length <= 120) { "health_record_dosage_invalid" }
        require(batchNumber == null || batchNumber.length <= 120) { "health_record_batch_invalid" }
        require(professionalName == null || professionalName.length <= 160) { "health_record_professional_invalid" }
        require(clinicName == null || clinicName.length <= 160) { "health_record_clinic_invalid" }
        require(productFieldsAreRelevant()) { "health_record_product_fields_not_applicable" }
        require(professionalFieldsAreRelevant()) { "health_record_professional_fields_not_applicable" }
        require(costAmount == null || (costAmount >= BigDecimal.ZERO && costAmount <= MAX_COST)) {
            "health_record_cost_invalid"
        }
        require(costAmount == null || costAmount.scale() <= 2) { "health_record_cost_scale_invalid" }
        require((costAmount == null) == (currency == null)) { "health_record_currency_required_with_cost" }
        currency?.let {
            require(it.matches(Regex("^[A-Z]{3}$"))) { "health_record_currency_invalid" }
            runCatching { Currency.getInstance(it) }.getOrElse { throw IllegalArgumentException("health_record_currency_invalid") }
        }
    }

    private fun productFieldsAreRelevant(): Boolean =
        (productName == null && dosage == null && batchNumber == null) ||
            type == HealthRecordType.VACCINE || type == HealthRecordType.MEDICATION

    private fun professionalFieldsAreRelevant(): Boolean =
        (professionalName == null && clinicName == null) ||
            type in PROFESSIONAL_RECORD_TYPES

    companion object {
        private val MAX_COST = BigDecimal("9999999999.99")
        private val PROFESSIONAL_RECORD_TYPES = setOf(
            HealthRecordType.VACCINE,
            HealthRecordType.MEDICATION,
            HealthRecordType.CONSULTATION,
            HealthRecordType.EXAM,
        )
    }
}
