package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftField
import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftFields
import dev.vilquer.petcarescheduler.core.domain.assistant.CareDraftId
import java.util.UUID

data class GenerateCareDraftCommand(
    val instruction: String,
    val requestId: UUID,
)

data class CorrectCareDraftCommand(
    val draftId: CareDraftId,
    val expectedVersion: Long,
    val fields: CareDraftFields,
    val requestId: UUID,
)

data class ConfirmCareDraftCommand(
    val draftId: CareDraftId,
    val expectedVersion: Long,
    val requestId: UUID,
)

data class CancelCareDraftCommand(
    val draftId: CareDraftId,
    val expectedVersion: Long,
    val requestId: UUID,
)

data class AddCareDraftFeedbackCommand(
    val draftId: CareDraftId,
    val positive: Boolean,
    val correctedFields: Set<CareDraftField> = emptySet(),
    val reason: String? = null,
    val comment: String? = null,
    val requestId: UUID,
)
