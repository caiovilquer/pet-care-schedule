package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.result.*

interface ExpenseUseCase {
    fun create(command: CreateExpenseCommand, access: HouseholdAccess): ExpenseResult
    fun update(command: UpdateExpenseCommand, access: HouseholdAccess): ExpenseResult
    fun search(query: SearchExpensesQuery, access: HouseholdAccess): ExpensesPageResult
    fun delete(id: ExpenseId, expectedVersion: Long, access: HouseholdAccess)
}

fun interface FinanceOverviewUseCase {
    fun overview(query: FinanceOverviewQuery, access: HouseholdAccess): FinanceOverviewResult
}
