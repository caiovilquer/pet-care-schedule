package dev.vilquer.petcarescheduler.core.domain.finance

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID

@JvmInline value class ExpenseId(val value: UUID)

enum class ExpenseCategory {
    VETERINARY,
    MEDICATION,
    VACCINE,
    EXAM,
    FOOD,
    HYGIENE,
    SERVICE,
    INSURANCE,
    OTHER,
}

data class Expense(
    val id: ExpenseId = ExpenseId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val petId: PetId,
    val category: ExpenseCategory,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val occurredAt: Instant,
    val notes: String? = null,
    val createdByTutorId: TutorId,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(description.isNotBlank() && description.length <= 160) { "expense_description_invalid" }
        require(amount > BigDecimal.ZERO && amount <= MAX_AMOUNT && amount.scale() <= 2) { "expense_amount_invalid" }
        require(currency.matches(Regex("^[A-Z]{3}$"))) { "expense_currency_invalid" }
        runCatching { Currency.getInstance(currency) }.getOrElse { throw IllegalArgumentException("expense_currency_invalid") }
        require(notes == null || notes.length <= 1_000) { "expense_notes_invalid" }
    }

    companion object { private val MAX_AMOUNT = BigDecimal("9999999999.99") }
}
