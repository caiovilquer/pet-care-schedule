package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.*
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

data class HouseholdAccessJpaRow(val household: HouseholdJpa, val role: HouseholdRole)
data class HouseholdMemberJpaRow(val member: HouseholdMemberJpa, val tutor: TutorJpa)
data class HouseholdActivityJpaRow(
    val activity: HouseholdActivityJpa,
    val actorFirst: String?, val actorLast: String?,
    val targetFirst: String?, val targetLast: String?,
    val petName: String?,
)
data class HouseholdHandoffJpaRow(
    val handoff: HouseholdHandoffJpa,
    val fromFirst: String, val fromLast: String?,
    val toFirst: String?, val toLast: String?,
)

interface HouseholdJpaRepository : JpaRepository<HouseholdJpa, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HouseholdJpa h where h.id = :id")
    fun findForUpdate(@Param("id") id: UUID): HouseholdJpa?

    @Query("""
        select new dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdAccessJpaRow(h, m.role)
        from HouseholdMemberJpa m join HouseholdJpa h on h.id = m.householdId
        where m.tutorId = :tutorId order by m.joinedAt asc, h.id asc
    """)
    fun listForTutor(@Param("tutorId") tutorId: Long): List<HouseholdAccessJpaRow>
}

interface HouseholdMemberJpaRepository : JpaRepository<HouseholdMemberJpa, UUID> {
    fun findByTutorIdAndHouseholdId(tutorId: Long, householdId: UUID): HouseholdMemberJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from HouseholdMemberJpa m where m.tutorId = :tutorId and m.householdId = :householdId")
    fun findAccessForUpdate(@Param("tutorId") tutorId: Long, @Param("householdId") householdId: UUID): HouseholdMemberJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from HouseholdMemberJpa m where m.id = :id and m.householdId = :householdId")
    fun findOwnedForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): HouseholdMemberJpa?

    @Query("""
        select new dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdMemberJpaRow(m, t)
        from HouseholdMemberJpa m join TutorJpa t on t.id = m.tutorId
        where m.householdId = :householdId
        order by case when m.role = dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole.OWNER then 0 when m.role = dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole.CAREGIVER then 1 else 2 end,
                 t.firstName asc, t.id asc
    """)
    fun listDetails(@Param("householdId") householdId: UUID): List<HouseholdMemberJpaRow>
    fun countByHouseholdId(householdId: UUID): Long
    fun countByHouseholdIdAndRole(householdId: UUID, role: HouseholdRole): Long
}

interface HouseholdInvitationJpaRepository : JpaRepository<HouseholdInvitationJpa, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from HouseholdInvitationJpa i where i.tokenHash = :hash and i.activeKey is not null")
    fun findActiveByHashForUpdate(@Param("hash") hash: String): HouseholdInvitationJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from HouseholdInvitationJpa i where i.activeKey = :activeKey")
    fun findActiveByKeyForUpdate(@Param("activeKey") activeKey: String): HouseholdInvitationJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from HouseholdInvitationJpa i where i.id = :id and i.householdId = :householdId")
    fun findOwnedForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): HouseholdInvitationJpa?

    @Query("select i from HouseholdInvitationJpa i where i.householdId = :householdId and i.activeKey is not null and i.expiresAt > :now order by i.createdAt desc")
    fun listActive(@Param("householdId") householdId: UUID, @Param("now") now: Instant): List<HouseholdInvitationJpa>
}

interface HouseholdActivityJpaRepository : JpaRepository<HouseholdActivityJpa, UUID> {
    @Query("""
        select new dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdActivityJpaRow(
            a, actor.firstName, actor.lastName, target.firstName, target.lastName, p.name
        )
        from HouseholdActivityJpa a
        left join TutorJpa actor on actor.id = a.actorTutorId
        left join TutorJpa target on target.id = a.targetTutorId
        left join PetJpa p on p.id = a.petId
        where a.householdId = :householdId order by a.happenedAt desc, a.id desc
    """)
    fun listRecent(@Param("householdId") householdId: UUID, pageable: Pageable): List<HouseholdActivityJpaRow>
}

interface HouseholdHandoffJpaRepository : JpaRepository<HouseholdHandoffJpa, UUID> {
    @Query("""
        select new dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdHandoffJpaRow(
            h, sender.firstName, sender.lastName, recipient.firstName, recipient.lastName
        )
        from HouseholdHandoffJpa h
        join TutorJpa sender on sender.id = h.fromTutorId
        left join TutorJpa recipient on recipient.id = h.toTutorId
        where h.householdId = :householdId order by h.createdAt desc, h.id desc
    """)
    fun listRecent(@Param("householdId") householdId: UUID, pageable: Pageable): List<HouseholdHandoffJpaRow>
}
