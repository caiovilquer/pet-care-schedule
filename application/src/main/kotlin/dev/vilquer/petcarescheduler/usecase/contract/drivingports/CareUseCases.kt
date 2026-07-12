package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.CompleteCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchCareOccurrencesQuery
import dev.vilquer.petcarescheduler.usecase.command.UndoCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrenceResult
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrencesPageResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlanResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlansPageResult
import dev.vilquer.petcarescheduler.usecase.result.TodayCareResult

interface CarePlanUseCase {
    fun create(command: CreateCarePlanCommand, tutorId: TutorId): CarePlanResult
    fun update(command: UpdateCarePlanCommand, tutorId: TutorId): CarePlanResult
    fun deactivate(planId: CarePlanId, tutorId: TutorId)
    fun get(planId: CarePlanId, tutorId: TutorId): CarePlanResult
    fun list(tutorId: TutorId, petId: PetId?, active: Boolean?, page: Int, size: Int): CarePlansPageResult
}

interface CareOccurrenceUseCase {
    fun search(query: SearchCareOccurrencesQuery, tutorId: TutorId): CareOccurrencesPageResult
    fun today(tutorId: TutorId): TodayCareResult
    fun complete(command: CompleteCareOccurrenceCommand, tutorId: TutorId): CareOccurrenceResult
    fun undo(command: UndoCareOccurrenceCommand, tutorId: TutorId): CareOccurrenceResult
}

fun interface CareScheduleMaintenanceUseCase {
    fun materializeAndEnqueueReminders()
}

fun interface DispatchPendingCareRemindersUseCase {
    fun dispatchPendingCareReminders()
}
