package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HouseholdContextUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HouseholdManagementUseCase
import dev.vilquer.petcarescheduler.usecase.result.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

class HouseholdAppService(
    private val households: HouseholdRepositoryPort,
    private val members: HouseholdMemberRepositoryPort,
    private val invitations: HouseholdInvitationRepositoryPort,
    private val activities: HouseholdActivityRepositoryPort,
    private val handoffs: HouseholdHandoffRepositoryPort,
    private val tutors: TutorRepositoryPort,
    private val notifier: HouseholdInvitationNotifierPort,
    private val transaction: TransactionPort,
    private val clock: ClockPort,
) : HouseholdContextUseCase, HouseholdManagementUseCase {

    override fun resolve(actorTutorId: TutorId, requestedHouseholdId: HouseholdId?): HouseholdAccess {
        val tutor = tutors.findById(actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        val householdId = requestedHouseholdId ?: tutor.defaultHouseholdId
            ?: throw NotFoundException("Família padrão não encontrada")
        val member = members.findAccess(actorTutorId, householdId)
            ?: throw NotFoundException("Família não encontrada")
        return HouseholdAccess(householdId, actorTutorId, member.role)
    }

    override fun list(actorTutorId: TutorId): List<HouseholdSummaryResult> {
        val default = tutors.findById(actorTutorId)?.defaultHouseholdId
        return households.listForTutor(actorTutorId).map { (household, role) ->
            household.toSummary(role, default == household.id, members.count(household.id))
        }
    }

    override fun setDefault(actorTutorId: TutorId, householdId: HouseholdId) {
        members.findAccess(actorTutorId, householdId) ?: throw NotFoundException("Família não encontrada")
        val tutor = tutors.findById(actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        tutors.save(tutor.copy(defaultHouseholdId = householdId))
    }

    override fun provisionFor(tutorId: TutorId, firstName: String): HouseholdId = transaction.execute {
        val tutor = tutors.findById(tutorId) ?: throw NotFoundException("Tutor não encontrado")
        tutor.defaultHouseholdId?.let { return@execute it }
        val now = clock.now().toInstant()
        val household = households.save(
            Household(name = "Família de ${firstName.trim()}".take(100), createdByTutorId = tutorId, createdAt = now, updatedAt = now),
        )
        members.save(HouseholdMember(householdId = household.id, tutorId = tutorId, role = HouseholdRole.OWNER, joinedAt = now))
        tutors.save(tutor.copy(defaultHouseholdId = household.id))
        household.id
    }

    override fun overview(access: HouseholdAccess): HouseholdOverviewResult {
        requirePermission(access, HouseholdPermission.VIEW)
        val household = households.findById(access.householdId) ?: throw NotFoundException("Família não encontrada")
        val memberItems = members.listDetails(access.householdId)
        val pending = if (access.can(HouseholdPermission.MANAGE_MEMBERS)) {
            invitations.listActive(access.householdId, clock.now().toInstant()).map { it.toResult() }
        } else emptyList()
        return HouseholdOverviewResult(
            household.toSummary(
                access.role,
                tutors.findById(access.actorTutorId)?.defaultHouseholdId == access.householdId,
                memberItems.size.toLong(),
            ),
            memberItems.map { it.toResult() },
            pending,
            activities.listRecent(access.householdId, ACTIVITY_LIMIT).map { it.toResult() },
            handoffs.listRecent(access.householdId, HANDOFF_LIMIT).map { it.toResult() },
        )
    }

    override fun invite(command: InviteHouseholdMemberCommand, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        require(command.role != HouseholdRole.OWNER) { "household_invitation_owner_not_allowed" }
        val email = Email.of(command.email.trim()).getOrElse { throw IllegalArgumentException("household_invitation_email_invalid") }
            .value.lowercase()
        val actor = tutors.findById(access.actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        require(actor.email.value.lowercase() != email) { "household_invitation_self_not_allowed" }
        val existingTutor = Email.of(email).getOrNull()?.let(tutors::findByEmail)
        if (existingTutor?.id?.let { members.findAccess(it, access.householdId) } != null) {
            throw ConflictException("Esta pessoa já faz parte da família")
        }
        val household = households.findById(access.householdId) ?: throw NotFoundException("Família não encontrada")
        val rawToken = token()
        val now = clock.now().toInstant()
        val invitation = transaction.execute {
            val activeKey = "${access.householdId.value}:$email"
            invitations.findActiveByKeyForUpdate(activeKey)?.let { current ->
                invitations.save(current.revoke(now))
            }
            invitations.save(
                HouseholdInvitation(
                    householdId = access.householdId,
                    email = email,
                    role = command.role,
                    tokenHash = hash(rawToken),
                    activeKey = activeKey,
                    invitedByTutorId = access.actorTutorId,
                    expiresAt = now.plus(INVITATION_TTL),
                    createdAt = now,
                ),
            )
        }
        // A persistência acontece antes do envio para que o token já esteja válido
        // quando o destinatário abrir o e-mail. Falhas não são ocultadas: o cliente
        // pode informar o proprietário e uma nova tentativa revoga este convite.
        notifier.sendInvitation(email, household.name, actor.firstName, rawToken, invitation.expiresAt)
    }

    override fun accept(command: AcceptHouseholdInvitationCommand, actorTutorId: TutorId): HouseholdId = transaction.execute {
        require(command.token.length in 32..128) { "household_invitation_token_invalid" }
        val invitation = invitations.findActiveByHashForUpdate(hash(command.token))
            ?: throw NotFoundException("Convite inválido ou já utilizado")
        val now = clock.now().toInstant()
        if (invitation.activeKey == null || invitation.acceptedAt != null || invitation.revokedAt != null || !invitation.expiresAt.isAfter(now)) {
            throw ConflictException("Este convite expirou ou já foi utilizado")
        }
        val tutor = tutors.findById(actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        if (!MessageDigest.isEqual(tutor.email.value.lowercase().toByteArray(), invitation.email.toByteArray())) {
            throw ForbiddenException("Este convite foi enviado para outro e-mail")
        }
        val existing = members.findAccessForUpdate(actorTutorId, invitation.householdId)
        if (existing == null) {
            members.save(
                HouseholdMember(
                    householdId = invitation.householdId,
                    tutorId = actorTutorId,
                    role = invitation.role,
                    joinedAt = now,
                ),
            )
        }
        invitations.save(invitation.accept(now))
        tutors.save(tutor.copy(defaultHouseholdId = invitation.householdId))
        activities.save(
            HouseholdActivity(
                householdId = invitation.householdId,
                type = HouseholdActivityType.MEMBER_JOINED,
                actorTutorId = actorTutorId,
                targetTutorId = actorTutorId,
                summary = "${tutor.firstName} entrou na família",
                happenedAt = now,
            ),
        )
        invitation.householdId
    }

    override fun revokeInvitation(id: HouseholdInvitationId, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        transaction.execute {
            val invitation = invitations.findByIdForUpdate(id, access.householdId)
                ?: throw NotFoundException("Convite não encontrado")
            if (invitation.activeKey != null) invitations.save(invitation.revoke(clock.now().toInstant()))
        }
    }

    override fun changeRole(command: ChangeHouseholdMemberRoleCommand, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        transaction.execute {
            val member = members.findByIdForUpdate(command.memberId, access.householdId)
                ?: throw NotFoundException("Membro não encontrado")
            requireVersion(member.version, command.expectedVersion)
            if (member.role == HouseholdRole.OWNER && command.role != HouseholdRole.OWNER && members.countOwners(access.householdId) <= 1) {
                throw ConflictException("A família precisa manter ao menos um proprietário")
            }
            if (member.role == command.role) return@execute
            members.save(member.copy(role = command.role))
            activities.save(
                HouseholdActivity(
                    householdId = access.householdId,
                    type = HouseholdActivityType.MEMBER_ROLE_CHANGED,
                    actorTutorId = access.actorTutorId,
                    targetTutorId = member.tutorId,
                    summary = "Permissão de um membro foi atualizada",
                    happenedAt = clock.now().toInstant(),
                ),
            )
        }
    }

    override fun removeMember(memberId: HouseholdMemberId, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        transaction.execute {
            val member = members.findByIdForUpdate(memberId, access.householdId)
                ?: throw NotFoundException("Membro não encontrado")
            if (member.role == HouseholdRole.OWNER && members.countOwners(access.householdId) <= 1) {
                throw ConflictException("A família precisa manter ao menos um proprietário")
            }
            members.delete(member.id)
            val target = tutors.findById(member.tutorId)
            if (target?.defaultHouseholdId == access.householdId) {
                val fallback = households.listForTutor(member.tutorId).firstOrNull()?.first?.id
                if (fallback != null) tutors.save(target.copy(defaultHouseholdId = fallback))
            }
            activities.save(
                HouseholdActivity(
                    householdId = access.householdId,
                    type = HouseholdActivityType.MEMBER_REMOVED,
                    actorTutorId = access.actorTutorId,
                    targetTutorId = member.tutorId,
                    summary = "Um membro saiu da família",
                    happenedAt = clock.now().toInstant(),
                ),
            )
        }
    }

    override fun createHandoff(command: CreateHouseholdHandoffCommand, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.COMPLETE_CARE)
        command.toTutorId?.let {
            members.findAccess(it, access.householdId) ?: throw NotFoundException("Destinatário não encontrado")
        }
        val now = clock.now().toInstant()
        val note = command.note.trim()
        transaction.execute {
            val handoff = handoffs.save(
                HouseholdHandoff(
                    householdId = access.householdId,
                    fromTutorId = access.actorTutorId,
                    toTutorId = command.toTutorId,
                    note = note,
                    createdAt = now,
                ),
            )
            activities.save(
                HouseholdActivity(
                    householdId = access.householdId,
                    type = HouseholdActivityType.HANDOFF,
                    actorTutorId = access.actorTutorId,
                    targetTutorId = handoff.toTutorId,
                    summary = "Nova passagem de turno registrada",
                    happenedAt = now,
                ),
            )
        }
    }

    override fun rename(command: RenameHouseholdCommand, access: HouseholdAccess): HouseholdSummaryResult {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        require(command.householdId == access.householdId) { "household_context_mismatch" }
        return transaction.execute {
            val current = households.findByIdForUpdate(access.householdId) ?: throw NotFoundException("Família não encontrada")
            requireVersion(current.version, command.expectedVersion)
            households.save(current.copy(name = command.name.trim(), updatedAt = clock.now().toInstant()))
                .toSummary(access.role, true, members.count(access.householdId))
        }
    }

    private fun requirePermission(access: HouseholdAccess, permission: HouseholdPermission) {
        if (!access.can(permission)) throw ForbiddenException("Seu papel nesta família não permite esta ação")
    }

    private fun requireVersion(actual: Long?, expected: Long) {
        if (actual != expected) throw ConflictException("Os dados foram alterados. Atualize e tente novamente")
    }

    private fun Household.toSummary(role: HouseholdRole, default: Boolean, count: Long) =
        HouseholdSummaryResult(id.value, version, name, role, default, count)
    private fun HouseholdMemberDetails.toResult() = HouseholdMemberResult(
        member.id.value, member.version, member.tutorId.value, firstName, lastName, email,
        avatarAssetId, member.role, member.joinedAt,
    )
    private fun HouseholdInvitation.toResult() = HouseholdInvitationResult(id.value, email, role, expiresAt, createdAt)
    private fun HouseholdActivityDetails.toResult() = HouseholdActivityResult(
        activity.id, activity.type, actorName, targetName, petName, activity.summary, activity.happenedAt,
    )
    private fun HouseholdHandoffDetails.toResult() = HouseholdHandoffResult(
        handoff.id, fromName, toName, handoff.note, handoff.createdAt,
    )

    private fun token(): String = ByteArray(32).also(SecureRandom()::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }
    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private val INVITATION_TTL: Duration = Duration.ofDays(7)
        private const val ACTIVITY_LIMIT = 50
        private const val HANDOFF_LIMIT = 20
    }
}
