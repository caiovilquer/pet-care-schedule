package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseCategory
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class CreateExpenseCommand(
    val petId: PetId,
    val category: ExpenseCategory,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val occurredAt: Instant,
    val notes: String?,
)

data class UpdateExpenseCommand(
    val expenseId: ExpenseId,
    val expectedVersion: Long,
    val category: ExpenseCategory,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val occurredAt: Instant,
    val notes: String?,
)

data class SearchExpensesQuery(
    val from: Instant,
    val to: Instant,
    val petId: PetId? = null,
    val category: ExpenseCategory? = null,
    val page: Int = 0,
    val size: Int = 20,
)

data class FinanceOverviewQuery(
    val from: LocalDate,
    val to: LocalDate,
    val forecastTo: LocalDate,
    val petId: PetId? = null,
)
