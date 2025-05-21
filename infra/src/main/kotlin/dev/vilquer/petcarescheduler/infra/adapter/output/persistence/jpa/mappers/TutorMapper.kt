package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa

object TutorMapper {

    fun toDomain(jpa: TutorJpa): Tutor =
        Tutor(
            id            = jpa.id?.let(::TutorId),
            firstName     = jpa.firstName,
            lastName      = jpa.lastName,
            email         = jpa.email,
            passwordHash  = jpa.passwordHash,
            phoneNumber   = jpa.phoneNumber,
            avatar        = jpa.avatar,
            pets          = jpa.pets.map { PetMapper.toDomain(it) }
        )


    fun toJpa(domain: Tutor, existing: TutorJpa? = null): TutorJpa {
        val jpa = existing ?: TutorJpa()

        jpa.id           = domain.id?.value
        jpa.firstName    = domain.firstName
        jpa.lastName     = domain.lastName
        jpa.email        = domain.email
        jpa.passwordHash = domain.passwordHash
        jpa.phoneNumber  = domain.phoneNumber
        jpa.avatar       = domain.avatar

        val existingPetsById = jpa.pets.associateBy { it.id }

        val domainPetIds = domain.pets.mapNotNull { it.id?.value }.toSet()

        val removals = jpa.pets.filter { it.id !in domainPetIds }
        jpa.pets.removeAll(removals)


        domain.pets.forEach { pd ->
            val petJpa = pd.id?.value?.let { existingPetsById[it] }
                ?.let { existingPet -> PetMapper.toJpa(pd, existingPet) }
                ?: PetMapper.toJpa(pd, null)


            if (pd.id?.value == null || pd.id!!.value !in existingPetsById) {
                jpa.pets.add(petJpa)
            }
        }

        return jpa
    }
}
