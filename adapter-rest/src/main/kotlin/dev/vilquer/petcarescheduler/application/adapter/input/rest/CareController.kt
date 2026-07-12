package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.usecase.command.CompleteCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchCareOccurrencesQuery
import dev.vilquer.petcarescheduler.usecase.command.UndoCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CareOccurrenceUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CarePlanUseCase
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrenceResult
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrencesPageResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlanResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlansPageResult
import dev.vilquer.petcarescheduler.usecase.result.TodayCareResult
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

data class CarePlanRequest(
    @field:Positive val petId: Long,
    val type: EventType,
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:Size(max = 2_000) val instructions: String? = null,
    val startAt: LocalDateTime,
    val frequency: Frequency? = null,
    @field:Positive @field:Max(365) val intervalCount: Long = 1,
    @field:Positive @field:Max(10_000) val repetitions: Int? = null,
    val finalDate: LocalDateTime? = null,
    @field:Min(0) @field:Max(10_080) val reminderMinutesBefore: Int = 0,
) {
    fun recurrence(): Recurrence? = frequency?.let { Recurrence(it, intervalCount, repetitions, finalDate) }
}

@RestController
@RequestMapping("/api/v1/care-plans")
@Validated
class CarePlanController(private val care: CarePlanUseCase) {
    @PostMapping
    fun create(@Valid @RequestBody body: CarePlanRequest, @AuthenticationPrincipal jwt: CurrentJwt): ResponseEntity<CarePlanResult> {
        validateDates(body)
        val result = care.create(
            CreateCarePlanCommand(
                PetId(body.petId), body.type, body.title, body.instructions, body.startAt,
                body.recurrence(), body.reminderMinutesBefore,
            ),
            TutorId(jwt.tutorId()),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody body: CarePlanRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): CarePlanResult {
        validateDates(body)
        return care.update(
            UpdateCarePlanCommand(
                CarePlanId(id), body.type, body.title, body.instructions, body.startAt,
                body.recurrence(), body.reminderMinutesBefore,
            ),
            TutorId(jwt.tutorId()),
        )
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) =
        care.get(CarePlanId(id), TutorId(jwt.tutorId()))

    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): CarePlansPageResult = care.list(TutorId(jwt.tutorId()), petId?.let(::PetId), active, page, size)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivate(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) =
        care.deactivate(CarePlanId(id), TutorId(jwt.tutorId()))

    private fun validateDates(body: CarePlanRequest) {
        require(body.finalDate == null || !body.finalDate.isBefore(body.startAt)) { "care_plan_final_before_start" }
        require(body.frequency != null || body.finalDate == null) { "care_plan_final_without_recurrence" }
        require(body.frequency != null || body.repetitions == null) { "care_plan_repetitions_without_recurrence" }
    }
}

data class CareActionRequest(
    val requestId: UUID,
    @field:Size(max = 500) val note: String? = null,
)

@RestController
@RequestMapping("/api/v1/care-occurrences")
@Validated
class CareOccurrenceController(
    private val care: CareOccurrenceUseCase,
    private val clock: ClockPort,
) {
    @GetMapping
    fun search(
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) from: LocalDateTime?,
        @RequestParam(required = false) to: LocalDateTime?,
        @RequestParam(required = false) petId: Long?,
        @RequestParam(required = false) type: EventType?,
        @RequestParam(required = false) status: CareOccurrenceStatus?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): CareOccurrencesPageResult {
        val now = clock.now().toLocalDateTime()
        return care.search(
            SearchCareOccurrencesQuery(
                from ?: now.minusDays(30), to ?: now.plusDays(90), petId?.let(::PetId), type, status, page, size,
            ),
            TutorId(jwt.tutorId()),
        )
    }

    @GetMapping("/today")
    fun today(@AuthenticationPrincipal jwt: CurrentJwt): TodayCareResult = care.today(TutorId(jwt.tutorId()))

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: UUID,
        @Valid @RequestBody body: CareActionRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): CareOccurrenceResult = care.complete(
        CompleteCareOccurrenceCommand(CareOccurrenceId(id), body.requestId, body.note),
        TutorId(jwt.tutorId()),
    )

    @PostMapping("/{id}/undo")
    fun undo(
        @PathVariable id: UUID,
        @Valid @RequestBody body: CareActionRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): CareOccurrenceResult = care.undo(
        UndoCareOccurrenceCommand(CareOccurrenceId(id), body.requestId),
        TutorId(jwt.tutorId()),
    )
}
