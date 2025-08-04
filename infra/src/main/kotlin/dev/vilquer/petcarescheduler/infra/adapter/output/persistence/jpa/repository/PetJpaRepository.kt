package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PetJpaRepository: JpaRepository<PetJpa, Long> {
    fun existsByName(name: String): Boolean
    fun findByName(name: String): PetJpa?
    fun findBySpecie(type: String): List<PetJpa>

    fun findAllByTutorId(tutorId: Long, pageable: Pageable): Page<PetJpa>
    fun findByIdAndTutorId(id: Long, tutorId: Long): PetJpa?
    fun existsByIdAndTutorId(id: Long, tutorId: Long): Boolean
    fun countByTutorId(tutorId: Long): Long

}