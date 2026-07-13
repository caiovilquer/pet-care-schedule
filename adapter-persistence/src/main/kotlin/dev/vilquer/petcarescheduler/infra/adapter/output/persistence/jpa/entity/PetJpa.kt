package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "pet")
class PetJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Version
    var version: Long? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var species: String

    var breed: String? = null

    @Column(name = "birthdate")
    var birthdate: LocalDate? = null

    @Column(name = "photo_url")
    var photoUrl: String? = null

    @Column(name = "photo_asset_id")
    var photoAssetId: UUID? = null

    @Column(name = "tutor_id", nullable = false)
    var tutorId: Long? = null

    @Column(name = "household_id", nullable = false)
    var householdId: UUID? = null

    override fun equals(other: Any?): Boolean =
        this === other || (other is PetJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
