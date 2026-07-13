package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentHousehold
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthMeasurementCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthRecordCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchHealthRecordsQuery
import dev.vilquer.petcarescheduler.usecase.command.UpdateHealthMeasurementCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateHealthRecordCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HealthMeasurementUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HealthRecordUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.MediaUploadUseCase
import dev.vilquer.petcarescheduler.usecase.result.HealthMeasurementResult
import dev.vilquer.petcarescheduler.usecase.result.HealthRecordResult
import dev.vilquer.petcarescheduler.usecase.result.HealthRecordsPageResult
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.CacheControl
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class HealthRecordCreateRequest(
    val type: HealthRecordType,
    val occurredAt: Instant,
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:Size(max = 4_000) val notes: String? = null,
    @field:Size(max = 160) val productName: String? = null,
    @field:Size(max = 120) val dosage: String? = null,
    @field:Size(max = 120) val batchNumber: String? = null,
    @field:Size(max = 160) val professionalName: String? = null,
    @field:Size(max = 160) val clinicName: String? = null,
    @field:DecimalMin("0.00") @field:DecimalMax("9999999999.99") @field:Digits(integer = 10, fraction = 2)
    val costAmount: BigDecimal? = null,
    @field:Pattern(regexp = "^[A-Za-z]{3}$") val currency: String? = null,
)

data class HealthRecordUpdateRequest(
    @field:PositiveOrZero val version: Long,
    val type: HealthRecordType,
    val occurredAt: Instant,
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:Size(max = 4_000) val notes: String? = null,
    @field:Size(max = 160) val productName: String? = null,
    @field:Size(max = 120) val dosage: String? = null,
    @field:Size(max = 120) val batchNumber: String? = null,
    @field:Size(max = 160) val professionalName: String? = null,
    @field:Size(max = 160) val clinicName: String? = null,
    @field:DecimalMin("0.00") @field:DecimalMax("9999999999.99") @field:Digits(integer = 10, fraction = 2)
    val costAmount: BigDecimal? = null,
    @field:Pattern(regexp = "^[A-Za-z]{3}$") val currency: String? = null,
)

@RestController
@RequestMapping("/api/v1")
@Validated
class HealthRecordController(
    private val records: HealthRecordUseCase,
    private val clock: ClockPort,
    private val household: CurrentHousehold,
) {
    @PostMapping("/pets/{petId}/health-records")
    fun create(
        @PathVariable petId: Long,
        @Valid @RequestBody body: HealthRecordCreateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): ResponseEntity<HealthRecordResult> = ResponseEntity.status(HttpStatus.CREATED).body(
        records.create(body.toCommand(PetId(petId)), household.resolve(jwt)),
    )

    @PutMapping("/health-records/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody body: HealthRecordUpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): HealthRecordResult = records.update(body.toCommand(HealthRecordId(id)), household.resolve(jwt))

    @GetMapping("/health-records/{id}")
    fun get(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt): HealthRecordResult =
        records.get(HealthRecordId(id), household.resolve(jwt))

    @GetMapping("/pets/{petId}/health-records")
    fun search(
        @PathVariable petId: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) type: HealthRecordType?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): HealthRecordsPageResult {
        val now = clock.now().toInstant()
        return records.search(
            SearchHealthRecordsQuery(
                PetId(petId), from ?: now.minusSeconds(DEFAULT_RECORD_PERIOD_SECONDS),
                to ?: now.plusSeconds(DEFAULT_FUTURE_WINDOW_SECONDS), type, page, size,
            ),
            household.resolve(jwt),
        )
    }

    @DeleteMapping("/health-records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        @RequestParam @PositiveOrZero version: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = records.delete(HealthRecordId(id), version, household.resolve(jwt))

    private fun HealthRecordCreateRequest.toCommand(petId: PetId) = CreateHealthRecordCommand(
        petId, type, occurredAt, title, notes, productName, dosage, batchNumber,
        professionalName, clinicName, costAmount, currency,
    )

    private fun HealthRecordUpdateRequest.toCommand(id: HealthRecordId) = UpdateHealthRecordCommand(
        id, version, type, occurredAt, title, notes, productName, dosage, batchNumber,
        professionalName, clinicName, costAmount, currency,
    )

    companion object {
        private const val DEFAULT_RECORD_PERIOD_SECONDS = 365L * 10 * 24 * 60 * 60
        private const val DEFAULT_FUTURE_WINDOW_SECONDS = 24L * 60 * 60
    }
}

data class HealthMeasurementCreateRequest(
    val type: HealthMeasurementType,
    @field:DecimalMin("0.01") @field:DecimalMax("500.00") @field:Digits(integer = 3, fraction = 2)
    val value: BigDecimal,
    val measuredAt: Instant,
    @field:Size(max = 500) val notes: String? = null,
)

data class HealthMeasurementUpdateRequest(
    @field:PositiveOrZero val version: Long,
    @field:DecimalMin("0.01") @field:DecimalMax("500.00") @field:Digits(integer = 3, fraction = 2)
    val value: BigDecimal,
    val measuredAt: Instant,
    @field:Size(max = 500) val notes: String? = null,
)

@RestController
@RequestMapping("/api/v1")
@Validated
class HealthMeasurementController(
    private val measurements: HealthMeasurementUseCase,
    private val clock: ClockPort,
    private val household: CurrentHousehold,
) {
    @PostMapping("/pets/{petId}/health-measurements")
    fun create(
        @PathVariable petId: Long,
        @Valid @RequestBody body: HealthMeasurementCreateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): ResponseEntity<HealthMeasurementResult> = ResponseEntity.status(HttpStatus.CREATED).body(
        measurements.create(
            CreateHealthMeasurementCommand(PetId(petId), body.type, body.value, body.measuredAt, body.notes),
            household.resolve(jwt),
        ),
    )

    @PutMapping("/health-measurements/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody body: HealthMeasurementUpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): HealthMeasurementResult = measurements.update(
        UpdateHealthMeasurementCommand(HealthMeasurementId(id), body.version, body.value, body.measuredAt, body.notes),
        household.resolve(jwt),
    )

    @GetMapping("/pets/{petId}/health-measurements")
    fun list(
        @PathVariable petId: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam(required = false) type: HealthMeasurementType?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ): List<HealthMeasurementResult> {
        val now = clock.now().toInstant()
        return measurements.list(
            PetId(petId), type, from ?: now.minusSeconds(DEFAULT_MEASUREMENT_PERIOD_SECONDS),
            to ?: now.plusSeconds(DEFAULT_FUTURE_WINDOW_SECONDS), household.resolve(jwt),
        )
    }

    @DeleteMapping("/health-measurements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        @RequestParam @PositiveOrZero version: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = measurements.delete(HealthMeasurementId(id), version, household.resolve(jwt))

    companion object {
        private const val DEFAULT_MEASUREMENT_PERIOD_SECONDS = 365L * 5 * 24 * 60 * 60
        private const val DEFAULT_FUTURE_WINDOW_SECONDS = 24L * 60 * 60
    }
}

data class HealthAttachmentDownloadResponse(val url: String)

@RestController
@RequestMapping("/api/v1/health-attachments")
class HealthAttachmentController(private val media: MediaUploadUseCase, private val household: CurrentHousehold) {
    @GetMapping("/{mediaId}/download-url")
    fun downloadUrl(
        @PathVariable mediaId: UUID,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): ResponseEntity<HealthAttachmentDownloadResponse> = ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Referrer-Policy", "no-referrer")
        .header("X-Content-Type-Options", "nosniff")
        .body(HealthAttachmentDownloadResponse(media.downloadUrl(mediaId, household.resolve(jwt))))
}
