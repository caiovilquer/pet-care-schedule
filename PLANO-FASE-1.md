# Fase 1 — Plano de implementação: redesenho dos módulos (consertar o hexágono)

Data: 2026-07-03

## 1. Objetivo e escopo

Reorganizar os módulos para que a estrutura física do projeto reflita a arquitetura
hexagonal: **camada de aplicação 100% livre de framework**, todos os adapters
(entrada e saída) do lado de fora, e a fronteira garantida por testes de
arquitetura (Konsist) — não por convenção.

**Entra nesta fase**
- Novo layout de módulos (`core`, `application`, `adapter-rest`, `adapter-persistence`,
  `adapter-messaging`, `bootstrap`).
- Novos ports que removem infraestrutura de dentro dos services: `TokenIssuerPort`,
  `RateLimitStorePort`, `PasswordResetNotifierPort`, `PasswordHashPort.matches()`.
- Ports de entrada para os schedulers (hoje chamam classe concreta).
- Remoção de código morto (MapStruct/kapt, finders sem uso, use case sem endpoint).
- Testes de arquitetura com Konsist rodando no `./gradlew check`.
- Uma única mudança de comportamento: **remover o envio de e-mail no cadastro de
  evento** (bug — "lembrete" disparado na criação, bloqueando a request).

**Fica fora (fases seguintes)**
- Erros tipados / 404 correto (Fase 2), agregados ricos (Fase 2).
- `@Transactional` no boundary, outbox, modelo de ocorrências (Fase 3).
- Upgrade Kotlin/Boot/JDK, version catalog, Testcontainers, observabilidade (Fase 4).

## 2. Decisões de design

| # | Decisão | Justificativa |
|---|---------|---------------|
| D1 | `application` **sem nenhuma dependência Spring**; beans declarados via `@Bean` no `bootstrap` | É a escolha de maior valor didático: a fronteira vira real, não anotada. Services perdem `@Service`/`@Value`/`@Transactional` (transação volta na Fase 3 via decisão própria). |
| D2 | Fundir `usecase` + services do atual `application` em um único módulo chamado `application` | Ports, commands, results e services são a mesma camada lógica; dois módulos só adicionavam cerimônia (apontado até na AUDITORIA.md como overengineering). |
| D3 | Três módulos de adapter (`adapter-rest`, `adapter-persistence`, `adapter-messaging`) + `bootstrap` | Um módulo por tecnologia/direção. Schedulers ficam no `bootstrap` (são adapters de entrada triviais de 5 linhas; criar `adapter-scheduler` seria módulo demais — tradeoff documentado). |
| D4 | Todo o maquinário JWT (emissão **e** validação) vai para `adapter-rest` | Emissão (`TokenIssuerPort`) e decodificação são as duas faces da mesma fronteira HTTP; compartilham `signingKey`/`JwtProperties`. Evita um 4º módulo de adapter. |
| D5 | Konsist (não ArchUnit) para os testes de arquitetura | Konsist analisa fonte Kotlin (imports, pacotes) e é mais idiomático; ArchUnit trabalha em bytecode e enxerga pior value classes/top-level functions. |
| D6 | Migrações Flyway movem para `adapter-persistence/src/main/resources/db/migration` | Schema é detalhe do adapter de persistência. O Boot encontra `db/migration` em qualquer jar do classpath. |
| D7 | Pacote raiz `dev.vilquer.petcarescheduler` permanece; subpacotes são renomeados junto com os módulos | `scanBasePackages` continua válido durante toda a migração. |

## 3. Arquitetura alvo

### 3.1 Módulos e dependências

```
settings.gradle.kts:
include("core", "application", "adapter-rest", "adapter-persistence",
        "adapter-messaging", "bootstrap")
```

```
                 ┌─────────────┐
                 │  bootstrap   │  Spring Boot app, yml, wiring (@Bean), schedulers
                 └──┬───┬───┬──┘
        ┌───────────┘   │   └────────────┐
┌───────▼──────┐ ┌──────▼───────────┐ ┌──▼────────────────┐
│ adapter-rest │ │ adapter-         │ │ adapter-messaging │
│ (driving)    │ │ persistence      │ │ (driven: mail)    │
└───────┬──────┘ │ (driven: JPA)    │ └──┬────────────────┘
        │        └──────┬───────────┘    │
        └───────────┬───┴────────────────┘
             ┌──────▼──────┐
             │ application │  use cases, ports, commands, results  (Kotlin puro)
             └──────┬──────┘
             ┌──────▼──────┐
             │    core     │  entidades, VOs, domain services      (Kotlin puro)
             └─────────────┘
```

