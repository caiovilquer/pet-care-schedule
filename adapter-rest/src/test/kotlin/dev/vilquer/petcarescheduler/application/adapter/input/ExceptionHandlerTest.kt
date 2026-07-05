package dev.vilquer.petcarescheduler.application.adapter.input

import dev.vilquer.petcarescheduler.application.adapter.input.rest.ApiExceptionHandler
import dev.vilquer.petcarescheduler.application.exception.InvalidCredentialsException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class ExceptionHandlerTest {

    @RestController
    class TestController {
        @GetMapping("/illegal")
        fun illegal(): String {
            throw IllegalArgumentException("bad arg")
        }

        @GetMapping("/notfound")
        fun notFound(): String {
            throw NotFoundException("missing")
        }

        @GetMapping("/bad")
        fun badCredentials(): String {
            throw InvalidCredentialsException("bad creds")
        }
    }

    private val mockMvc = MockMvcBuilders
        .standaloneSetup(TestController())
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `illegal argument maps to 400`() {
        mockMvc.perform(get("/illegal"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("bad arg"))
    }

    @Test
    fun `not found maps to 404`() {
        mockMvc.perform(get("/notfound"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("missing"))
    }

    @Test
    fun `bad credentials maps to 401`() {
        mockMvc.perform(get("/bad"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("bad creds"))
    }
}
