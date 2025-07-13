package dev.vilquer.petcarescheduler.application

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import java.time.ZonedDateTime

internal class FakeClock(var fixed: ZonedDateTime) : ClockPort {
    override fun now(): ZonedDateTime = fixed
}

internal class FakeNotifier : NotificationPort {
    val notified = mutableListOf<Event>()
    override fun sendEventReminder(event: Event) { notified.add(event) }
}

internal class InMemoryTutorRepo(initial: Map<TutorId, Tutor> = emptyMap()) : TutorRepositoryPort {
    private val store = LinkedHashMap<TutorId, Tutor>().apply { putAll(initial) }
    private var seq = (store.keys.map { it.value }.maxOrNull() ?: 0L) + 1
    override fun save(tutor: Tutor): Tutor {
        val id = tutor.id ?: TutorId(seq++)
        val saved = tutor.copy(id = id)
        store[id] = saved
        return saved
    }
    override fun findById(id: TutorId): Tutor? = store[id]
    override fun findAll(page: Int, size: Int): List<Tutor> =
        store.values.drop(page * size).take(size)
    override fun countAll(): Long = store.size.toLong()
    override fun delete(id: TutorId) { store.remove(id) }
}

internal class InMemoryPetRepo(initial: Map<PetId, Pet> = emptyMap()) : PetRepositoryPort {
    private val store = LinkedHashMap<PetId, Pet>().apply { putAll(initial) }
    private var seq = (store.keys.map { it.value }.maxOrNull() ?: 0L) + 1
    override fun save(pet: Pet): Pet {
        val id = pet.id ?: PetId(seq++)
        val saved = pet.copy(id = id)
        store[id] = saved
        return saved
    }
    override fun findById(id: PetId): Pet? = store[id]
    override fun delete(id: PetId) { store.remove(id) }
    override fun findAll(page: Int, size: Int): List<Pet> =
        store.values.drop(page * size).take(size)
    override fun countAll(): Long = store.size.toLong()
}

internal class InMemoryEventRepo(initial: Map<EventId, Event> = emptyMap()) : EventRepositoryPort {
    private val store = LinkedHashMap<EventId, Event>().apply { putAll(initial) }
    private var seq = (store.keys.map { it.value }.maxOrNull() ?: 0L) + 1
    override fun save(event: Event): Event {
        val id = event.id ?: EventId(seq++)
        val saved = event.copy(id = id)
        store[id] = saved
        return saved
    }
    override fun findById(id: EventId): Event? = store[id]
    override fun findByPetId(petId: PetId): List<Event> =
        store.values.filter { it.petId == petId }

    fun allEvents(): Collection<Event> = store.values
}
