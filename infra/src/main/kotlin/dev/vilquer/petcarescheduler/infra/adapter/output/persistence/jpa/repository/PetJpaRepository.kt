package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PetJpaRepository: JpaRepository<PetJpa, Long> {
    fun existsByName(name: String): Boolean
    fun findByName(name: String): PetJpa?
    fun findBySpecie(type: String): List<PetJpa>

}