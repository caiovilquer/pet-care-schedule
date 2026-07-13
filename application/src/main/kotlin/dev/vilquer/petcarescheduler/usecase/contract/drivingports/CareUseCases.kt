package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.command.CompleteCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchCareOccurrencesQuery
import dev.vilquer.petcarescheduler.usecase.command.UndoCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.AssignCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrenceResult
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrencesPageResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlanResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlansPageResult
import dev.vilquer.petcarescheduler.usecase.result.TodayCareResult

interface CarePlanUseCase {
    fun create(command: CreateCarePlanCommand, access: HouseholdAccess): CarePlanResult
    fun update(command: UpdateCarePlanCommand, access: HouseholdAccess): CarePlanResult
    fun deactivate(planId: CarePlanId, access: HouseholdAccess)
    fun get(planId: CarePlanId, access: HouseholdAccess): CarePlanResult
    fun list(access: HouseholdAccess, petId: PetId?, active: Boolean?, page: Int, size: Int): CarePlansPageResult
}

interface CareOccurrenceUseCase {
    fun search(query: SearchCareOccurrencesQuery, access: HouseholdAccess): CareOccurrencesPageResult
    fun today(access: HouseholdAccess): TodayCareResult
    fun complete(command: CompleteCareOccurrenceCommand, access: HouseholdAccess): CareOccurrenceResult
    fun undo(command: UndoCareOccurrenceCommand, access: HouseholdAccess): CareOccurrenceResult
    fun assign(command: AssignCareOccurrenceCommand, access: HouseholdAccess): CareOccurrenceResult
}

fun interface CareScheduleMaintenanceUseCase {
    fun materializeAndEnqueueReminders()
}

fun interface DispatchPendingCareRemindersUseCase {
    fun dispatchPendingCareReminders()
}

fun interface DispatchPendingCareEscalationsUseCase { fun dispatchPendingCareEscalations() }
