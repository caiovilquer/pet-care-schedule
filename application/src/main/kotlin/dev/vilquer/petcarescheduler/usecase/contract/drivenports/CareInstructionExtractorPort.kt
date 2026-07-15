package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftField
import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftFieldProvenance
import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftFields
import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftWarning
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import java.time.Instant
import java.time.ZoneId

data class CareInstructionPetContext(val id: PetId, val name: String)

data class CareInstructionExtractionRequest(
    val instruction: String,
    val receivedAt: Instant,
    val zoneId: ZoneId,
    val pets: List<CareInstructionPetContext>,
)

data class ExtractedCareDraft(
    val fields: CareDraftFields,
    val evidence: Map<CareDraftField, String> = emptyMap(),
    val missingFields: Set<CareDraftField> = emptySet(),
    val warnings: List<CareDraftWarning> = emptyList(),
    val provenance: Map<CareDraftField, CareDraftFieldProvenance> = emptyMap(),
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
)

class CareInstructionExtractionException(
    val code: String,
    val retryable: Boolean,
    cause: Throwable? = null,
) : RuntimeException(code, cause)

interface CareInstructionExtractorPort {
    val provider: String
    val model: String
    val promptVersion: String
    fun extract(request: CareInstructionExtractionRequest): ExtractedCareDraft
}
