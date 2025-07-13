package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TutorJpaRepository: JpaRepository<TutorJpa,Long> {
    fun findByEmail(email: String): TutorJpa?
    fun findByPhoneNumber(phoneNumber: String): TutorJpa?
}