package dev.vilquer.petcarescheduler.application

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import dev.vilquer.petcarescheduler.core.domain.session.RefreshToken
import dev.vilquer.petcarescheduler.core.domain.media.MediaAsset
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.result.GeoLocation
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import dev.vilquer.petcarescheduler.usecase.result.PlaceSummary
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
    override fun resetForEvent(eventId: EventId) {
        store.entries.removeIf { it.value.eventId == eventId }
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

internal class InMemoryMediaAssetRepo : MediaAssetRepositoryPort {
    private val store = LinkedHashMap<UUID, MediaAsset>()
    override fun save(asset: MediaAsset): MediaAsset = asset.also { store[it.id] = it }
    override fun findById(id: UUID): MediaAsset? = store[id]
    override fun delete(id: UUID) { store.remove(id) }
    override fun findCleanupCandidates(pendingBefore: Instant, limit: Int): List<MediaAsset> =
        store.values.filter {
            it.status == MediaStatus.PENDING_DELETE ||
                (it.status == MediaStatus.PENDING && it.createdAt.isBefore(pendingBefore))
        }.take(limit)
    override fun markPetAssetsForDeletion(petId: PetId) = Unit
    override fun markTutorAssetsForDeletion(tutorId: TutorId) = Unit
    fun all(): Collection<MediaAsset> = store.values
}

internal class FakeObjectStorage : ObjectStoragePort {
    val objects = LinkedHashMap<String, ByteArray>()
    val deleted = mutableListOf<String>()
    var failDeletes = false
    override fun presignUpload(key: String, contentType: String, checksumSha256: String, expiresIn: Duration) =
        PresignedUpload("https://storage.example/$key?signature=test", mapOf("content-type" to contentType))
    override fun readObject(key: String, maxBytes: Long): ByteArray =
        objects[key]?.takeIf { it.size <= maxBytes } ?: error("object_not_found")
    override fun promote(sourceKey: String, destinationKey: String, contentType: String) {
        objects[destinationKey] = objects.remove(sourceKey) ?: error("object_not_found")
    }
    override fun delete(key: String) {
        if (failDeletes) error("storage_unavailable")
        objects.remove(key)
        deleted += key
    }
    override fun presignDownload(key: String, expiresIn: Duration): String =
        "https://storage.example/$key?signature=download"
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

internal class InMemoryRefreshTokenPort : RefreshTokenPort {
    private val store = LinkedHashMap<UUID, RefreshToken>()
    override fun create(token: RefreshToken): RefreshToken {
        store[token.id] = token
        return token
    }
    override fun findByHash(tokenHash: String): RefreshToken? =
        store.values.firstOrNull { it.tokenHash == tokenHash }
    override fun markRotated(id: UUID, replacedBy: UUID, at: Instant) {
        store[id]?.let { store[id] = it.copy(usedAt = at, replacedBy = replacedBy) }
    }
    override fun revokeFamily(familyId: UUID, at: Instant) {
        store.values.filter { it.familyId == familyId && it.revokedAt == null }
            .forEach { store[it.id] = it.copy(revokedAt = at) }
    }
    override fun revokeAllForUser(userId: TutorId, at: Instant) {
        store.values.filter { it.userId == userId && it.revokedAt == null }
            .forEach { store[it.id] = it.copy(revokedAt = at) }
    }
    override fun cleanup(now: Instant) {
        store.values.filter { it.expiresAt.isBefore(now) }.forEach { store.remove(it.id) }
    }
    fun allTokens(): Collection<RefreshToken> = store.values
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

internal class InMemoryEventRepo(
    initial: Map<EventId, Event> = emptyMap(),
    private val countsByTutor: Map<TutorId, Long> = emptyMap(),
) : EventRepositoryPort {
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
    override fun countByTutor(tutorId: TutorId): Long = countsByTutor[tutorId] ?: 0
    override fun findUpcomingByTutor(
        tutorId: TutorId,
        start: java.time.LocalDateTime,
        end: java.time.LocalDateTime,
        limit: Int,
    ): List<Event> = store.values.filter {
        it.status == Status.PENDING && !it.dateStart.isBefore(start) && !it.dateStart.isAfter(end)
    }.sortedBy { it.dateStart }.take(limit)
    override fun findByIdAndTutor(id: EventId, tutorId: TutorId): Event? = store[id]
    override fun existsForTutor(id: EventId, tutorId: TutorId): Boolean = store.containsKey(id)
    override fun findPendingReminders(start: java.time.LocalDateTime, end: java.time.LocalDateTime):
        List<EventReminderTarget> =
        store.values.filter {
            it.status == Status.PENDING && !it.dateStart.isBefore(start) && it.dateStart.isBefore(end)
        }.map { EventReminderTarget(it, "test@example.com", null) }

    fun allEvents(): Collection<Event> = store.values
}

/** Sem TTL real: sempre executa o loader — suficiente para testar orquestração. */
internal class FakePlacesCachePort : PlacesCachePort {
    override fun <T> getOrCompute(key: String, ttlSeconds: Long, loader: () -> T): T = loader()
}

internal class FakeGeocodingPort(private val result: GeoLocation?) : GeocodingPort {
    var calls = 0
    override fun geocode(zipCode: String): GeoLocation? {
        calls++
        return result
    }
}

internal class FakePlacesPort(
    private val nearby: List<PlaceSummary> = emptyList(),
    private val detail: PlaceDetails? = null,
    private val reviewList: List<PlaceReview> = emptyList(),
    private val photo: PlacePhoto = PlacePhoto(ByteArray(0), "image/jpeg")
) : PlacesPort {
    var nearbyCalls = 0
    var lastCategory: PlaceCategory? = null

    override fun searchNearby(latitude: Double, longitude: Double, radiusMeters: Int, category: PlaceCategory): List<PlaceSummary> {
        nearbyCalls++
        lastCategory = category
        return nearby
    }
    override fun details(placeId: String): PlaceDetails? = detail
    override fun reviews(placeId: String): List<PlaceReview> = reviewList
    override fun fetchPhoto(photoReference: String, maxWidth: Int): PlacePhoto = photo
}
