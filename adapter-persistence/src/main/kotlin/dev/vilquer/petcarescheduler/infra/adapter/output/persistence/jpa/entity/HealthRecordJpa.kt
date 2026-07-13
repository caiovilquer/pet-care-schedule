package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
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
@Table(name = "health_record")
class HealthRecordJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "tutor_id", nullable = false) var tutorId: Long = 0
    @Column(name = "pet_id", nullable = false) var petId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var type: HealthRecordType
    @Column(name = "occurred_at", nullable = false) lateinit var occurredAt: Instant
    @Column(nullable = false, length = 120) lateinit var title: String
    @Column(length = 4000) var notes: String? = null
    @Column(name = "product_name", length = 160) var productName: String? = null
    @Column(length = 120) var dosage: String? = null
    @Column(name = "batch_number", length = 120) var batchNumber: String? = null
    @Column(name = "professional_name", length = 160) var professionalName: String? = null
    @Column(name = "clinic_name", length = 160) var clinicName: String? = null
    @Column(name = "cost_amount", precision = 12, scale = 2) var costAmount: BigDecimal? = null
    @Column(length = 3) var currency: String? = null
    @Column(name = "created_by_tutor_id", nullable = false) var createdByTutorId: Long = 0
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "updated_at", nullable = false) lateinit var updatedAt: Instant

    override fun equals(other: Any?) = this === other || (other is HealthRecordJpa && ::id.isInitialized && other::id.isInitialized && id == other.id)
    override fun hashCode() = if (::id.isInitialized) id.hashCode() else 0
}