Regras: adapters não dependem uns dos outros; `application` só de `core`; `core` de nada.

### 3.2 Layout de pacotes do novo `application`

```
dev.vilquer.petcarescheduler.application
├── port/driving/      CreatePetUseCase, AuthUseCase, SendDailyRemindersUseCase, ...
├── port/driven/       TutorRepositoryPort, TokenIssuerPort, RateLimitStorePort, ...
├── command/           CreatePetCommand, ...
├── result/            PetDetailResult, ...
├── service/           PetAppService, AuthAppService, RateLimiterService, ...
├── mapper/            ResultMapper (domínio → result)
└── error/             ConflictException, ForbiddenException, RateLimitException
```

### 3.3 Conteúdo de cada adapter

**adapter-rest** (depende de: application, core; libs: spring-web, spring-security,
oauth2-resource-server, jjwt, validation, jackson)
- Controllers: `AuthController`, `PasswordResetController`, `PetController`,
  `EventController`, `TutorController`, `PublicController`
- DTOs + mappers de request (`PetDtoMapper`, `EventDtoMapper`, `TutorDtoMapper`)
- `ApiExceptionHandler`
- Segurança web: `SecurityConfig`, `BearerTokenResolver`, `JwtDecoder`,
  `PasswordChangedAtCache`, `JwtCacheProperties`, `JwtProperties`, `JwtBeans`
- `JwtTokenIssuerAdapter` (implementa `TokenIssuerPort`, absorve o `Jwts.builder()`
  que hoje está no `AuthAppService`)
- `BCryptHashAdapter` (implementa `PasswordHashPort` — hoje está, errado, em
  `adapter/input/security`; é driven)
- `JacksonVoModule`, `CorsConfig`

**adapter-persistence** (depende de: application, core; libs: spring-data-jpa,
flyway, postgres/h2 runtime)
- Entidades JPA (`TutorJpa`, `PetJpa`, `EventJpa`, `RecurrenceEmb`,
  `PasswordResetTokenJpa`, **`RateLimitAttempt`** — sai do módulo application)
- Spring Data repositories + mappers manuais (`TutorMapper` etc.)
- Adapters: `TutorRepositoryAdapter`, `PetRepositoryAdapter`, `EventRepositoryAdapter`,
  `PasswordResetTokenJpaAdapter`, **`RateLimitStoreJpaAdapter` (novo)**
- `db/migration/*.sql`

**adapter-messaging** (depende de: application, core; libs: spring-webflux)
- `MailApiConfig` (WebClient + `MailApiProps`)
- `PasswordResetMailAdapter` (implementa `PasswordResetNotifierPort`; absorve
  montagem de link + template HTML que hoje está no `PasswordResetService`)
- `EventReminderMailAdapter` (implementa `NotificationPort` de método único;
  perde as dependências de `TutorRepositoryPort`/`PetRepositoryPort`)

**bootstrap** (depende de: todos; substitui o `infra`)
- `PetCareSchedulerApplication`, `application*.yml`, keystore SSL, `config/jvm.options`
- Schedulers: `EventReminderScheduler`, `SecurityMaintenanceScheduler`
  (dependendo **apenas de driving ports**)
- `UseCaseWiring` (`@Configuration` com um `@Bean` por service)
- Binding de properties → objetos de configuração puros passados aos services
- `ClockAdapter` (trivial; vira `@Bean ClockPort` aqui)

## 4. Novos ports (assinaturas)

