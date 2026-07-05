package dev.vilquer.petcarescheduler.infra

import com.fasterxml.jackson.databind.JsonNode
import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

/**
 * Rede de segurança da Fase 1: exercita o fluxo completo da API com o contexto
 * real (H2 + Flyway + JWT). Se o wiring de qualquer bean sumir durante a
 * migração de módulos, este teste quebra imediatamente.
 */
@SpringBootTest(
    classes = [PetCareSchedulerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "server.ssl.enabled=false",
        "app.mail.from=noreply@test.local",
    ],
)
@ActiveProfiles("dev")
class SmokeE2ETest {

    @Autowired
    lateinit var rest: TestRestTemplate

    private fun jsonHeaders(token: String? = null) = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        token?.let { setBearerAuth(it) }
    }

    @Test
    fun `full journey - signup, login, pet, event, toggle, delete`() {
        // --- signup ---
        val signup = rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Ana","lastName":"Silva","email":"ana.smoke@example.com","rawPassword":"s3cret-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CREATED, signup.statusCode, "signup: ${signup.body}")
        assertTrue(signup.body!!["tutorId"].asLong() > 0)

        // --- login ---
        val login = rest.postForEntity(
            "/api/v1/auth/login",
            HttpEntity("""{"email":"ana.smoke@example.com","password":"s3cret-pwd"}""", jsonHeaders()),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode, "login: ${login.body}")
        val token = login.body!!["token"].asText()
        assertTrue(token.isNotBlank())

        // --- rota protegida sem token é rejeitada ---
        val anonymous = rest.getForEntity("/api/v1/pets", JsonNode::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.statusCode)

        // --- perfil ---
        val me = rest.exchange(
            "/api/v1/tutors/me", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, me.statusCode, "me: ${me.body} / ${me.headers["WWW-Authenticate"]}")
        assertEquals("ana.smoke@example.com", me.body!!["email"].asText())

        // --- cria pet ---
        val petCreated = rest.postForEntity(
            "/api/v1/pets",
            HttpEntity(
                """{"name":"Rex","species":"Dog","breed":"SRD","birthdate":"2020-01-01"}""",
                jsonHeaders(token),
            ),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CREATED, petCreated.statusCode, "create pet: ${petCreated.body}")
        val petId = petCreated.body!!["petId"].asLong()

        // --- lista pets ---
        val pets = rest.exchange(
            "/api/v1/pets", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, pets.statusCode)
        assertEquals(1, pets.body!!["items"].size(), "pets: ${pets.body}")
        assertEquals("Rex", pets.body!!["items"][0]["name"].asText())

        // --- cria evento ---
        val eventCreated = rest.postForEntity(
            "/api/v1/events",
            HttpEntity(
                """{"petId":$petId,"type":"VACCINE","description":"antirrabica","dateStart":"2099-01-01T10:00:00"}""",
                jsonHeaders(token),
            ),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CREATED, eventCreated.statusCode, "create event: ${eventCreated.body}")
        val eventId = eventCreated.body!!["eventId"].asLong()

        // --- lista eventos do tutor ---
        val events = rest.exchange(
            "/api/v1/events", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, events.statusCode)
        assertEquals(1, events.body!!["items"].size(), "events: ${events.body}")
        assertEquals("PENDING", events.body!!["items"][0]["status"].asText())

        // --- toggle para DONE ---
        val toggle = rest.exchange(
            "/api/v1/events/$eventId/toggle", HttpMethod.PUT,
            HttpEntity<Void>(jsonHeaders(token)), Void::class.java,
        )
        assertEquals(HttpStatus.OK, toggle.statusCode)
        val toggled = rest.exchange(
            "/api/v1/events/$eventId", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals("DONE", toggled.body!!["status"].asText(), "toggled: ${toggled.body}")

        // --- deleta evento e pet ---
        val delEvent = rest.exchange(
            "/api/v1/events/$eventId", HttpMethod.DELETE, HttpEntity<Void>(jsonHeaders(token)), Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, delEvent.statusCode)
        val delPet = rest.exchange(
            "/api/v1/pets/$petId", HttpMethod.DELETE, HttpEntity<Void>(jsonHeaders(token)), Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, delPet.statusCode)

        // --- pet já excluído responde 404 real, não 400 ---
        val petNotFound = rest.exchange(
            "/api/v1/pets/$petId", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.NOT_FOUND, petNotFound.statusCode, "pet not found: ${petNotFound.body}")
        assertEquals(404, petNotFound.body!!["status"].asInt())

        val petsAfter = rest.exchange(
            "/api/v1/pets", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(0, petsAfter.body!!["items"].size(), "pets after delete: ${petsAfter.body}")
    }

    @Test
    fun `login rate limit blocks after repeated failed attempts`() {
        // signup próprio para não competir por janela de rate limit com o outro teste
        rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Bea","lastName":null,"email":"bea.smoke@example.com","rawPassword":"correct-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )

        fun wrongLogin() = HttpEntity("""{"email":"bea.smoke@example.com","password":"wrong-pwd"}""", jsonHeaders())
        // default: maxAttempts=5 na janela; a 6a tentativa deve ser bloqueada
        repeat(5) {
            val attempt = rest.postForEntity("/api/v1/auth/login", wrongLogin(), JsonNode::class.java)
            assertEquals(HttpStatus.UNAUTHORIZED, attempt.statusCode, "attempt $it: ${attempt.body}")
        }
        val blocked = rest.postForEntity("/api/v1/auth/login", wrongLogin(), JsonNode::class.java)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.statusCode, "blocked: ${blocked.body}")
    }

    @Test
    fun `password reset request runs its atomic transaction without a real SpringTransactionPort wiring gap`() {
        rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Carla","lastName":null,"email":"carla.smoke@example.com","rawPassword":"correct-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )

        // O token vai por e-mail (fora do escopo deste teste); o que importa
        // aqui é que a transação real (invalidar + criar token) não estoure
        // por falta do bean de PlatformTransactionManager/TransactionTemplate.
        val forgot = rest.postForEntity(
            "/api/v1/auth/password/forgot",
            HttpEntity("""{"email":"carla.smoke@example.com"}""", jsonHeaders()),
            Void::class.java,
        )
        assertEquals(HttpStatus.ACCEPTED, forgot.statusCode)

        val invalidToken = rest.exchange(
            "/api/v1/auth/password/reset/validate?token=not-a-real-token",
            HttpMethod.GET, HttpEntity<Void>(jsonHeaders()), Void::class.java,
        )
        assertEquals(HttpStatus.BAD_REQUEST, invalidToken.statusCode)
    }
}
