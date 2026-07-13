package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentHousehold
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseCategory
import dev.vilquer.petcarescheduler.core.domain.finance.ExpenseId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.ExpenseUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.FinanceOverviewUseCase
import dev.vilquer.petcarescheduler.usecase.result.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ExpenseCreateRequest(
    @field:Positive val petId: Long,
    val category: ExpenseCategory,
    @field:NotBlank @field:Size(max = 160) val description: String,
    @field:DecimalMin("0.01") @field:DecimalMax("9999999999.99") @field:Digits(integer = 10, fraction = 2) val amount: BigDecimal,
    @field:Pattern(regexp = "^[A-Za-z]{3}$") val currency: String = "BRL",
    val occurredAt: Instant,
    @field:Size(max = 1_000) val notes: String? = null,
)

data class ExpenseUpdateRequest(
    @field:PositiveOrZero val version: Long,
    val category: ExpenseCategory,
    @field:NotBlank @field:Size(max = 160) val description: String,
    @field:DecimalMin("0.01") @field:DecimalMax("9999999999.99") @field:Digits(integer = 10, fraction = 2) val amount: BigDecimal,
    @field:Pattern(regexp = "^[A-Za-z]{3}$") val currency: String = "BRL",
    val occurredAt: Instant,
    @field:Size(max = 1_000) val notes: String? = null,
)

@RestController
@RequestMapping("/api/v1")
@Validated
class FinanceController(
    private val expenses: ExpenseUseCase,
    private val finance: FinanceOverviewUseCase,
    private val household: CurrentHousehold,
    private val clock: ClockPort,
) {
    @PostMapping("/expenses")
    fun create(@Valid @RequestBody body: ExpenseCreateRequest, @AuthenticationPrincipal jwt: CurrentJwt): ResponseEntity<ExpenseResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(expenses.create(body.toCommand(), household.resolve(jwt)))

    @PutMapping("/expenses/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody body: ExpenseUpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): ExpenseResult = expenses.update(body.toCommand(ExpenseId(id)), household.resolve(jwt))

    @GetMapping("/expenses")
    fun search(
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) category: ExpenseCategory?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ExpensesPageResult {
        val now = clock.now().toInstant()
        return expenses.search(
            SearchExpensesQuery(from ?: now.minusSeconds(365L * 24 * 3600), to ?: now.plusSeconds(300), petId?.let(::PetId), category, page, size),
            household.resolve(jwt),
        )
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        @RequestParam @PositiveOrZero version: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = expenses.delete(ExpenseId(id), version, household.resolve(jwt))

    @GetMapping("/finances/overview")
    fun overview(
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) forecastTo: LocalDate?,
        @RequestParam(required = false) petId: Long?,
    ): FinanceOverviewResult {
        val today = clock.now().toLocalDate()
        return finance.overview(
            FinanceOverviewQuery(from ?: today.withDayOfMonth(1).minusMonths(5), to ?: today, forecastTo ?: today.plusDays(30), petId?.let(::PetId)),
            household.resolve(jwt),
        )
    }

    private fun ExpenseCreateRequest.toCommand() = CreateExpenseCommand(PetId(petId), category, description, amount, currency, occurredAt, notes)
    private fun ExpenseUpdateRequest.toCommand(id: ExpenseId) = UpdateExpenseCommand(id, version, category, description, amount, currency, occurredAt, notes)
}
