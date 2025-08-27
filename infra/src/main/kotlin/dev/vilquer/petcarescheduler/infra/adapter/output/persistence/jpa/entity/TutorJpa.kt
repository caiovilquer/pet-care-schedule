package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity


import jakarta.persistence.*
import java.time.Instant

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
    lateinit var email: String

    @Column(nullable = false)
    lateinit var passwordHash: String

    @Column(nullable = true)
    var phoneNumber: String? = null

    var avatar: String? = null

    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "tutor_id")
    val pets: MutableList<PetJpa> = mutableListOf()

    @Column(name = "password_changed_at")
    var passwordChangedAt: Instant? = null

    override fun equals(other: Any?): Boolean =
        this === other || (other is TutorJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
