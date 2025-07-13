package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.EventDtoMapper
import dev.vilquer.petcarescheduler.application.service.EventAppService
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class EventControllerTest {
    private val service: EventAppService = mock(EventAppService::class.java)
    private val mapper: EventDtoMapper = object : EventDtoMapper {}
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.standaloneSetup(EventController(service, mapper)).build()
    }

    @Test
    fun `register returns 201`() {
        `when`(service.registerEvent(any())).thenReturn(EventRegisteredResult(EventId(2)))
        val req = EventDtoMapper.RegisterRequest(1,"SERVICE","desc","2025-07-01T10:00:00Z")

        mvc.perform(post("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventId").value(2))
    }
}
