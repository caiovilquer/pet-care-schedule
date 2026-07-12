package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.ReminderOutboxJpa
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReminderOutboxJpaRepository : JpaRepository<ReminderOutboxJpa, Long> {
    fun existsByEventId(eventId: Long): Boolean
    fun deleteByEventId(eventId: Long)

    @Query(
        """
        select o from ReminderOutboxJpa o
         where o.sentAt is null and o.attempts < :maxAttempts
         order by o.createdAt asc
        """
    )
    fun findPendingDelivery(@Param("maxAttempts") maxAttempts: Int, pageable: Pageable): List<ReminderOutboxJpa>
}
