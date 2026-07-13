package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PetJpaRepository: JpaRepository<PetJpa, Long> {
    fun findAllByTutorIdOrderByNameAscIdAsc(tutorId: Long, pageable: Pageable): Page<PetJpa>
    fun findAllByTutorIdOrderByNameAscIdAsc(tutorId: Long): List<PetJpa>
    fun findByIdAndTutorId(id: Long, tutorId: Long): PetJpa?
    fun existsByIdAndTutorId(id: Long, tutorId: Long): Boolean
    fun countByTutorId(tutorId: Long): Long
    fun findAllByHouseholdIdOrderByNameAscIdAsc(householdId: UUID, pageable: Pageable): Page<PetJpa>
    fun findByIdAndHouseholdId(id: Long, householdId: UUID): PetJpa?
    fun existsByIdAndHouseholdId(id: Long, householdId: UUID): Boolean
    fun countByHouseholdId(householdId: UUID): Long

}
