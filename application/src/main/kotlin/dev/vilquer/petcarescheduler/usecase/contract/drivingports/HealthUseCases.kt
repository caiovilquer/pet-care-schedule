package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthMeasurementCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthRecordCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchHealthRecordsQuery
import dev.vilquer.petcarescheduler.usecase.command.UpdateHealthMeasurementCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateHealthRecordCommand
import dev.vilquer.petcarescheduler.usecase.result.HealthMeasurementResult
import dev.vilquer.petcarescheduler.usecase.result.HealthRecordResult
import dev.vilquer.petcarescheduler.usecase.result.HealthRecordsPageResult
import java.time.Instant

interface HealthRecordUseCase {
    fun create(command: CreateHealthRecordCommand, access: HouseholdAccess): HealthRecordResult
    fun update(command: UpdateHealthRecordCommand, access: HouseholdAccess): HealthRecordResult
    fun get(recordId: HealthRecordId, access: HouseholdAccess): HealthRecordResult
    fun search(query: SearchHealthRecordsQuery, access: HouseholdAccess): HealthRecordsPageResult
    fun delete(recordId: HealthRecordId, expectedVersion: Long, access: HouseholdAccess)
}

interface HealthMeasurementUseCase {
    fun create(command: CreateHealthMeasurementCommand, access: HouseholdAccess): HealthMeasurementResult
    fun update(command: UpdateHealthMeasurementCommand, access: HouseholdAccess): HealthMeasurementResult
    fun list(
        petId: PetId,
        type: HealthMeasurementType?,
        from: Instant,
        to: Instant,
        access: HouseholdAccess,
    ): List<HealthMeasurementResult>
    fun delete(measurementId: HealthMeasurementId, expectedVersion: Long, access: HouseholdAccess)
}
