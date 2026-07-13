package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.finance.Expense
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.ExpenseJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.ExpenseJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ExpenseFilter
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ExpenseRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class FinanceRepositoryAdapter(private val jpa: ExpenseJpaRepository) : ExpenseRepositoryPort {
    override fun save(expense: Expense) = jpa.saveAndFlush(expense.toJpa()).toDomain()
    override fun findByIdAndHousehold(id: ExpenseId, householdId: HouseholdId) =
        jpa.findByIdAndHouseholdId(id.value, householdId.value)?.toDomain()
    override fun findByIdAndHouseholdForUpdate(id: ExpenseId, householdId: HouseholdId) =
        jpa.findForUpdate(id.value, householdId.value)?.toDomain()
    override fun search(householdId: HouseholdId, filter: ExpenseFilter, page: Int, size: Int) =
        jpa.search(householdId.value, filter.from, filter.to, filter.petId?.value, filter.category, PageRequest.of(page, size))
            .content.map { it.toDomain() }
    override fun count(householdId: HouseholdId, filter: ExpenseFilter) =
        jpa.search(householdId.value, filter.from, filter.to, filter.petId?.value, filter.category, PageRequest.of(0, 1)).totalElements
    override fun delete(id: ExpenseId) = jpa.deleteById(id.value)
}

private fun Expense.toJpa() = ExpenseJpa().also {
    it.id = id.value; it.version = version; it.householdId = householdId.value; it.petId = petId.value
    it.category = category; it.description = description; it.amount = amount; it.currency = currency
    it.occurredAt = occurredAt; it.notes = notes; it.createdByTutorId = createdByTutorId.value
    it.createdAt = createdAt; it.updatedAt = updatedAt
}

private fun ExpenseJpa.toDomain() = Expense(
    ExpenseId(id), version, HouseholdId(householdId), PetId(petId), category, description, amount, currency,
    occurredAt, notes, TutorId(createdByTutorId), createdAt, updatedAt,
)
