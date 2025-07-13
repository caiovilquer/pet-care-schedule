import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.usecase.command.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CommandsTest {
    @Test
    fun `create pet command holds provided values`() {
        val cmd = CreatePetCommand(
            name = "Bidu",
            specie = "Dog",
            race = "Mixed",
            birthdate = LocalDate.of(2020, 5, 4),
            tutorId = TutorId(5L)
        )
        assertEquals("Bidu", cmd.name)
        assertEquals("Dog", cmd.specie)
        assertEquals("Mixed", cmd.race)
        assertEquals(LocalDate.of(2020, 5, 4), cmd.birthdate)
        assertEquals(TutorId(5L), cmd.tutorId)
    }

    @Test
    fun `register event command stores event information`() {
        val cmd = RegisterEventCommand(
            petId = PetId(9L),
            type = EventType.MEDICINE,
            description = "Worm medicine",
            dateStart = LocalDateTime.of(2025, 7, 12, 14, 0)
        )
        assertEquals(PetId(9L), cmd.petId)
        assertEquals(EventType.MEDICINE, cmd.type)
        assertEquals("Worm medicine", cmd.description)
        assertEquals(LocalDateTime.of(2025, 7, 12, 14, 0), cmd.dateStart)
    }
}
