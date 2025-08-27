package dev.vilquer.petcarescheduler.infra.adapter.output.reset.jpa

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PasswordResetTokenRepository :
    JpaRepository<PasswordResetTokenJpa, UUID> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): PasswordResetTokenJpa?
}
