package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DeleteEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.ToggleEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.RegisterEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.UpdateEventUseCase
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.springframework.stereotype.Service

@Service
class EventAppService(
    private val eventRepo: EventRepositoryPort,
    private val petRepo: PetRepositoryPort,
    private val clock: ClockPort,
    private val notifier: NotificationPort
) :
    RegisterEventUseCase,
    DeleteEventUseCase,
    UpdateEventUseCase,
    ToggleEventUseCase

{
    override fun execute(cmd: RegisterEventCommand): EventRegisteredResult {
        if (petRepo.findById(cmd.petId) == null) {
            throw IllegalArgumentException("Pet ${cmd.petId.value} not found")
        }
        val toSave = Event(
            type = cmd.type,
            description = cmd.description,
            dateStart = cmd.dateStart,
            recurrence = cmd.recurrence,
            status = Status.PENDING,
            petId = cmd.petId
        )
        val saved = eventRepo.save(toSave)
        notifier.sendEventReminder(saved)
        return EventRegisteredResult(saved.id!!)
    }

    override fun execute(cmd: DeleteEventCommand) {
        eventRepo.delete(cmd.eventId)
    }

    override fun execute(cmd: ToggleEventCommand) {
        val event = eventRepo.findById(cmd.eventId)
            ?: throw IllegalArgumentException("Event ${cmd.eventId.value} not found")
        if (event.status == Status.PENDING) eventRepo.save(event.markDone())
        else eventRepo.save(event.markPending())
    }


    override fun execute(cmd: UpdateEventCommand): EventDetailResult {
        val existing = eventRepo.findById(cmd.eventId)
            ?: throw IllegalArgumentException("Event ${cmd.eventId.value} not found")
        val updated = existing.copy(
            description = cmd.description ?: existing.description,
            recurrence = cmd.recurrence ?: existing.recurrence,
            dateStart = cmd.dateStart ?: existing.dateStart
        )
        val saved = eventRepo.save(updated)
        return saved.toDetailResult()
    }

    fun sendRemindersForToday() {
        val today = clock.now().toLocalDate()
        val pageSize = 50
        val total = petRepo.countAll()
        var page = 0
        while (page * pageSize < total) {
            val pets = petRepo.findAll(page, pageSize)
            pets.forEach { pet ->
                val events = eventRepo.findByPetId(pet.id!!)
                events.filter { it.status == Status.PENDING && it.dateStart.toLocalDate() == today }
                    .forEach { notifier.sendEventReminder(it) }
            }
            page++
        }
    }
}