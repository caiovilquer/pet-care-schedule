package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.CompleteMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.command.DeleteMediaCommand
import dev.vilquer.petcarescheduler.usecase.command.InitiateMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.result.MediaAssetResult
import dev.vilquer.petcarescheduler.usecase.result.MediaUploadInitiatedResult
import java.util.UUID

interface MediaUploadUseCase {
    fun initiate(command: InitiateMediaUploadCommand, tutorId: TutorId): MediaUploadInitiatedResult
    fun complete(command: CompleteMediaUploadCommand, tutorId: TutorId): MediaAssetResult
    fun delete(command: DeleteMediaCommand, tutorId: TutorId)
    fun downloadUrl(mediaId: UUID): String
}

interface MediaMaintenanceUseCase { fun cleanupMedia() }
