package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "pet")
class PetJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var specie: String

    var race: String? = null

    @Column(name = "birthdate")
    var birthdate: LocalDate? = null

    @Column(name = "tutor_id", nullable = false)
    var tutorId: Long = 0


    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "pet_id")
    var events: MutableList<EventJpa> = mutableListOf()

    override fun equals(other: Any?): Boolean =
        this === other || (other is PetJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
