package dev.vilquer.petcarescheduler.application.adapter.input.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.application.service.PetAppService
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.DeletePetCommand
import dev.vilquer.petcarescheduler.usecase.result.PetCreatedResult
import dev.vilquer.petcarescheduler.usecase.result.PetsPageResult
import dev.vilquer.petcarescheduler.usecase.result.PetSummary
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentCaptor
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PetControllerTest {
    private val service: PetAppService = mock(PetAppService::class.java)
    private val mapper: PetDtoMapper = object : PetDtoMapper {}
    private lateinit var mvc: MockMvc
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.standaloneSetup(PetController(service, mapper)).build()
    }

    @Test
    fun `create pet returns 201`() {
        `when`(service.createPet(any())).thenReturn(PetCreatedResult(PetId(5)))
        val req = PetDtoMapper.CreateRequest("Rex","Dog",null,"2020-01-01",1)

        mvc.perform(post("/api/v1/pets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.petId").value(5))
    }

    @Test
    fun `list pets returns page`() {
        val page = PetsPageResult(listOf(PetSummary(PetId(1),"Rex","Dog")),1,0,20)
        `when`(service.listPets(0,20)).thenReturn(page)

        mvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(1))
    }

    @Test
    fun `delete pet delegates to service`() {
        val captor = ArgumentCaptor.forClass(DeletePetCommand::class.java)
        doNothing().`when`(service).deletePet(captor.capture())

        mvc.perform(delete("/api/v1/pets/3"))
            .andExpect(status().isNoContent)

        verify(service).deletePet(any())
        assert(PetId(3) == captor.value.petId)
    }
}
