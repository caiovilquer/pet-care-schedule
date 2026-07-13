package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HouseholdAppServiceTest {
    private val ownerId = TutorId(1)
    private val guestId = TutorId(2)
    private val householdId = TEST_HOUSEHOLD_ID
    private val access = HouseholdAccess(householdId, ownerId, HouseholdRole.OWNER)
    private val clock = FakeClock(ZonedDateTime.ofInstant(Instant.parse("2026-07-12T15:00:00Z"), ZoneId.of("America/Sao_Paulo")))
    private lateinit var tutors: InMemoryTutorRepo
    private lateinit var households: Households
    private lateinit var members: Members
    private lateinit var invitations: Invitations
    private lateinit var activities: Activities
    private lateinit var notifier: InvitationNotifier
    private lateinit var service: HouseholdAppService

    @BeforeEach
    fun setup() {
        tutors = InMemoryTutorRepo()
        tutors.save(tutor(ownerId, "Ana", "ana@example.com", householdId))
        tutors.save(tutor(guestId, "Bia", "bia@example.com", null))
        val now = clock.now().toInstant()
        households = Households(Household(householdId, 0, "Casa da Ana", ownerId, now, now))
        members = Members(HouseholdMember(householdId = householdId, tutorId = ownerId, role = HouseholdRole.OWNER, joinedAt = now, version = 0))
        invitations = Invitations()
        activities = Activities()
        notifier = InvitationNotifier()
        service = HouseholdAppService(
            households, members, invitations, activities, Handoffs(), tutors, notifier, FakeTransactionPort(), clock,
        )
    }

    @Test
    fun `invitation persists only a hash and token is bound to invited email`() {
        service.invite(InviteHouseholdMemberCommand("BIA@EXAMPLE.COM", HouseholdRole.CAREGIVER), access)

        val stored = invitations.items.single()
        assertEquals("bia@example.com", stored.email)
        assertEquals(64, stored.tokenHash.length)
        assertNotEquals(notifier.token, stored.tokenHash)
        assertFalse(stored.tokenHash.contains(notifier.token))

        assertThrows(ForbiddenException::class.java) {
            service.accept(AcceptHouseholdInvitationCommand(notifier.token), ownerId)
        }
        assertNotNull(invitations.items.single().activeKey, "uma tentativa por outro e-mail não consome o convite")
    }

    @Test
    fun `accepting once joins family changes default and records audit activity`() {
        service.invite(InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.VIEWER), access)
        val accepted = service.accept(AcceptHouseholdInvitationCommand(notifier.token), guestId)

        assertEquals(householdId, accepted)
        assertEquals(HouseholdRole.VIEWER, members.findAccess(guestId, householdId)?.role)
        assertEquals(householdId, tutors.findById(guestId)?.defaultHouseholdId)
        assertNull(invitations.items.single().activeKey)
        assertEquals(HouseholdActivityType.MEMBER_JOINED, activities.items.single().type)
        assertThrows(RuntimeException::class.java) {
            service.accept(AcceptHouseholdInvitationCommand(notifier.token), guestId)
        }
    }

    @Test
    fun `owner invitation creates shared ownership and allows the original owner to change role`() {
        service.invite(InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.OWNER), access)

        val preview = service.invitationPreview(AcceptHouseholdInvitationCommand(notifier.token), guestId)
        val accepted = service.accept(AcceptHouseholdInvitationCommand(notifier.token), guestId)

        assertEquals("Casa da Ana", preview.householdName)
        assertEquals("Ana", preview.inviterName)
        assertEquals(HouseholdRole.OWNER, preview.role)
        assertEquals(householdId, accepted)
        assertEquals(HouseholdRole.OWNER, invitations.items.single().role)
        assertEquals(HouseholdRole.OWNER, notifier.role)
        assertEquals(HouseholdRole.OWNER, members.findAccess(guestId, householdId)?.role)
        assertEquals(2L, members.countOwners(householdId))

        val originalOwner = members.findAccess(ownerId, householdId)!!
        service.changeRole(
            ChangeHouseholdMemberRoleCommand(originalOwner.id, originalOwner.version!!, HouseholdRole.CAREGIVER),
            access,
        )

        assertEquals(HouseholdRole.CAREGIVER, members.findAccess(ownerId, householdId)?.role)
        assertEquals(HouseholdRole.OWNER, members.findAccess(guestId, householdId)?.role)

        val remainingOwner = members.findAccess(guestId, householdId)!!
        assertThrows(ConflictException::class.java) {
            service.changeRole(
                ChangeHouseholdMemberRoleCommand(remainingOwner.id, remainingOwner.version!!, HouseholdRole.CAREGIVER),
                HouseholdAccess(householdId, guestId, HouseholdRole.OWNER),
            )
        }
    }

    @Test
    fun `owner invitation is revoked when its inviter is no longer an owner`() {
        members.save(
            HouseholdMember(
                householdId = householdId,
                tutorId = guestId,
                role = HouseholdRole.OWNER,
                joinedAt = clock.now().toInstant(),
            ),
        )
        val candidateId = TutorId(3)
        tutors.save(tutor(candidateId, "Clara", "clara@example.com", null))
        service.invite(InviteHouseholdMemberCommand("clara@example.com", HouseholdRole.OWNER), access)

        val inviter = members.findAccess(ownerId, householdId)!!
        service.changeRole(
            ChangeHouseholdMemberRoleCommand(inviter.id, inviter.version!!, HouseholdRole.CAREGIVER),
            HouseholdAccess(householdId, guestId, HouseholdRole.OWNER),
        )

        assertNull(invitations.items.single().activeKey)
        val demotedInviter = members.findAccess(ownerId, householdId)!!
        service.changeRole(
            ChangeHouseholdMemberRoleCommand(demotedInviter.id, demotedInviter.version!!, HouseholdRole.OWNER),
            HouseholdAccess(householdId, guestId, HouseholdRole.OWNER),
        )
        assertThrows(RuntimeException::class.java) {
            service.accept(AcceptHouseholdInvitationCommand(notifier.token), candidateId)
        }
        assertNull(members.findAccess(candidateId, householdId))
    }

    @Test
    fun `a person who became a member cannot receive or silently consume another invitation`() {
        service.invite(InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.OWNER), access)
        members.save(
            HouseholdMember(
                householdId = householdId,
                tutorId = guestId,
                role = HouseholdRole.CAREGIVER,
                joinedAt = clock.now().toInstant(),
            ),
        )

        assertThrows(ConflictException::class.java) {
            service.accept(AcceptHouseholdInvitationCommand(notifier.token), guestId)
        }
        assertNotNull(invitations.items.single().activeKey)

        invitations.items.clear()
        assertThrows(ConflictException::class.java) {
            service.invite(InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.OWNER), access)
        }
        assertTrue(invitations.items.isEmpty())
    }

    @Test
    fun `stale owner access cannot mutate members or create an invitation`() {
        val guest = members.save(
            HouseholdMember(
                householdId = householdId,
                tutorId = guestId,
                role = HouseholdRole.CAREGIVER,
                joinedAt = clock.now().toInstant(),
            ),
        )
        val owner = members.findAccess(ownerId, householdId)!!
        members.save(owner.copy(role = HouseholdRole.CAREGIVER))

        assertThrows(ForbiddenException::class.java) {
            service.invite(InviteHouseholdMemberCommand("clara@example.com", HouseholdRole.OWNER), access)
        }
        assertThrows(ForbiddenException::class.java) {
            service.changeRole(ChangeHouseholdMemberRoleCommand(guest.id, guest.version!!, HouseholdRole.OWNER), access)
        }
        assertThrows(ForbiddenException::class.java) {
            service.removeMember(guest.id, access)
        }
    }

    @Test
    fun `viewer cannot invite and last owner cannot be demoted`() {
        assertThrows(ForbiddenException::class.java) {
            service.invite(
                InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.VIEWER),
                HouseholdAccess(householdId, guestId, HouseholdRole.VIEWER),
            )
        }
        val owner = members.findAccess(ownerId, householdId)!!
        assertThrows(ConflictException::class.java) {
            service.changeRole(ChangeHouseholdMemberRoleCommand(owner.id, owner.version!!, HouseholdRole.CAREGIVER), access)
        }
    }

    @Test
    fun `mail failure is reported while invitation remains revocable and retry safe`() {
        notifier.fail = true

        assertThrows(IllegalStateException::class.java) {
            service.invite(InviteHouseholdMemberCommand("bia@example.com", HouseholdRole.CAREGIVER), access)
        }

        assertNotNull(invitations.items.single().activeKey)
    }

    private fun tutor(id: TutorId, name: String, email: String, default: HouseholdId?) = Tutor(
        id = id, firstName = name, lastName = null, email = Email.of(email).getOrThrow(), passwordHash = "hash",
        phoneNumber = null, defaultHouseholdId = default,
    )

    private class Households(initial: Household) : HouseholdRepositoryPort {
        private val map = linkedMapOf(initial.id to initial)
        override fun save(household: Household) = household.copy(version = (household.version ?: -1) + 1).also { map[it.id] = it }
        override fun findById(id: HouseholdId) = map[id]
        override fun findByIdForUpdate(id: HouseholdId) = map[id]
        override fun listForTutor(tutorId: TutorId) = map.values.map { it to HouseholdRole.OWNER }
    }
    private class Members(initial: HouseholdMember) : HouseholdMemberRepositoryPort {
        private val map = linkedMapOf((initial.tutorId to initial.householdId) to initial)
        override fun save(member: HouseholdMember) = member.copy(version = (member.version ?: -1) + 1).also { map[it.tutorId to it.householdId] = it }
        override fun findAccess(tutorId: TutorId, householdId: HouseholdId) = map[tutorId to householdId]
        override fun findAccessForUpdate(tutorId: TutorId, householdId: HouseholdId) = findAccess(tutorId, householdId)
        override fun findByIdForUpdate(id: HouseholdMemberId, householdId: HouseholdId) = map.values.firstOrNull { it.id == id && it.householdId == householdId }
        override fun listDetails(householdId: HouseholdId) = emptyList<HouseholdMemberDetails>()
        override fun count(householdId: HouseholdId) = map.values.count { it.householdId == householdId }.toLong()
        override fun countOwners(householdId: HouseholdId) = map.values.count { it.householdId == householdId && it.role == HouseholdRole.OWNER }.toLong()
        override fun delete(id: HouseholdMemberId) { map.entries.removeIf { it.value.id == id } }
    }
    private class Invitations : HouseholdInvitationRepositoryPort {
        val items = mutableListOf<HouseholdInvitation>()
        override fun save(invitation: HouseholdInvitation) = invitation.also { value -> items.removeIf { it.id == value.id }; items += value }
        override fun findActiveByHash(hash: String) = items.firstOrNull { it.tokenHash == hash && it.activeKey != null }
        override fun findActiveByHashForUpdate(hash: String) = items.firstOrNull { it.tokenHash == hash && it.activeKey != null }
        override fun findActiveByKeyForUpdate(activeKey: String) = items.firstOrNull { it.activeKey == activeKey }
        override fun findByIdForUpdate(id: HouseholdInvitationId, householdId: HouseholdId) = items.firstOrNull { it.id == id && it.householdId == householdId }
        override fun listActiveByInviterAndRoleForUpdate(householdId: HouseholdId, inviterTutorId: TutorId, role: HouseholdRole) =
            items.filter { it.householdId == householdId && it.invitedByTutorId == inviterTutorId && it.role == role && it.activeKey != null }
        override fun listActive(householdId: HouseholdId, now: Instant) = items.filter { it.householdId == householdId && it.activeKey != null && it.expiresAt > now }
    }
    private class Activities : HouseholdActivityRepositoryPort {
        val items = mutableListOf<HouseholdActivity>()
        override fun save(activity: HouseholdActivity) = activity.also(items::add)
        override fun listRecent(householdId: HouseholdId, limit: Int) = emptyList<HouseholdActivityDetails>()
    }
    private class Handoffs : HouseholdHandoffRepositoryPort {
        override fun save(handoff: HouseholdHandoff) = handoff
        override fun listRecent(householdId: HouseholdId, limit: Int) = emptyList<HouseholdHandoffDetails>()
    }
    private class InvitationNotifier : HouseholdInvitationNotifierPort {
        lateinit var token: String
        lateinit var role: HouseholdRole
        var fail = false
        override fun sendInvitation(
            email: String,
            householdName: String,
            inviterName: String,
            role: HouseholdRole,
            token: String,
            expiresAt: Instant,
        ) {
            this.role = role
            this.token = token
            if (fail) throw IllegalStateException("mail unavailable")
        }
    }
}
