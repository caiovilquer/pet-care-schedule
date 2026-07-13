package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.CareEscalationOutboxJpa
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CareEscalationOutboxJpaRepository : JpaRepository<CareEscalationOutboxJpa, Long> {
    fun existsByOccurrenceId(occurrenceId: UUID): Boolean
    fun findAllBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(maxAttempts: Int, pageable: Pageable): List<CareEscalationOutboxJpa>
}
