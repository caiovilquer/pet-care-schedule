package dev.vilquer.petcarescheduler.core.domain.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PetTest {

    private fun pet(name: String = "Rex", species: String = "Dog") = Pet(
        name = name,
        species = species,
        breed = null,
        birthdate = LocalDate.of(2020, 1, 1),
        tutorId = TutorId(1)
    )

    @Test
    fun `accepts non-blank name and species`() {
        val saved = pet(name = "Rex", species = "Dog")
        assertEquals("Rex", saved.name)
        assertEquals("Dog", saved.species)
    }

    @Test
    fun `rejects a blank name`() {
        assertThrows(IllegalArgumentException::class.java) { pet(name = "") }
    }

    @Test
    fun `rejects a blank species`() {
        assertThrows(IllegalArgumentException::class.java) { pet(species = "  ") }
    }
}
