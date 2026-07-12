package dev.vilquer.petcarescheduler.infra

import com.fasterxml.jackson.databind.JsonNode
import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.ReminderOutboxJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DispatchPendingRemindersUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SendDailyRemindersUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.RegisterEventUseCase
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
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
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

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

    @Autowired
    lateinit var sendDailyReminders: SendDailyRemindersUseCase

    @Autowired
    lateinit var dispatchPendingReminders: DispatchPendingRemindersUseCase

    @Autowired
    lateinit var reminderOutboxRepository: ReminderOutboxJpaRepository

    @Autowired
    lateinit var registerLegacyEvent: RegisterEventUseCase

    private fun jsonHeaders(token: String? = null) = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        token?.let { setBearerAuth(it) }
    }

    private fun refreshCookieValue(response: org.springframework.http.ResponseEntity<*>): String {
        val setCookie = response.headers["Set-Cookie"]?.firstOrNull { it.startsWith("refresh_token=") }
            ?: error("no refresh_token cookie in response: ${response.headers}")
        return setCookie.substringAfter("refresh_token=").substringBefore(";")
    }

    private fun cookieHeaders(cookieValue: String) = HttpHeaders().apply {
        set("Cookie", "refresh_token=$cookieValue")
    }

    @Test
    fun `full journey - signup, login, pet, care plan, safe completion, undo and delete`() {
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

        // --- cria plano recorrente; ocorrências independem da conclusão anterior ---
        val startAt = LocalDateTime.now(ZoneId.systemDefault()).plusDays(1).truncatedTo(ChronoUnit.MINUTES)
        val planCreated = rest.postForEntity(
            "/api/v1/care-plans",
            HttpEntity(
                """{"petId":$petId,"type":"VACCINE","title":"Antirrábica","instructions":"Levar carteira","startAt":"$startAt","frequency":"DAILY","intervalCount":1,"repetitions":2,"reminderMinutesBefore":30}""",
                jsonHeaders(token),
            ),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CREATED, planCreated.statusCode, "create care plan: ${planCreated.body}")
        val planId = planCreated.body!!["id"].asText()

        // --- lista duas ocorrências materializadas no horizonte ---
        val occurrences = rest.exchange(
            "/api/v1/care-occurrences", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, occurrences.statusCode)
        assertEquals(2, occurrences.body!!["items"].size(), "occurrences: ${occurrences.body}")
        assertEquals("SCHEDULED", occurrences.body!!["items"][0]["status"].asText())
        val occurrenceId = occurrences.body!!["items"][0]["id"].asText()

        // --- dashboard agregado evita N+1 no frontend ---
        val dashboard = rest.exchange(
            "/api/v1/dashboard", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(token)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, dashboard.statusCode, "dashboard: ${dashboard.body}")
        assertEquals(1, dashboard.body!!["totalPets"].asInt())
        assertEquals(2, dashboard.body!!["totalEvents"].asInt())
        assertEquals("ana.smoke@example.com", dashboard.body!!["email"].asText())

        // --- conclusão idempotente e protegida contra administração em dobro ---
        val completionRequest = UUID.randomUUID()
        val complete = rest.postForEntity(
            "/api/v1/care-occurrences/$occurrenceId/complete",
            HttpEntity("""{"requestId":"$completionRequest","note":"Aplicada"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, complete.statusCode, "complete: ${complete.body}")
        assertEquals("COMPLETED", complete.body!!["status"].asText())
        val replay = rest.postForEntity(
            "/api/v1/care-occurrences/$occurrenceId/complete",
            HttpEntity("""{"requestId":"$completionRequest","note":"Aplicada"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, replay.statusCode)
        val duplicate = rest.postForEntity(
            "/api/v1/care-occurrences/$occurrenceId/complete",
            HttpEntity("""{"requestId":"${UUID.randomUUID()}"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CONFLICT, duplicate.statusCode)

        // --- desfazer dentro da janela segura ---
        val undo = rest.postForEntity(
            "/api/v1/care-occurrences/$occurrenceId/undo",
            HttpEntity("""{"requestId":"${UUID.randomUUID()}"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, undo.statusCode, "undo: ${undo.body}")
        assertEquals("SCHEDULED", undo.body!!["status"].asText())

        val sameRequestId = UUID.randomUUID()
        val idempotencyPool = Executors.newFixedThreadPool(2)
        try {
            val sameRequestStatuses = (1..2).map {
                CompletableFuture.supplyAsync({
                    rest.postForEntity(
                        "/api/v1/care-occurrences/$occurrenceId/complete",
                        HttpEntity("""{"requestId":"$sameRequestId"}""", jsonHeaders(token)),
                        JsonNode::class.java,
                    ).statusCode
                }, idempotencyPool)
            }.map { it.get() }
            assertTrue(sameRequestStatuses.all { it == HttpStatus.OK }, "a mesma chave deve reproduzir o sucesso: $sameRequestStatuses")
        } finally {
            idempotencyPool.shutdownNow()
        }
        val secondUndo = rest.postForEntity(
            "/api/v1/care-occurrences/$occurrenceId/undo",
            HttpEntity("""{"requestId":"${UUID.randomUUID()}"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, secondUndo.statusCode)

        val completionPool = Executors.newFixedThreadPool(2)
        try {
            val concurrentStatuses = (1..2).map {
                CompletableFuture.supplyAsync({
                    rest.postForEntity(
                        "/api/v1/care-occurrences/$occurrenceId/complete",
                        HttpEntity("""{"requestId":"${UUID.randomUUID()}"}""", jsonHeaders(token)),
                        JsonNode::class.java,
                    ).statusCode
                }, completionPool)
            }.map { it.get() }
            assertEquals(1, concurrentStatuses.count { it == HttpStatus.OK }, "apenas uma conclusão pode vencer: $concurrentStatuses")
            assertEquals(1, concurrentStatuses.count { it == HttpStatus.CONFLICT }, "a duplicada deve ser bloqueada: $concurrentStatuses")
        } finally {
            completionPool.shutdownNow()
        }

        // --- encerra plano preservando o que já foi registrado e exclui pet ---
        val endPlan = rest.exchange(
            "/api/v1/care-plans/$planId", HttpMethod.DELETE, HttpEntity<Void>(jsonHeaders(token)), Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, endPlan.statusCode)
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
    fun `concurrent login attempts do not return conflict`() {
        rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Dan","lastName":null,"email":"dan.smoke@example.com","rawPassword":"correct-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )

        val loginBody = HttpEntity("""{"email":"dan.smoke@example.com","password":"wrong-pwd"}""", jsonHeaders())
        val pool = Executors.newFixedThreadPool(10)
        try {
            val statuses = (1..10).map {
                CompletableFuture.supplyAsync({
                    rest.postForEntity("/api/v1/auth/login", loginBody, JsonNode::class.java).statusCode
                }, pool)
            }.map { it.get() }

            assertTrue(
                statuses.none { it == HttpStatus.CONFLICT },
                "login concorrente não deve retornar 409: $statuses",
            )
        } finally {
            pool.shutdownNow()
        }
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

    @Test
    fun `reminder outbox enqueues against the real database and the relay retries on delivery failure`() {
        val signup = rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Duda","lastName":null,"email":"duda.smoke@example.com","rawPassword":"correct-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.CREATED, signup.statusCode)
        val tutorId = TutorId(signup.body!!["tutorId"].asLong())

        val login = rest.postForEntity(
            "/api/v1/auth/login",
            HttpEntity("""{"email":"duda.smoke@example.com","password":"correct-pwd"}""", jsonHeaders()),
            JsonNode::class.java,
        )
        val token = login.body!!["token"].asText()

        val petCreated = rest.postForEntity(
            "/api/v1/pets",
            HttpEntity("""{"name":"Toby","species":"Dog","breed":null,"birthdate":"2020-01-01"}""", jsonHeaders(token)),
            JsonNode::class.java,
        )
        val petId = petCreated.body!!["petId"].asLong()

        // dateStart "hoje", no fim do dia, no fuso da aplicação: precisa estar
        // dentro da janela que o scheduler de detecção varre e ainda satisfazer
        // @FutureOrPresent no DTO (por isso não usamos meio-dia, que já teria
        // passado se o teste rodar à tarde).
        val todayLate = java.time.LocalDate.now(ZoneId.of("America/Sao_Paulo")).atTime(23, 59)
        val eventCreated = registerLegacyEvent.execute(
            RegisterEventCommand(PetId(petId), EventType.VACCINE, "lembrete real", todayLate),
            tutorId,
        )
        assertTrue(eventCreated.eventId.value > 0)

        // Chama os beans diretamente (em vez de esperar o cron) para exercitar
        // o ReminderOutboxJpaAdapter de verdade, contra o H2, sem fakes.
        sendDailyReminders.sendRemindersForToday()
        sendDailyReminders.sendRemindersForToday() // idempotência: não deve duplicar a mensagem

        val enqueued = reminderOutboxRepository.findAll().filter { it.tutorEmail == "duda.smoke@example.com" }
        assertEquals(1, enqueued.size, "a segunda varredura não deve ter criado uma segunda mensagem")
        assertEquals(0, enqueued.first().attempts)

        // A API de e-mail não está configurada neste ambiente de teste, então
        // a entrega falha de verdade — o que comprova que o relay trata falha
        // como "tentar de novo", não como sucesso silencioso.
        dispatchPendingReminders.dispatchPendingReminders()

        val afterDispatch = reminderOutboxRepository.findById(enqueued.first().id!!).orElseThrow()
        assertEquals(1, afterDispatch.attempts, "a tentativa de entrega deve ter sido contabilizada")
        assertTrue(afterDispatch.sentAt == null, "a API de e-mail indisponível não deve ter sido marcada como enviada")
    }

    @Test
    fun `refresh token rotation, reuse detection and logout`() {
        rest.postForEntity(
            "/api/v1/public/signup",
            HttpEntity(
                """{"firstName":"Fefe","lastName":null,"email":"fefe.smoke@example.com","rawPassword":"correct-pwd"}""",
                jsonHeaders(),
            ),
            JsonNode::class.java,
        )

        val login = rest.postForEntity(
            "/api/v1/auth/login",
            HttpEntity("""{"email":"fefe.smoke@example.com","password":"correct-pwd"}""", jsonHeaders()),
            JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode)
        val firstRefreshCookie = refreshCookieValue(login)

        // --- refresh rotaciona: novo access + novo cookie ---
        val refreshed = rest.postForEntity(
            "/api/v1/auth/refresh", HttpEntity<Void>(cookieHeaders(firstRefreshCookie)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, refreshed.statusCode, "refresh: ${refreshed.body}")
        val newAccessToken = refreshed.body!!["token"].asText()
        assertTrue(newAccessToken.isNotBlank())
        val secondRefreshCookie = refreshCookieValue(refreshed)
        assertTrue(secondRefreshCookie != firstRefreshCookie, "refresh deve rotacionar o cookie")

        // --- o novo access token funciona numa rota protegida ---
        val me = rest.exchange(
            "/api/v1/tutors/me", HttpMethod.GET, HttpEntity<Void>(jsonHeaders(newAccessToken)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.OK, me.statusCode, "me: ${me.body}")

        // --- reapresentar o cookie antigo (já rotacionado) é reuso: revoga a família inteira ---
        val reuse = rest.postForEntity(
            "/api/v1/auth/refresh", HttpEntity<Void>(cookieHeaders(firstRefreshCookie)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.statusCode, "reuse: ${reuse.body}")

        // --- e a família inteira morreu: o cookie rotacionado (válido) também não serve mais ---
        val deadFamily = rest.postForEntity(
            "/api/v1/auth/refresh", HttpEntity<Void>(cookieHeaders(secondRefreshCookie)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, deadFamily.statusCode, "dead family: ${deadFamily.body}")

        // --- logout revoga o cookie atual ---
        val login2 = rest.postForEntity(
            "/api/v1/auth/login",
            HttpEntity("""{"email":"fefe.smoke@example.com","password":"correct-pwd"}""", jsonHeaders()),
            JsonNode::class.java,
        )
        val cookieToLogout = refreshCookieValue(login2)
        val logout = rest.postForEntity(
            "/api/v1/auth/logout", HttpEntity<Void>(cookieHeaders(cookieToLogout)), Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, logout.statusCode)
        val refreshAfterLogout = rest.postForEntity(
            "/api/v1/auth/refresh", HttpEntity<Void>(cookieHeaders(cookieToLogout)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, refreshAfterLogout.statusCode)

        // --- logout-all exige autenticação por access token ---
        val logoutAllAnonymous = rest.postForEntity("/api/v1/auth/logout-all", HttpEntity<Void>(jsonHeaders()), Void::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, logoutAllAnonymous.statusCode)

        val login3 = rest.postForEntity(
            "/api/v1/auth/login",
            HttpEntity("""{"email":"fefe.smoke@example.com","password":"correct-pwd"}""", jsonHeaders()),
            JsonNode::class.java,
        )
        val accessToken3 = login3.body!!["token"].asText()
        val cookie3 = refreshCookieValue(login3)
        val logoutAll = rest.postForEntity(
            "/api/v1/auth/logout-all", HttpEntity<Void>(jsonHeaders(accessToken3)), Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, logoutAll.statusCode)
        val refreshAfterLogoutAll = rest.postForEntity(
            "/api/v1/auth/refresh", HttpEntity<Void>(cookieHeaders(cookie3)), JsonNode::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, refreshAfterLogoutAll.statusCode)
    }
}
