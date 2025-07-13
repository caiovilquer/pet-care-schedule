import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
class ResultMappingTest {

    private fun Pet.toDetailResult(): PetDetailResult = PetDetailResult(
        id = id!!,
        name = name,
        specie = specie,
        race = race,
        birthdate = birthdate!!,
        events = events.map { e ->
            PetDetailResult.EventInfo(
                id = e.id!!,
                type = e.type,
                dateStart = e.dateStart
            )
        }
    )

    private fun Tutor.toDetailResult(): TutorDetailResult = TutorDetailResult(
        id = id!!,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber,
        avatar = avatar,
        pets = pets.map { p ->
            TutorDetailResult.PetInfo(p.id!!, p.name, p.specie)
        }
    )

    @Test
    fun `pet detail result maps fields from domain`() {
        val pet = Pet(
            id = PetId(1L),
            name = "Luna",
            specie = "Cat",
            race = "SRD",
            birthdate = LocalDate.of(2022, 1, 1),
            tutorId = TutorId(2L),
            events = listOf(
                Event(
                    id = EventId(3L),
                    type = EventType.VACCINE,
                    description = null,
                    dateStart = LocalDateTime.of(2025, 7, 12, 10, 0),
                    recurrence = null,
                    status = Status.PENDING,
                    petId = PetId(1L)
                )
            )
        )

        val result = pet.toDetailResult()
        assertEquals(PetId(1L), result.id)
        assertEquals("Luna", result.name)
        assertEquals(EventType.VACCINE, result.events.first().type)
    }

    @Test
    fun `tutor detail result maps pet summaries`() {
        val tutor = Tutor(
            id = TutorId(5L),
            firstName = "Ana",
            lastName = "Silva",
            email = "ana@ex.com",
            passwordHash = "hash",
            phoneNumber = "1111",
            avatar = null,
            pets = listOf(
                Pet(
                    id = PetId(9L),
                    name = "Bidu",
                    specie = "Dog",
                    race = null,
                    birthdate = LocalDate.of(2020, 5, 4),
                    tutorId = TutorId(5L)
                )
            )
        )

        val result = tutor.toDetailResult()
        assertEquals(TutorId(5L), result.id)
        assertEquals(1, result.pets.size)
        assertEquals(PetId(9L), result.pets.first().id)
        assertEquals("Bidu", result.pets.first().name)
    }
}
