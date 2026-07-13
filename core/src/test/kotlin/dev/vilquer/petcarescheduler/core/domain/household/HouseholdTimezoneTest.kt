package dev.vilquer.petcarescheduler.core.domain.household

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HouseholdTimezoneTest {
    @Test
    fun `null keeps Sao Paulo compatibility fallback`() {
        assertEquals("America/Sao_Paulo", HouseholdTimezone.parse(null).id)
    }

    @Test
    fun `valid IANA timezone is preserved and invalid value is rejected`() {
        assertEquals("America/New_York", HouseholdTimezone.requireValid("America/New_York").id)
        assertThrows(IllegalArgumentException::class.java) { HouseholdTimezone.requireValid("UTC-3") }
    }
}
