package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity


import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import jakarta.persistence.*

@Entity
@Table(name = "tutor")
class TutorJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    lateinit var firstName: String

    var lastName: String? = null

    @Column(nullable = false, unique = true)
    var email: Email = Email.of("placeholder@example.com").getOrThrow()

    @Column(nullable = false)
    lateinit var passwordHash: String

    @Column(nullable = false)
    var phoneNumber: PhoneNumber? = null

    var avatar: String? = null

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "tutor_id")
    val pets: MutableList<PetJpa> = mutableListOf()

    override fun equals(other: Any?): Boolean =
        this === other || (other is TutorJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
