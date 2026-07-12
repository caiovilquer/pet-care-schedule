package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.CareOccurrenceActionJpa
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CareOccurrenceActionJpaRepository : JpaRepository<CareOccurrenceActionJpa, UUID> {
    fun findByRequestId(requestId: UUID): CareOccurrenceActionJpa?
}
