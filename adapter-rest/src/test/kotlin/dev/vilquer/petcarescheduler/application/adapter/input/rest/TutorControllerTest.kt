package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.config.JacksonVoModule
import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.TutorCreatedResult
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class TutorControllerTest {

    private val createTutor: CreateTutorUseCase = mock()
    private val updateTutor: UpdateTutorUseCase = mock()
    private val deleteTutor: DeleteTutorUseCase = mock()
    private val getTutor: GetTutorUseCase = mock()
    private val mapper = TutorDtoMapper()
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()
        .registerModule(
            JacksonVoModule().voModule()
        )

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.standaloneSetup(
            PublicController(createTutor, mapper),
            TutorController(updateTutor, deleteTutor, getTutor, mapper)
        )
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
    }

    @Test
    fun `create tutor returns 201`() {
        whenever(createTutor.execute(any()))
            .thenReturn(TutorCreatedResult(TutorId(1)))

        val req = TutorDtoMapper.CreateRequest(
            firstName   = "Ana",
            lastName    = null,
            email       = "a@e.com",
            rawPassword = "password123",
            phoneNumber = PhoneNumber.of("+5511998765432").getOrNull(),
            avatar      = null
        )

        mvc.perform(post("/api/v1/public/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tutorId").value(1))
        verify(createTutor).execute(any())
    }

    @Test
    fun `my profile returns detail`() {
        setJwtPrincipal()
        val detail = TutorDetailResult(
            id = TutorId(1),
            firstName = "Ana",
            lastName = null,
            email = "a@e.com",
            phoneNumber = null,
            avatar = null,
            pets = emptyList()
        )

        whenever(getTutor.get(TutorId(1))).thenReturn(detail)

        mvc.perform(get("/api/v1/tutors/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
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
