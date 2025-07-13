package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TutorAppServiceTest {

    private val tutorRepo = InMemoryTutorRepo()
    private val service = TutorAppService(tutorRepo)

    @Test
    fun `createTutor persists and returns id`() {
        val cmd = CreateTutorCommand("Ana", null, "ana@ex.com", "pwd", "1", null)
        val result = service.createTutor(cmd)
        assertNotNull(result.tutorId)
        assertEquals(1, tutorRepo.countAll())
    }

    @Test
    fun `updateTutor modifies existing tutor`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email="a@e.com", passwordHash="h", phoneNumber="1"))
        val cmd = UpdateTutorCommand(saved.id!!, firstName="Maria", lastName=null, phoneNumber=null, avatar=null)
        val detail = service.updateTutor(cmd)
        assertEquals("Maria", detail.firstName)
    }

    @Test
    fun `listTutors returns data`() {
        tutorRepo.save(Tutor(firstName="Ana", lastName=null, email="a@e.com", passwordHash="h", phoneNumber="1"))
        val page = service.listTutors(0, 10)
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `deleteTutor removes tutor`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email="a@e.com", passwordHash="h", phoneNumber="1"))
        service.deleteTutor(DeleteTutorCommand(saved.id!!))
        assertEquals(0, tutorRepo.countAll())
    }

    @Test
    fun `getTutor returns detail`() {
        val saved = tutorRepo.save(Tutor(firstName="Ana", lastName=null, email="a@e.com", passwordHash="h", phoneNumber="1"))
        val detail = service.getTutor(saved.id!!)
        assertEquals(saved.id, detail.id)
    }
}
