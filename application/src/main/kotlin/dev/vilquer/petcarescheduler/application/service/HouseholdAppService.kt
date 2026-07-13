package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
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
        val normalizedEmail = Email.of(command.email.trim())
            .getOrElse { throw IllegalArgumentException("household_invitation_email_invalid") }
        val email = normalizedEmail.value.lowercase()
        val actor = tutors.findById(access.actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        require(actor.email.value.lowercase() != email) { "household_invitation_self_not_allowed" }
        val household = households.findById(access.householdId) ?: throw NotFoundException("Família não encontrada")
        val rawToken = token()
        val now = clock.now().toInstant()
        val invitation = transaction.execute {
            households.findByIdForUpdate(access.householdId) ?: throw NotFoundException("Família não encontrada")
            requireCurrentPermissionForUpdate(access, HouseholdPermission.MANAGE_MEMBERS)
            val currentTutor = tutors.findByEmail(normalizedEmail)
            if (currentTutor?.id?.let { members.findAccessForUpdate(it, access.householdId) } != null) {
                throw ConflictException("Esta pessoa já faz parte da família")
            }
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
        notifier.sendInvitation(email, household.name, actor.firstName, invitation.role, rawToken, invitation.expiresAt)
    }

    override fun invitationPreview(
        command: AcceptHouseholdInvitationCommand,
        actorTutorId: TutorId,
    ): HouseholdInvitationPreviewResult {
        require(command.token.length in 32..128) { "household_invitation_token_invalid" }
        val invitation = invitations.findActiveByHash(hash(command.token))
            ?: throw NotFoundException("Convite inválido ou já utilizado")
        validateActiveInvitation(invitation)
        val tutor = tutors.findById(actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        requireInvitationEmail(invitation, tutor)
        if (invitation.role == HouseholdRole.OWNER) requireActiveOwnerInviter(invitation, lock = false)
        val household = households.findById(invitation.householdId) ?: throw NotFoundException("Família não encontrada")
        val inviter = tutors.findById(invitation.invitedByTutorId) ?: throw NotFoundException("Responsável pelo convite não encontrado")
        return HouseholdInvitationPreviewResult(household.name, inviter.firstName, invitation.role, invitation.expiresAt)
    }

    override fun accept(command: AcceptHouseholdInvitationCommand, actorTutorId: TutorId): HouseholdId = transaction.execute {
        require(command.token.length in 32..128) { "household_invitation_token_invalid" }
        val tokenHash = hash(command.token)
        val candidate = invitations.findActiveByHash(tokenHash)
            ?: throw NotFoundException("Convite inválido ou já utilizado")
        households.findByIdForUpdate(candidate.householdId) ?: throw NotFoundException("Família não encontrada")
        val invitation = invitations.findActiveByHashForUpdate(tokenHash)
            ?: throw NotFoundException("Convite inválido ou já utilizado")
        validateActiveInvitation(invitation)
        val now = clock.now().toInstant()
        val tutor = tutors.findById(actorTutorId) ?: throw NotFoundException("Tutor não encontrado")
        requireInvitationEmail(invitation, tutor)
        if (invitation.role == HouseholdRole.OWNER) requireActiveOwnerInviter(invitation, lock = true)
        val existing = members.findAccessForUpdate(actorTutorId, invitation.householdId)
        if (existing != null) {
            throw ConflictException("Esta pessoa já faz parte da família")
        }
        members.save(
            HouseholdMember(
                householdId = invitation.householdId,
                tutorId = actorTutorId,
                role = invitation.role,
                joinedAt = now,
            ),
        )
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
            households.findByIdForUpdate(access.householdId) ?: throw NotFoundException("Família não encontrada")
            requireCurrentPermissionForUpdate(access, HouseholdPermission.MANAGE_MEMBERS)
            val invitation = invitations.findByIdForUpdate(id, access.householdId)
                ?: throw NotFoundException("Convite não encontrado")
            if (invitation.activeKey != null) invitations.save(invitation.revoke(clock.now().toInstant()))
        }
    }

    override fun changeRole(command: ChangeHouseholdMemberRoleCommand, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_MEMBERS)
        transaction.execute {
            households.findByIdForUpdate(access.householdId) ?: throw NotFoundException("Família não encontrada")
            requireCurrentPermissionForUpdate(access, HouseholdPermission.MANAGE_MEMBERS)
            val member = members.findByIdForUpdate(command.memberId, access.householdId)
                ?: throw NotFoundException("Membro não encontrado")
            requireVersion(member.version, command.expectedVersion)
            if (member.role == HouseholdRole.OWNER && command.role != HouseholdRole.OWNER && members.countOwners(access.householdId) <= 1) {
                throw ConflictException("A família precisa manter ao menos um proprietário")
            }
            if (member.role == command.role) return@execute
            if (member.role == HouseholdRole.OWNER && command.role != HouseholdRole.OWNER) {
                revokePendingOwnerInvitations(member.tutorId, access.householdId)
            }
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
            households.findByIdForUpdate(access.householdId) ?: throw NotFoundException("Família não encontrada")
            requireCurrentPermissionForUpdate(access, HouseholdPermission.MANAGE_MEMBERS)
            val member = members.findByIdForUpdate(memberId, access.householdId)
                ?: throw NotFoundException("Membro não encontrado")
            if (member.role == HouseholdRole.OWNER && members.countOwners(access.householdId) <= 1) {
                throw ConflictException("A família precisa manter ao menos um proprietário")
            }
            revokePendingOwnerInvitations(member.tutorId, access.householdId)
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
            val actor = requireCurrentPermissionForUpdate(access, HouseholdPermission.MANAGE_MEMBERS)
            requireVersion(current.version, command.expectedVersion)
            households.save(current.copy(name = command.name.trim(), updatedAt = clock.now().toInstant()))
                .toSummary(actor.role, true, members.count(access.householdId))
        }
    }

    private fun requirePermission(access: HouseholdAccess, permission: HouseholdPermission) {
        if (!access.can(permission)) throw ForbiddenException("Seu papel nesta família não permite esta ação")
    }

    private fun requireCurrentPermissionForUpdate(
        access: HouseholdAccess,
        permission: HouseholdPermission,
    ): HouseholdMember {
        val current = members.findAccessForUpdate(access.actorTutorId, access.householdId)
        if (current?.role?.allows(permission) != true) {
            throw ForbiddenException("Seu papel nesta família não permite esta ação")
        }
        return current
    }

    private fun validateActiveInvitation(invitation: HouseholdInvitation) {
        val now = clock.now().toInstant()
        if (invitation.activeKey == null || invitation.acceptedAt != null || invitation.revokedAt != null || !invitation.expiresAt.isAfter(now)) {
            throw ConflictException("Este convite expirou ou já foi utilizado")
        }
    }

    private fun requireInvitationEmail(invitation: HouseholdInvitation, tutor: Tutor) {
        if (!MessageDigest.isEqual(tutor.email.value.lowercase().toByteArray(), invitation.email.toByteArray())) {
            throw ForbiddenException("Este convite foi enviado para outro e-mail")
        }
    }

    private fun requireActiveOwnerInviter(invitation: HouseholdInvitation, lock: Boolean) {
        val inviter = if (lock) {
            members.findAccessForUpdate(invitation.invitedByTutorId, invitation.householdId)
        } else {
            members.findAccess(invitation.invitedByTutorId, invitation.householdId)
        }
        if (inviter?.role != HouseholdRole.OWNER) {
            throw ConflictException("Este convite de proprietário perdeu a validade")
        }
    }

    private fun revokePendingOwnerInvitations(inviterTutorId: TutorId, householdId: HouseholdId) {
        val now = clock.now().toInstant()
        invitations.listActiveByInviterAndRoleForUpdate(householdId, inviterTutorId, HouseholdRole.OWNER)
            .forEach { invitations.save(it.revoke(now)) }
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
