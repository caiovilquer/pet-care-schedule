package dev.vilquer.petcarescheduler.application

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

internal class FakeClock(var fixed: ZonedDateTime) : ClockPort {
    override fun now(): ZonedDateTime = fixed
}

internal class FakeNotifier : NotificationPort {
    val notified = mutableListOf<Event>()
    var deliverySucceeds = true
    override fun sendEventReminder(target: EventReminderTarget): Boolean {
        notified.add(target.event)
        return deliverySucceeds
    }
}

internal class FakeReminderOutboxPort : ReminderOutboxPort {
    private val store = LinkedHashMap<Long, ReminderOutboxMessage>()
    private val sentIds = mutableSetOf<Long>()
    private var seq = 1L
    override fun enqueueIfAbsent(message: ReminderOutboxMessage) {
        if (store.values.any { it.eventId == message.eventId }) return
        val id = seq++
        store[id] = message.copy(id = id)
    }
    override fun findPendingDelivery(maxAttempts: Int, limit: Int): List<ReminderOutboxMessage> =
        store.values.filter { it.id !in sentIds && it.attempts < maxAttempts }
            .sortedBy { it.createdAt }
            .take(limit)
    override fun markSent(id: Long) { sentIds.add(id) }
    override fun incrementAttempts(id: Long) {
        store[id]?.let { store[id] = it.copy(attempts = it.attempts + 1) }
    }
    fun allMessages(): Collection<ReminderOutboxMessage> = store.values
    fun isSent(id: Long?): Boolean = id != null && id in sentIds
}

internal class FakePasswordHash : PasswordHashPort {
    override fun hash(raw: CharSequence): String = "hashed-$raw"
    override fun matches(raw: CharSequence, hash: String): Boolean = hash(raw) == hash
}

/** Executa o bloco diretamente, sem transação real — suficiente para testar orquestração. */
internal class FakeTransactionPort : TransactionPort {
    override fun <T> execute(block: () -> T): T = block()
}

internal class InMemoryPasswordResetTokenPort : PasswordResetTokenPort {
    private val store = LinkedHashMap<UUID, PasswordResetToken>()
    override fun create(token: PasswordResetToken): PasswordResetToken {
        store[token.id] = token
        return token
    }
    override fun findActiveByHash(tokenHash: String): PasswordResetToken? =
        store.values.firstOrNull { it.tokenHash == tokenHash && it.usedAt == null }
    override fun markUsed(id: UUID, usedAt: Instant) {
        store[id]?.let { store[id] = it.copy(usedAt = usedAt) }
    }
    override fun invalidateAllForUser(userId: TutorId, usedAt: Instant) {
        store.values.filter { it.userId == userId && it.usedAt == null }
            .forEach { store[it.id] = it.copy(usedAt = usedAt) }
    }
    override fun cleanup(now: Instant) {
        store.values.filter { it.expiresAt.isBefore(now) }.forEach { store.remove(it.id) }
    }
    fun allTokens(): Collection<PasswordResetToken> = store.values
}

internal class FakePasswordResetNotifier : PasswordResetNotifierPort {
    data class SentLink(val to: Email, val tokenPlain: String, val ttl: Duration)
    val sent = mutableListOf<SentLink>()
    override fun sendResetLink(to: Email, tokenPlain: String, ttl: Duration) {
        sent.add(SentLink(to, tokenPlain, ttl))
    }
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
    override fun findAllByTutor(tutorId: TutorId): List<Pet> =
        store.values.filter { it.tutorId == tutorId }
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