```kotlin
// port/driven/PasswordHashPort.kt  (ganha matches; substitui PasswordEncoder nos services)
interface PasswordHashPort {
    fun hash(raw: CharSequence): String
    fun matches(raw: CharSequence, hash: String): Boolean
}

// port/driven/TokenIssuerPort.kt  (tira o jjwt do AuthAppService)
interface TokenIssuerPort {
    fun issueAccessToken(tutorId: TutorId, name: String): String
}

// port/driven/RateLimitStorePort.kt  (tira JPA do módulo application;
// a atomicidade — optimistic lock + retry — vira responsabilidade do adapter)
interface RateLimitStorePort {
    /** Registra uma tentativa e devolve o total acumulado na janela vigente. */
    fun registerAttempt(key: String, now: Instant, window: Duration): Int
    fun deleteOlderThan(cutoff: Instant): Int
}

// port/driven/PasswordResetNotifierPort.kt  (substitui MailSenderPort;
// link, remetente e HTML passam a ser detalhe do adapter)
interface PasswordResetNotifierPort {
    fun sendResetLink(to: Email, tokenPlain: String, ttl: Duration)
}

// port/driven/NotificationPort.kt  (fica só o método completo; o overload que
// fazia o adapter consultar repositórios é removido)
fun interface NotificationPort {
    fun sendEventReminder(target: EventReminderTarget)
}

// port/driving — schedulers deixam de chamar classe concreta
fun interface SendDailyRemindersUseCase { fun sendRemindersForToday() }
fun interface SecurityMaintenanceUseCase { fun cleanupSecurityArtifacts() }
```

Configurações que os services recebem hoje via `@Value`/`@ConfigurationProperties`
viram data classes puras construídas no bootstrap:

```kotlin
// application/service/PasswordResetPolicy.kt (puro)
data class PasswordResetPolicy(val ttl: Duration)

// application/service/RateLimitPolicy.kt (puro — substitui RateLimitProperties)
data class RateLimitPolicy(val login: Limit, val passwordReset: Limit) {
    data class Limit(val maxAttempts: Int, val window: Duration)
}
```

## 5. Mapa de movimentação (origem → destino)

| Hoje | Destino |
|------|---------|
| `usecase/**` (ports, commands, results) | `application` (pacotes `port/…`, `command`, `result`) |
| `application/service/*` | `application/service` (sem anotações Spring) |
| `application/mapper/ResultMapper.kt` | `application/mapper` |
| `application/mapper/{Pet,Event,Tutor}DtoMapper.kt` | `adapter-rest` |
| `application/adapter/input/rest/*` | `adapter-rest` |
| `application/config/{SecurityConfig,CorsConfig,JacksonVoModule,SecurityBeans}` | `adapter-rest` |
| `application/adapter/input/security/{JwtProperties,BCryptHashAdapter,CurrentTutor}` | `adapter-rest` |
| `application/security/{JwtCache,PasswordChangedAtCache}` | `adapter-rest` |
| `application/security/{RateLimitAttempt,RateLimitAttemptRepository}` | `adapter-persistence` |
| `application/adapter/input/scheduler/*` | `bootstrap` |
| `application/exception/*` | `application/error` |
| `infra/…/persistence/**`, `…/reset/**` | `adapter-persistence` |
| `infra/…/external/{Tutor,Pet,Event}RepositoryAdapter` | `adapter-persistence` |
| `infra/…/external/ClockAdapter` | `bootstrap` (vira `@Bean`) |
| `infra/…/mail/*`, `…/notification/*`, `infra/config/MailApiConfig` | `adapter-messaging` |
| `infra/src/main/resources/db/migration` | `adapter-persistence/src/main/resources/db/migration` |
| `infra/src/main/resources/{application*.yml,ssl/}` | `bootstrap/src/main/resources` |
| `infra/PetCareSchedulerApplication.kt` | `bootstrap` |

Testes acompanham suas classes: testes de controller → `adapter-rest`; testes de
mapper/repository de JPA → `adapter-persistence`; testes de service →
`application` (só JUnit + mockito-kotlin, sem spring-boot-starter-test);
`TestDoubles.kt` → `application` via plugin `java-test-fixtures`, para os fakes
serem reutilizados pelos testes dos adapters e do bootstrap.

## 6. Sequência de execução (PRs pequenos, build verde em cada um)

A ordem é: primeiro **purificar os ports dentro do layout atual** (PRs 2–5, cada
um verificável isoladamente), só depois **mover módulos** (PRs 6–7). Mover código
já limpo é mecânico; mover código acoplado espalha o problema.

