package dev.vilquer.petcarescheduler.adapter.output.persistence.jpa.entity


import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import jakarta.persistence.*

@Entity
@Table(name = "tutor")
data class TutorJpa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var firstName: String,
    var lastName: String?,
    var email: String,
    var passwordHash: String,
    var phoneNumber: String,
    var avatar: String? = null,
    @OneToMany(mappedBy = "tutor", cascade = [CascadeType.ALL], orphanRemoval = true)
    var pets: List<PetJpa> = mutableListOf()
){
    fun toDomain(): Tutor {
        val tutorId = id ?: throw IllegalStateException("Tutor ID cannot be null when mapping to domain object")
        return Tutor(TutorId(tutorId), firstName, lastName, email, passwordHash, phoneNumber, avatar, pets.map{p -> p.toDomain()})
    }
}
fun Tutor.toJpa(): TutorJpa = TutorJpa(id?.value, firstName, lastName, email, passwordHash, phoneNumber, avatar, pets.map { p -> p.toJpa() })