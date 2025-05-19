package dev.vilquer.petcarescheduler.adapter.output.persistence.jpa.entity


import com.fasterxml.jackson.annotation.JsonManagedReference
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import jakarta.persistence.*

@Entity
@Table(name = "pet")
data class PetJpa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
    var specie: String = "",
    var race: String?,
    var birthdate: String?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    var tutor: TutorJpa,
    @OneToMany(mappedBy = "pet", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("dateTime")
    @JsonManagedReference
    var event: List<EventJpa> = mutableListOf()
) {
    fun toDomain():Pet{
        val petId = id ?: throw IllegalStateException("Pet ID cannot be null when mapping to domain object")
        return Pet(PetId(petId), name, specie, race, birthdate,tutor.toDomain() ,event.map{ e -> e.toDomain()})
    }
}

fun Pet.toJpa(): PetJpa = PetJpa(id!!.value, name, specie, race, birthdate, tutor.toJpa(),event.map { e -> e.toJpa() })

