package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.finance.Expense
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseCategory
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import java.time.Instant

data class ExpenseFilter(
    val from: Instant,
    val to: Instant,
    val petId: PetId? = null,
    val category: ExpenseCategory? = null,
)

interface ExpenseRepositoryPort {
    fun save(expense: Expense): Expense
    fun findByIdAndHousehold(id: ExpenseId, householdId: HouseholdId): Expense?
    fun findByIdAndHouseholdForUpdate(id: ExpenseId, householdId: HouseholdId): Expense?
    fun search(householdId: HouseholdId, filter: ExpenseFilter, page: Int, size: Int): List<Expense>
    fun count(householdId: HouseholdId, filter: ExpenseFilter): Long
    fun delete(id: ExpenseId)
}