### PR 0 — Rede de segurança (S)
Teste E2E de fumaça em `infra/src/test`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
com H2, fluxo completo via `TestRestTemplate`:
signup → login → cria pet → cria evento → lista eventos → toggle → get tutor /me → deletes.
É ele que garante que o wiring manual do PR 7 não esqueceu nenhum bean.
**Verificação:** `./gradlew build` verde; teste falha se qualquer bean sumir.

### PR 1 — Faxina (S)
- Remover `mapstruct` + `kapt` do `application/build.gradle.kts` (confirmado: zero usos).
- Deletar código morto: `EventJpaRepository.{findByType,findByDateStart,findByStatus}`,
  `PetJpaRepository.{existsByName,findByName,findBySpecie}`,
  `TutorJpaRepository.findByPhoneNumber`, `PetRepositoryPort.{findAll,countAll}` (+ impls),
  `ListTutorsUseCase` + `TutorAppService.list` + `TutorsPageResult`/`TutorSummary`
  (não existe endpoint; git guarda o histórico).
**Verificação:** build + PR 0 verdes.

### PR 2 — Ports de segurança (M)
- `PasswordHashPort.matches()`; `AuthAppService` e `PasswordResetService` trocam
  `PasswordEncoder` pelo port.
- `TokenIssuerPort` + `JwtTokenIssuerAdapter` (código do `Jwts.builder()` migra
  para o adapter); `AuthAppService` fica sem import de jjwt.
**Verificação:** login no PR 0 continua passando; `grep -r "io.jsonwebtoken" application/src/main/kotlin/**/service` vazio.

### PR 3 — Notificação semântica (M)
- `PasswordResetNotifierPort`; template HTML + montagem de link + `from` migram
  para o `MailSenderAdapter` (que passa a implementar o novo port; `MailSenderPort` morre).
- `NotificationPort` reduzido a 1 método (`EventReminderTarget`); overload com
  consulta a repositórios deletado; `EmailNotificationAdapter` perde `TutorRepositoryPort`/`PetRepositoryPort`.
- **Mudança de comportamento:** `RegisterEventUseCase` deixa de chamar `notifier` —
  era o e-mail de "lembrete" disparado no cadastro. Registrar no changelog/README.
**Verificação:** teste do scheduler (`EventReminderSchedulerTest`) ajustado e verde;
teste novo garantindo que registrar evento não envia e-mail.

### PR 4 — Rate limit para a persistência (M)
- Criar `RateLimitStorePort` no usecase; mover `RateLimitAttempt` + repository
  para `infra`; criar `RateLimitStoreJpaAdapter` (leva o loop de retry otimista
  e o `@Transactional`).
- `RateLimiterService` mantém só política (janela, máximo, exceção) usando
  `RateLimitPolicy` pura.
- Remover `spring-boot-starter-data-jpa` do `application/build.gradle.kts`.
**Verificação:** build verde sem starter-data-jpa no application; PR 0 verde.

### PR 5 — Schedulers via ports (S)
- `SendDailyRemindersUseCase` (implementado por `EventAppService`) e
  `SecurityMaintenanceUseCase` (novo `SecurityMaintenanceService` orquestrando
  `RateLimiterService.cleanup` + `PasswordResetTokenPort.cleanup`).
- Schedulers passam a injetar os ports.
**Verificação:** `EventReminderSchedulerTest` com mock do port.

### PR 6 — Novo esqueleto de módulos (L — fazer em 4 commits)
1. Renomear `infra` → `bootstrap` (settings.gradle, diretório, `run-prod.sh`).
2. Extrair `adapter-persistence` do bootstrap (código JPA + migrações Flyway —
   apagar do local antigo para não duplicar `db/migration` no classpath).
3. Extrair `adapter-messaging` do bootstrap (mail/notification + `MailApiConfig`).
4. Criar `adapter-rest` e mover para lá tudo de REST/segurança-web/Jackson/CORS
   que está no `application` (incl. jjwt e starters web/security do build).
**Verificação por commit:** `./gradlew build` + PR 0. Ao final, o módulo
`application` só contém services, mappers de result e exceptions.

