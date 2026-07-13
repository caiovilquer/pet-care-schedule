package dev.vilquer.petcarescheduler.core.domain.care

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.math.BigDecimal
import java.util.Currency

@JvmInline value class CarePlanId(val value: UUID)

data class CarePlan(
    val id: CarePlanId = CarePlanId(UUID.randomUUID()),
    val version: Long? = null,
    val scheduleRevision: Int = 0,
    val householdId: HouseholdId,
    val tutorId: TutorId,
    val petId: PetId,
    val responsibleTutorId: TutorId,
    val type: EventType,
    val title: String,
    val instructions: String? = null,
    val startAt: LocalDateTime,
    val recurrence: Recurrence? = null,
    val reminderMinutesBefore: Int = 0,
    val critical: Boolean = false,
    val escalationDelayMinutes: Int? = null,
    val escalationTutorId: TutorId? = null,
    val estimatedCostAmount: BigDecimal? = null,
    val estimatedCostCurrency: String? = null,
    val active: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(title.isNotBlank() && title.length <= 120) { "care_plan_title_invalid" }
        require(instructions == null || instructions.length <= 2_000) { "care_plan_instructions_invalid" }
        require(reminderMinutesBefore in 0..10_080) { "care_plan_reminder_invalid" }
        require(scheduleRevision >= 0) { "care_plan_revision_invalid" }
        require(critical == (escalationDelayMinutes != null)) { "care_plan_escalation_configuration_invalid" }
        require(escalationDelayMinutes == null || escalationDelayMinutes in 15..10_080) { "care_plan_escalation_delay_invalid" }
        require(!critical || escalationTutorId != null) { "care_plan_escalation_recipient_required" }
        require(estimatedCostAmount == null || (estimatedCostAmount > BigDecimal.ZERO && estimatedCostAmount <= MAX_COST && estimatedCostAmount.scale() <= 2)) {
            "care_plan_estimated_cost_invalid"
        }
        require((estimatedCostAmount == null) == (estimatedCostCurrency == null)) { "care_plan_estimated_currency_required" }
        estimatedCostCurrency?.let {
            require(it.matches(Regex("^[A-Z]{3}$"))) { "care_plan_estimated_currency_invalid" }
            runCatching { Currency.getInstance(it) }.getOrElse { throw IllegalArgumentException("care_plan_estimated_currency_invalid") }
        }
    }

    fun occurrencesThrough(horizon: LocalDateTime, generatedAt: Instant): List<CareOccurrence> {
        if (horizon.isBefore(startAt)) return emptyList()
        val occurrences = ArrayList<CareOccurrence>()
        var dueAt = startAt
        var sequence = 0

        while (!dueAt.isAfter(horizon) && sequence < MAX_OCCURRENCES_PER_HORIZON) {
            val recurrence = recurrence
            if (recurrence?.repetitions != null && sequence >= recurrence.repetitions) break
            if (recurrence?.finalDate != null && dueAt.isAfter(recurrence.finalDate)) break

            occurrences += CareOccurrence(
                id = deterministicOccurrenceId(sequence),
                planId = id,
                scheduleRevision = scheduleRevision,
                householdId = householdId,
                tutorId = tutorId,
                petId = petId,
                responsibleTutorId = responsibleTutorId,
                sequence = sequence,
                type = type,
                title = title,
                instructions = instructions,
                dueAt = dueAt,
                status = CareOccurrenceStatus.SCHEDULED,
                critical = critical,
                escalationDelayMinutes = escalationDelayMinutes,
                escalationTutorId = escalationTutorId,
                estimatedCostAmount = estimatedCostAmount,
                estimatedCostCurrency = estimatedCostCurrency,
                createdAt = generatedAt,
                updatedAt = generatedAt,
            )

            if (recurrence == null) break
            val next = recurrence.nextOccurrence(dueAt)
            require(next.isAfter(dueAt)) { "care_plan_recurrence_did_not_advance" }
            dueAt = next
            sequence += 1
        }
        return occurrences
    }

    fun deactivate(at: Instant): CarePlan = copy(active = false, updatedAt = at)

    private fun deterministicOccurrenceId(sequence: Int): CareOccurrenceId = CareOccurrenceId(
        UUID.nameUUIDFromBytes("${id.value}:$scheduleRevision:$sequence".toByteArray(StandardCharsets.UTF_8)),
    )

    companion object {
        const val MAX_OCCURRENCES_PER_HORIZON = 500
        private val MAX_COST = BigDecimal("9999999999.99")
    }
}
