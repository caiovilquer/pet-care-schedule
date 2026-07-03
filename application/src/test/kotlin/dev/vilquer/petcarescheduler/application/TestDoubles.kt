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
    override fun sendEventReminder(event: Event, tutorEmail: String, petName: String?) { notified.add(event) }
}

internal class FakePasswordHash : PasswordHashPort {
    override fun hash(raw: CharSequence): String = "hashed-$raw"
    override fun matches(raw: CharSequence, hash: String): Boolean = hash(raw) == hash
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
    override fun findByEmail(email: dev.vilquer.petcarescheduler.core.domain.valueobject.Email): Tutor? =
        store.values.firstOrNull { it.email.value == email.value }
    override fun findAll(page: Int, size: Int): List<Tutor> =
        store.values.drop(page * size).take(size)
    override fun countAll(): Long = store.size.toLong()
    override fun delete(id: TutorId) { store.remove(id) }
    override fun updatePassword(id: TutorId, passwordHash: String) {
        val existing = store[id] ?: return
        store[id] = existing.copy(passwordHash = passwordHash)
    }
    override fun bumpPasswordChangedAt(id: TutorId, whenUtc: java.time.Instant) {
        val existing = store[id] ?: return
        store[id] = existing.copy(passwordChangedAt = whenUtc)
    }
    override fun findPasswordChangedAt(id: TutorId): java.time.Instant? =
        store[id]?.passwordChangedAt
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
    override fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Pet> =
        store.values.filter { it.tutorId == tutorId }.drop(page * size).take(size)
    override fun countByTutor(tutorId: TutorId): Long =
        store.values.count { it.tutorId == tutorId }.toLong()
    override fun findByIdAndTutor(id: PetId, tutorId: TutorId): Pet? =
        store[id]?.takeIf { it.tutorId == tutorId }
    override fun existsForTutor(id: PetId, tutorId: TutorId): Boolean =
        store[id]?.tutorId == tutorId
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
    override fun delete(id: EventId) {
        store.remove(id)
    }
    override fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Event> =
        emptyList()
    override fun countByTutor(tutorId: TutorId): Long = 0
    override fun findByIdAndTutor(id: EventId, tutorId: TutorId): Event? = store[id]
    override fun existsForTutor(id: EventId, tutorId: TutorId): Boolean = store.containsKey(id)
    override fun findPendingReminders(start: java.time.LocalDateTime, end: java.time.LocalDateTime):
        List<EventReminderTarget> =
        store.values.filter {
            it.status == Status.PENDING && !it.dateStart.isBefore(start) && it.dateStart.isBefore(end)
        }.map { EventReminderTarget(it, "test@example.com", null) }

    fun allEvents(): Collection<Event> = store.values
}
