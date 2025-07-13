package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.application.service.PetAppService
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.DeletePetCommand
import dev.vilquer.petcarescheduler.usecase.result.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PetControllerTest {

    private val service: PetAppService = mock()
    private val mapper = PetDtoMapper()               // classe concreta
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders
            .standaloneSetup(PetController(service, mapper))
            .build()
    }

    @Test
    fun `create pet returns 201`() {
        whenever(service.createPet(any()))
            .thenReturn(PetCreatedResult(PetId(5)))

        val req = PetDtoMapper.CreateRequest(
            name      = "Rex",
            specie    = "Dog",
            race      = null,
            birthdate = "2020-01-01",
            tutorId   = 1
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
        val page = PetsPageResult(
            items = listOf(PetSummary(PetId(1), "Rex", "Dog")),
            total = 1, page = 0, size = 20
        )

        whenever(service.listPets(eq(0), eq(20))).thenReturn(page)

        mvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(1))
    }

    @Test
    fun `delete pet delegates to service`() {
        val captor = argumentCaptor<DeletePetCommand>()
        doNothing().whenever(service).deletePet(captor.capture())

        mvc.perform(delete("/api/v1/pets/3"))
            .andExpect(status().isNoContent)

        verify(service).deletePet(any())                // garante que foi chamado
        assert(captor.firstValue.petId == PetId(3))     // verifica ID capturado
    }
}
