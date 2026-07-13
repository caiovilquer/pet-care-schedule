package dev.vilquer.petcarescheduler.core.domain.health

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId

@JvmInline value class HealthMeasurementId(val value: UUID)

enum class HealthMeasurementType { WEIGHT, TEMPERATURE, BODY_CONDITION_SCORE }
enum class HealthMeasurementUnit { KILOGRAM, CELSIUS, SCORE_1_TO_9 }

data class HealthMeasurement(
    val id: HealthMeasurementId = HealthMeasurementId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val tutorId: TutorId,
    val petId: PetId,
    val type: HealthMeasurementType,
    val value: BigDecimal,
    val unit: HealthMeasurementUnit = type.defaultUnit(),
    val measuredAt: Instant,
    val notes: String? = null,
    val createdByTutorId: TutorId,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(unit == type.defaultUnit()) { "health_measurement_unit_invalid" }
        require(notes == null || notes.length <= 500) { "health_measurement_notes_invalid" }
        when (type) {
            HealthMeasurementType.WEIGHT ->
                require(value >= BigDecimal("0.01") && value <= BigDecimal("500")) { "health_measurement_weight_invalid" }
            HealthMeasurementType.TEMPERATURE ->
                require(value >= BigDecimal("20") && value <= BigDecimal("50")) { "health_measurement_temperature_invalid" }
            HealthMeasurementType.BODY_CONDITION_SCORE ->
                require(value.stripTrailingZeros().scale() <= 0 && value >= BigDecimal.ONE && value <= BigDecimal("9")) {
                    "health_measurement_score_invalid"
                }
        }
    }
}

fun HealthMeasurementType.defaultUnit(): HealthMeasurementUnit = when (this) {
    HealthMeasurementType.WEIGHT -> HealthMeasurementUnit.KILOGRAM
    HealthMeasurementType.TEMPERATURE -> HealthMeasurementUnit.CELSIUS
    HealthMeasurementType.BODY_CONDITION_SCORE -> HealthMeasurementUnit.SCORE_1_TO_9
}
