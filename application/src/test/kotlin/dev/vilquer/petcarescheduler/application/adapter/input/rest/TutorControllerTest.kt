package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.application.service.TutorAppService
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.TutorCreatedResult
import dev.vilquer.petcarescheduler.usecase.result.TutorSummary
import dev.vilquer.petcarescheduler.usecase.result.TutorsPageResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class TutorControllerTest {
    private val service: TutorAppService = mock(TutorAppService::class.java)
    private val mapper: TutorDtoMapper = object : TutorDtoMapper {}
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.standaloneSetup(TutorController(service, mapper)).build()
    }

    @Test
    fun `create tutor returns 201`() {
        `when`(service.createTutor(any())).thenReturn(TutorCreatedResult(TutorId(1)))
        val req = TutorDtoMapper.CreateRequest("Ana", null, "a@e.com", "pwd", "1", null)

        mvc.perform(post("/api/v1/tutors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tutorId").value(1))
    }

    @Test
    fun `list tutors returns page`() {
        val page = TutorsPageResult(listOf(TutorSummary(TutorId(1),"Ana","a@e.com",0)),1,0,20)
        `when`(service.listTutors(0,20)).thenReturn(page)

        mvc.perform(get("/api/v1/tutors"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(1))
    }
}