### PR 7 — Purificação do `application` (L)
1. Mover o conteúdo restante do `application` antigo para dentro do módulo `usecase`.
2. Deletar o módulo `application` vazio; renomear `usecase` → `application`.
3. Reorganizar pacotes (`contract/drivingports` → `port/driving` etc.).
4. Remover `@Service`/`@Value`/`@Transactional`/imports Spring dos services;
   `build.gradle.kts` do novo `application` fica só com `project(":core")` + JUnit
   (+ `java-test-fixtures`).
5. Criar `UseCaseWiring` no bootstrap: um `@Bean` por service; properties viram
   `PasswordResetPolicy`/`RateLimitPolicy` construídas de `@ConfigurationProperties`
   do bootstrap.
**Verificação:** PR 0 (o contexto sobe ou não sobe — é aqui que ele paga o
investimento); `./gradlew :application:dependencies` sem nenhum artefato Spring.

### PR 8 — Konsist (M)
Módulo de teste no `bootstrap` (Konsist lê a árvore de fontes inteira do projeto):

```kotlin
class ArchitectureTest {
    private val forbiddenInCore = listOf("org.springframework", "jakarta", "io.jsonwebtoken",
                                         "com.fasterxml", "org.hibernate")

    @Test fun `core nao importa framework nem outras camadas`() =
        Konsist.scopeFromModule("core").files.assertFalse { file ->
            file.imports.any { imp -> forbiddenInCore.any { imp.name.startsWith(it) } ||
                                      imp.name.contains(".application.") ||
                                      imp.name.contains(".adapter.") }
        }

    @Test fun `application so importa core e stdlib`() = /* mesma técnica */

    @Test fun `adapters nao importam outros adapters`() = /* prefixo de pacote */

    @Test fun `driving ports terminam em UseCase e driven em Port`() = /* naming */
}
```

Regras mínimas: (1) core puro; (2) application sem Spring/Jakarta; (3) adapter
não importa adapter; (4) services implementam ao menos um driving port;
(5) convenção de nomes de ports. Plugar no `check` do Gradle.
**Verificação:** `./gradlew check` roda Konsist; quebrar de propósito um import
(ex.: Spring no core) e confirmar que falha.

### PR 9 — Documentação e encerramento (S)
- README: nova tabela de módulos + diagrama; nota sobre a remoção do e-mail no cadastro.
- `scripts/run-prod.sh` já ajustado no PR 6 (conferir caminho `bootstrap/build/libs/petcare.jar`).
- Atualizar AUDITORIA.md marcando o item de overengineering de módulos como tratado.

## 7. Riscos e mitigações

| Risco | Mitigação |
|-------|-----------|
| Wiring manual (`@Bean`) esquece dependência e o contexto não sobe | PR 0 (`@SpringBootTest` E2E) roda em todo PR; falha imediata e localizada |
| Duas cópias de `db/migration` no classpath durante o PR 6 | Mover e deletar no mesmo commit; smoke test sobe Flyway e detectaria checksum duplicado |
| Renomes de pacote quebram component scan | Pacote raiz `dev.vilquer.petcarescheduler` intocado (D7); `scanBasePackages` segue válido |
| `@ConfigurationProperties` de módulos adapter não registradas | Registrar via `@EnableConfigurationProperties`/`@ConfigurationPropertiesScan` no bootstrap; coberto pelo smoke test |
| Remoção do e-mail no cadastro surpreende usuário do frontend | Único ponto de comportamento alterado; documentado no PR 3 e no README |
| PRs 6–7 grandes geram conflito com outras frentes | Não abrir outras frentes durante os dois PRs; são majoritariamente `git mv` |

## 8. Definition of Done

- [ ] `settings.gradle.kts` = `core, application, adapter-rest, adapter-persistence, adapter-messaging, bootstrap`
- [ ] `./gradlew :application:dependencies` e `:core:dependencies` sem qualquer artefato Spring/Jakarta
- [ ] Konsist verde no `./gradlew check` (e falhando quando violado de propósito)
- [ ] Smoke E2E (PR 0) verde
- [ ] `./gradlew :bootstrap:bootRun` sobe com H2 + Swagger; `run-prod.sh` funciona com o novo caminho do jar
- [ ] Nenhum `io.jsonwebtoken`, `jakarta.persistence` ou template HTML dentro de `application/`
- [ ] README e AUDITORIA.md atualizados
