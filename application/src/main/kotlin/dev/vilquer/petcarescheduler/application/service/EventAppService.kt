package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.springframework.stereotype.Service

@Service
class EventAppService(
    private val eventRepo: EventRepositoryPort,
    private val petRepo: PetRepositoryPort,
    private val clock: ClockPort,
    private val notifier: NotificationPort
) {
    fun registerEvent(cmd: RegisterEventCommand): EventRegisteredResult {
        if (petRepo.findById(cmd.petId) == null) {
            throw IllegalArgumentException("Pet ${cmd.petId.value} not found")
        }
        val toSave = Event(
            type = cmd.type,
            description = cmd.description,
            dateStart = cmd.dateStart,
            recurrence = null,
            status = Status.PENDING,
            petId = cmd.petId
        )
        val saved = eventRepo.save(toSave)
        notifier.sendEventReminder(saved)
        return EventRegisteredResult(saved.id!!)
    }

    fun deleteEvent(cmd: DeleteEventCommand) {
        val event = eventRepo.findById(cmd.eventId)
            ?: throw IllegalArgumentException("Event ${cmd.eventId.value} not found")
        eventRepo.save(event.markDone())
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