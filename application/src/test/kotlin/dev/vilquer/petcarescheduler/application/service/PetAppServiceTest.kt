package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
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
    fun `createPet should throw when tutor is missing`() {
        val cmd = CreatePetCommand("Rex", "Dog", null, LocalDate.now(), TutorId(1))
        assertThrows(IllegalArgumentException::class.java) { service.createPet(cmd) }
    }

    @Test
    fun `createPet saves pet and returns id`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val cmd = CreatePetCommand("Rex", "Dog", "Labrador", LocalDate.now(), tutor.id!!)
        val result = service.createPet(cmd)
        assertNotNull(result.petId)
        val saved = petRepo.findById(result.petId)!!
        assertEquals("Rex", saved.name)
    }

    @Test
    fun `updatePet modifies existing pet`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race="mix", birthdate=LocalDate.now(), tutorId=tutor.id!!))
        val cmd = UpdatePetCommand(saved.id!!, name="Bidu", race=null, birthdate=null)
        val detail = service.updatePet(cmd)
        assertEquals("Bidu", detail.name)
    }

    @Test
    fun `listPets returns paginated result`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        petRepo.save(Pet(name="P1", specie="Dog", race=null, birthdate=LocalDate.now(), tutorId=tutor.id!!))
        val page = service.listPets(0, 10)
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `deletePet removes pet`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race=null, birthdate=LocalDate.now(), tutorId=tutor.id!!))
        service.deletePet(DeletePetCommand(saved.id!!))
        assertNull(petRepo.findById(saved.id!!))
    }

    @Test
    fun `getPet returns detail`() {
        val tutor = tutorRepo.save(Tutor(firstName="A", lastName=null, email=Email.of("e@x.com").getOrThrow(), passwordHash="p", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val saved = petRepo.save(Pet(name="Rex", specie="Dog", race=null, birthdate=LocalDate.now(), tutorId=tutor.id!!))
        val detail = service.getPet(saved.id!!)
        assertEquals(saved.id, detail.id)
    }
}
