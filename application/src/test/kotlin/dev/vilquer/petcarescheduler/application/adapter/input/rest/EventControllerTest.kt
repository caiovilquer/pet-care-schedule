package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.EventDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDateTime

class EventControllerTest {

    private val registerEvent: RegisterEventUseCase = mock()
    private val deleteEvent: DeleteEventUseCase = mock()
    private val updateEvent: UpdateEventUseCase = mock()
    private val toggleEvent: ToggleEventUseCase = mock()
    private val listEvents: ListEventsUseCase = mock()
    private val listPetEvents: ListPetEventsUseCase = mock()
    private val getEvent: GetEventUseCase = mock()
    private val mapper = EventDtoMapper()            // classe concreta
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders
            .standaloneSetup(
                EventController(
                    registerEvent,
                    deleteEvent,
                    updateEvent,
                    toggleEvent,
                    listEvents,
                    listPetEvents,
                    getEvent,
                    mapper
                )
            )
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
    }

    @Test
    fun `register returns 201`() {
        setJwtPrincipal()
        // stub do serviço
        whenever(registerEvent.execute(any(), any()))
            .thenReturn(EventRegisteredResult(EventId(2)))

        val req = EventDtoMapper.RegisterRequest(
            petId       = 1,
            type        = "SERVICE",
            description = "desc",
            dateStart   = LocalDateTime.of(2025, 7, 1, 10, 0),
            frequency   = null
        )

        mvc.perform(
            post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventId").value(2))

        // opcional: verificar que o serviço foi chamado
        verify(registerEvent).execute(any(), eq(TutorId(1)))
    }

    private fun setJwtPrincipal(tutorId: Long = 1L) {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(tutorId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(jwt, null)
    }
}
