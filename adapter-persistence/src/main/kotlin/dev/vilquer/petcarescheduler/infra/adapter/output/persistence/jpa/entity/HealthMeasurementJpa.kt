package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementUnit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "health_measurement")
class HealthMeasurementJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "tutor_id", nullable = false) var tutorId: Long = 0
    @Column(name = "pet_id", nullable = false) var petId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var type: HealthMeasurementType
    @Column(name = "measurement_value", nullable = false, precision = 10, scale = 3) lateinit var value: BigDecimal
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var unit: HealthMeasurementUnit
    @Column(name = "measured_at", nullable = false) lateinit var measuredAt: Instant
    @Column(length = 500) var notes: String? = null
    @Column(name = "created_by_tutor_id", nullable = false) var createdByTutorId: Long = 0
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "updated_at", nullable = false) lateinit var updatedAt: Instant

    override fun equals(other: Any?) = this === other || (other is HealthMeasurementJpa && ::id.isInitialized && other::id.isInitialized && id == other.id)
    override fun hashCode() = if (::id.isInitialized) id.hashCode() else 0
}
