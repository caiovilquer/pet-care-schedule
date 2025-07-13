package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.result.*
import org.springframework.stereotype.Service

@Service
class TutorAppService(
    private val tutorRepo: TutorRepositoryPort
) {
    fun createTutor(cmd: CreateTutorCommand): TutorCreatedResult {
        val tutor = Tutor(
            firstName = cmd.firstName,
            lastName = cmd.lastName,
            email = cmd.email,
            passwordHash = cmd.rawPassword,
            phoneNumber = cmd.phoneNumber,
            avatar = cmd.avatar
        )
        val saved = tutorRepo.save(tutor)
        return TutorCreatedResult(saved.id!!)
    }

    fun listTutors(page: Int, size: Int): TutorsPageResult {
        val items = tutorRepo.findAll(page, size).map { it.toSummary() }
        val total = tutorRepo.countAll()
        return TutorsPageResult(items, total, page, size)
    }

    fun updateTutor(cmd: UpdateTutorCommand): TutorDetailResult {
        val existing = tutorRepo.findById(cmd.tutorId)
            ?: throw IllegalArgumentException("Tutor ${cmd.tutorId.value} not found")
        val updated = existing.copy(
            firstName = cmd.firstName ?: existing.firstName,
            lastName = cmd.lastName ?: existing.lastName,
            phoneNumber = cmd.phoneNumber ?: existing.phoneNumber,
            avatar = cmd.avatar ?: existing.avatar
        )
        val saved = tutorRepo.save(updated)
        return saved.toDetailResult()
    }

    fun deleteTutor(cmd: DeleteTutorCommand) {
        tutorRepo.delete(cmd.tutorId)
    }

    fun getTutor(id: TutorId): TutorDetailResult =
        tutorRepo.findById(id)?.toDetailResult()
            ?: throw IllegalArgumentException("Tutor ${id.value} not found")
}