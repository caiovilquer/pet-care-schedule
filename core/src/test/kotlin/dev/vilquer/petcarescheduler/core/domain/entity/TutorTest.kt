package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TutorTest {

    private fun tutor(firstName: String) = Tutor(
        firstName = firstName,
        lastName = null,
        email = Email.of("ana@example.com").getOrThrow(),
        passwordHash = "hash"
    )

    @Test
    fun `accepts a non-blank firstName`() {
        assertEquals("Ana", tutor("Ana").firstName)
    }

    @Test
    fun `rejects a blank firstName`() {
        assertThrows(IllegalArgumentException::class.java) { tutor("") }
    }

    @Test
    fun `rejects a whitespace-only firstName`() {
        assertThrows(IllegalArgumentException::class.java) { tutor("   ") }
    }
}
