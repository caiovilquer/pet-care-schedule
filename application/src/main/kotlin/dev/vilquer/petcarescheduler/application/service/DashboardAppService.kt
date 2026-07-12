package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.GetDashboardOverviewUseCase
import dev.vilquer.petcarescheduler.usecase.result.DashboardOverviewResult

class DashboardAppService(
    private val tutors: TutorRepositoryPort,
    private val pets: PetRepositoryPort,
    private val events: EventRepositoryPort,
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
            totalEvents = events.countByTutor(tutorId),
            pets = petItems.map { it.toSummary() },
            upcomingEvents = events.findUpcomingByTutor(
                tutorId = tutorId,
                start = now,
                end = now.plusDays(7),
                limit = MAX_UPCOMING,
            ).map { it.toSummary() },
        )
    }

    companion object {
        private const val MAX_PETS = 100
        private const val MAX_UPCOMING = 20
    }
}
