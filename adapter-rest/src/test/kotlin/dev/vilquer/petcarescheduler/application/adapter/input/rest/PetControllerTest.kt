package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeletePetCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate

class PetControllerTest {

    private val createPet: CreatePetUseCase = mock()
    private val listPets: ListPetsUseCase = mock()
    private val updatePet: UpdatePetUseCase = mock()
    private val deletePet: DeletePetUseCase = mock()
    private val getPet: GetPetUseCase = mock()
    private val mapper = PetDtoMapper()               // classe concreta
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders
            .standaloneSetup(
                PetController(
                    mapper,
                    createPet,
                    listPets,
                    updatePet,
                    deletePet,
                    getPet
                )
            )
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()
    }

    @Test
    fun `create pet returns 201`() {
        setJwtPrincipal()
        whenever(createPet.execute(any()))
            .thenReturn(PetCreatedResult(PetId(5)))

        val req = PetDtoMapper.CreateRequest(
            name      = "Rex",
            species   = "Dog",
            breed     = null,
            birthdate = LocalDate.of(2020, 1, 1),
            photoUrl  = null
        )

        mvc.perform(
            post("/api/v1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.petId").value(5))
    }

    @Test
    fun `list pets returns page`() {
        setJwtPrincipal()
        val page = PetsPageResult(
            items = listOf(PetSummary(PetId(1), "Rex", "Dog", photoUrl = "https://example.com/pets/rex.png")),
            total = 1, page = 0, size = 20
        )

        // Matchers sobre value classes não casam com o argumento desembrulhado; usar valores concretos
        whenever(listPets.list(TutorId(1), 0, 20)).thenReturn(page)

        mvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(1))
    }

    @Test
    fun `delete pet delegates to service`() {
        setJwtPrincipal()
        mvc.perform(delete("/api/v1/pets/3"))
            .andExpect(status().isNoContent)

        // Matchers/captors sobre value classes explodem no unboxing; usar valores concretos
        verify(deletePet).execute(DeletePetCommand(PetId(3)), TutorId(1))
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
