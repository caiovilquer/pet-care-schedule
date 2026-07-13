package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseCategory
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.ExpenseJpa
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ExpenseJpaRepository : JpaRepository<ExpenseJpa, UUID> {
    fun findByIdAndHouseholdId(id: UUID, householdId: UUID): ExpenseJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExpenseJpa e where e.id = :id and e.householdId = :householdId")
    fun findForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): ExpenseJpa?

    @Query("""
        select e from ExpenseJpa e
        where e.householdId = :householdId and e.occurredAt >= :from and e.occurredAt < :to
          and (:petId is null or e.petId = :petId)
          and (:category is null or e.category = :category)
        order by e.occurredAt desc, e.id desc
    """)
    fun search(
        @Param("householdId") householdId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("petId") petId: Long?,
        @Param("category") category: ExpenseCategory?,
        pageable: Pageable,
    ): Page<ExpenseJpa>
}
