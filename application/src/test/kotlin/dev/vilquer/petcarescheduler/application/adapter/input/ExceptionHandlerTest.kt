package dev.vilquer.petcarescheduler.application.adapter.input

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(ExceptionHandlerTest.TestController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ExceptionHandler::class)
class ExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @RestController
    class TestController {
        @GetMapping("/illegal")
        fun illegal(): String {
            throw IllegalArgumentException("bad arg")
        }

        @GetMapping("/notfound")
        fun notFound(): String {
            throw NoSuchElementException("missing")
        }

        @GetMapping("/bad")
        fun badCredentials(): String {
            throw BadCredentialsException("bad creds")
        }
    }

    @Test
    fun `illegal argument maps to 400`() {
        mockMvc.perform(get("/illegal"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("bad arg"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `not found maps to 404`() {
        mockMvc.perform(get("/notfound"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("missing"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `bad credentials maps to 401`() {
        mockMvc.perform(get("/bad"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("bad creds"))
            .andExpect(jsonPath("$.timestamp").exists())
    }
}