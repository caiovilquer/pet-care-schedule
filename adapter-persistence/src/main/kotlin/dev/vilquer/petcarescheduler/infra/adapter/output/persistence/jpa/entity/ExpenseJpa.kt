package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseCategory
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "expense")
class ExpenseJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "pet_id", nullable = false) var petId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) lateinit var category: ExpenseCategory
    @Column(nullable = false, length = 160) lateinit var description: String
    @Column(nullable = false, precision = 12, scale = 2) lateinit var amount: BigDecimal
    @Column(nullable = false, length = 3) lateinit var currency: String
    @Column(name = "occurred_at", nullable = false) lateinit var occurredAt: Instant
    @Column(length = 1000) var notes: String? = null
    @Column(name = "created_by_tutor_id", nullable = false) var createdByTutorId: Long = 0
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "updated_at", nullable = false) lateinit var updatedAt: Instant
}
