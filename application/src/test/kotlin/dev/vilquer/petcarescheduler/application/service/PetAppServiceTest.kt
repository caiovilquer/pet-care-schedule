package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PetAppServiceTest {

    private val petRepo = InMemoryPetRepo()
    private val tutorRepo = InMemoryTutorRepo()
    private val service = PetAppService(petRepo, tutorRepo)

    @Test
    fun `createPet should throw NotFoundException when tutor is missing`() {
        val cmd = CreatePetCommand("Rex", "Dog", null, LocalDate.now(), photoUrl = null, tutorId = TutorId(1))
        assertThrows(NotFoundException::class.java) { service.execute(cmd) }
    }

    @Test
    fun `getPet throws NotFoundException when pet does not exist`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        assertThrows(NotFoundException::class.java) { service.get(PetId(999), tutor.id!!) }
    }

    @Test
    fun `createPet saves pet and returns id`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val cmd = CreatePetCommand(
            "Rex",
            "Dog",
            "Labrador",
            LocalDate.now(),
            photoUrl = "https://example.com/pets/rex.png",
            tutorId = tutor.id!!
        )
        val result = service.execute(cmd)
        assertNotNull(result.petId)
        val saved = petRepo.findById(result.petId)!!
        assertEquals("Rex", saved.name)
        assertEquals("https://example.com/pets/rex.png", saved.photoUrl)
    }

    @Test
    fun `updatePet modifies existing pet`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race="mix", birthdate=LocalDate.now(), tutorId=tutor.id!!))
        val cmd = UpdatePetCommand(saved.id!!, name="Bidu", race=null, birthdate=null)
        val detail = service.execute(cmd, tutor.id!!)
        assertEquals("Bidu", detail.name)
    }

    @Test
    fun `listPets returns paginated result`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        petRepo.save(Pet(name="P1", specie="Dog", race=null, birthdate=LocalDate.now(), photoUrl = null, tutorId=tutor.id!!))
        val page = service.list(tutor.id!!, 0, 10)
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `deletePet removes pet`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race=null, birthdate=LocalDate.now(), tutorId=tutor.id!!))
        service.execute(DeletePetCommand(saved.id!!), tutor.id!!)
        assertNull(petRepo.findById(saved.id!!))
    }

    @Test
    fun `getPet returns detail`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race=null, birthdate=LocalDate.now(), tutorId=tutor.id!!))
        val detail = service.get(saved.id!!, tutor.id!!)
        assertEquals(saved.id, detail.id)
    }
}
