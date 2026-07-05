package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*

class TutorAppService(
    private val tutorRepo: TutorRepositoryPort,
    private val passwordHash: PasswordHashPort,
    private val petRepo: PetRepositoryPort
) :
    CreateTutorUseCase,
    UpdateTutorUseCase,
    DeleteTutorUseCase,
    GetTutorUseCase {
    override fun execute(cmd: CreateTutorCommand): TutorCreatedResult {
        tutorRepo.findByEmail(cmd.email)?.let {
            throw ConflictException("E-mail ${cmd.email.value} already exists")
        }
        val tutor = Tutor(
            firstName = cmd.firstName,
            lastName = cmd.lastName,
            email = cmd.email,
            passwordHash = passwordHash.hash(cmd.rawPassword),
            passwordChangedAt = java.time.Instant.now(),
            phoneNumber = cmd.phoneNumber,
            avatar = cmd.avatar
        )
        val saved = tutorRepo.save(tutor)
        return TutorCreatedResult(saved.id!!)
    }

    override fun execute(cmd: UpdateTutorCommand): TutorDetailResult {
        val existing = tutorRepo.findById(cmd.tutorId)
            ?: throw NotFoundException("Tutor ${cmd.tutorId.value} not found")
        val updated = existing.copy(
            firstName = cmd.firstName ?: existing.firstName,
            lastName = cmd.lastName ?: existing.lastName,
            phoneNumber = cmd.phoneNumber ?: existing.phoneNumber,
            avatar = cmd.avatar ?: existing.avatar
        )
        val saved = tutorRepo.save(updated)
        return saved.toDetailResult(petRepo.findAllByTutor(saved.id!!))
    }

    override fun execute(cmd: DeleteTutorCommand) {
        tutorRepo.delete(cmd.tutorId)
    }

    override fun get(id: TutorId): TutorDetailResult {
        val tutor = tutorRepo.findById(id) ?: throw NotFoundException("Tutor ${id.value} not found")
        return tutor.toDetailResult(petRepo.findAllByTutor(id))
    }
}
