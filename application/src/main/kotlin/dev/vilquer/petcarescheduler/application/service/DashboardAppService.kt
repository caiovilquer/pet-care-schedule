package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetDashboardOverviewUseCase
import dev.vilquer.petcarescheduler.usecase.result.DashboardOverviewResult

class DashboardAppService(
    private val tutors: TutorRepositoryPort,
    private val pets: PetRepositoryPort,
    private val occurrences: CareOccurrenceRepositoryPort,
    private val clock: ClockPort,
) : GetDashboardOverviewUseCase {

    override fun getOverview(tutorId: TutorId): DashboardOverviewResult {
        val tutor = tutors.findById(tutorId)
            ?: throw NotFoundException("Tutor ${tutorId.value} not found")
        val now = clock.now().toLocalDateTime()
        val petItems = pets.listByTutor(tutorId, page = 0, size = MAX_PETS)

        return DashboardOverviewResult(
            firstName = tutor.firstName,
            lastName = tutor.lastName,
            email = tutor.email.value,
            avatar = tutor.avatar,
            avatarAssetId = tutor.avatarAssetId,
            totalPets = pets.countByTutor(tutorId),
            totalEvents = occurrences.countByTutor(tutorId),
            pets = petItems.map { it.toSummary() },
            upcomingEvents = occurrences.findUpcoming(tutorId, now, now.plusDays(7), MAX_UPCOMING).map {
                dev.vilquer.petcarescheduler.usecase.result.CareOccurrenceResult(
                    id = it.id.value,
                    version = it.version,
                    planId = it.planId.value,
                    petId = it.petId.value,
                    type = it.type,
                    title = it.title,
                    instructions = it.instructions,
                    dueAt = it.dueAt,
                    status = it.status,
                    completedAt = it.completedAt,
                    completedByTutorId = it.completedByTutorId?.value,
                    completionNote = it.completionNote,
                    canUndoUntil = it.completedAt?.plus(CareAppService.UNDO_WINDOW),
                )
            },
        )
    }

    companion object {
        private const val MAX_PETS = 100
        private const val MAX_UPCOMING = 20
    }
}
