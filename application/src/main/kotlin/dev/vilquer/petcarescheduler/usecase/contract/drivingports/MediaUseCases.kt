package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.command.CompleteMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.command.DeleteMediaCommand
import dev.vilquer.petcarescheduler.usecase.command.InitiateMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.result.MediaAssetResult
import dev.vilquer.petcarescheduler.usecase.result.MediaUploadInitiatedResult
import java.util.UUID

interface MediaUploadUseCase {
    fun initiate(command: InitiateMediaUploadCommand, access: HouseholdAccess): MediaUploadInitiatedResult
    fun complete(command: CompleteMediaUploadCommand, access: HouseholdAccess): MediaAssetResult
    fun delete(command: DeleteMediaCommand, access: HouseholdAccess)
    fun downloadUrl(mediaId: UUID, access: HouseholdAccess? = null): String
}

interface MediaMaintenanceUseCase { fun cleanupMedia() }
