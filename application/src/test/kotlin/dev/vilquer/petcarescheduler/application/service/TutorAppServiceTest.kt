package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TutorAppServiceTest {

    private val tutorRepo = InMemoryTutorRepo()
    private val service = TutorAppService(tutorRepo)

    @Test
    fun `createTutor persists and returns id`() {
        val cmd = CreateTutorCommand("Ana", null, Email.of("ana@ex.com").getOrThrow(), "pwd", PhoneNumber.of("+5511912345678").getOrNull(), null)
        val result = service.createTutor(cmd)
        assertNotNull(result.tutorId)
        assertEquals(1, tutorRepo.countAll())
    }
    @Test
    fun `updateTutor modifies existing tutor`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email=Email.of("a@e.com").getOrThrow(), passwordHash="h", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val cmd = UpdateTutorCommand(saved.id!!, firstName="Maria", lastName=null, phoneNumber=null, avatar=null)
        val detail = service.updateTutor(cmd)
        assertEquals("Maria", detail.firstName)
    }

    @Test
    fun `listTutors returns data`() {
        tutorRepo.save(Tutor(firstName="Ana", lastName=null, email=Email.of("a@e.com").getOrThrow(), passwordHash="h", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val page = service.listTutors(0, 10)
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `deleteTutor removes tutor`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email=Email.of("a@e.com").getOrThrow(), passwordHash="h", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        service.deleteTutor(DeleteTutorCommand(saved.id!!))
        assertEquals(0, tutorRepo.countAll())
    }

    @Test
    fun `getTutor returns detail`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email=Email.of("a@e.com").getOrThrow(), passwordHash="h", phoneNumber=PhoneNumber.of("+5511912345678").getOrNull()))
        val detail = service.getTutor(saved.id!!)
        assertEquals(saved.id, detail.id)
    }
}
