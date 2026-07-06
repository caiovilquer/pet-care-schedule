# Fase 4 — Plano de implementação: higiene de plataforma

Data: 2026-07-05

## 1. Objetivo e escopo

Atualizar o toolchain, organizar a gestão de versões, fechar a lacuna entre
H2-em-modo-Postgres e o Postgres real nos testes de integração, e dar à
aplicação observabilidade mínima de produção. Esta é a fase de "arrumar a
casa" depois de três fases de mudança estrutural — sem features novas, sem
mudança de comportamento de negócio.

**Entra nesta fase**
- Gradle version catalog (`libs.versions.toml`): toda versão de plugin/
  dependência centralizada, hoje espalhada e duplicada em 6 `build.gradle.kts`.
- Kotlin 1.9.25 → 2.0.x, Spring Boot 3.3.5 → 3.5.x, toolchain Java 17 → 21
  (o JDK 21 já está instalado no ambiente; o README já pedia "JDK 21+" desde
  o início — o código é quem estava desalinhado).
- Testcontainers (Postgres real) para os testes de `adapter-persistence`
  que hoje rodam contra H2 em modo Postgres.
- Observabilidade mínima: Actuator + Micrometer, correlation ID por
  requisição propagado ao log.
- Varredura de código morto remanescente.

**Fica fora**
- **Redis para `PasswordChangedAtCache`/rate limit** — a proposta original
  já marcava isso como opcional "quando houver mais de uma instância". A
  Fase 3 resolveu o problema real de multi-instância que existia (schedulers
  duplicados) via ShedLock, sem precisar de infraestrutura nova. Trocar um
  `ConcurrentHashMap` por Redis agora, sem um segundo ambiente rodando de
  verdade para justificar, seria infraestrutura especulativa — o oposto do
  que essas instruções pedem. Fica documentado aqui como decisão consciente,
  não esquecimento.
- Tracing distribuído (Zipkin/Jaeger) — não há mais de um serviço na
  topologia hoje; correlation ID por log já resolve o problema real
  ("achar todas as linhas de uma requisição"), tracing distribuído resolve
  um problema que só existe com múltiplos serviços.

## 2. Decisões de design

| # | Decisão | Justificativa |
|---|---------|----------------|
| D1 | Version catalog primeiro, upgrade de versão depois (PRs separados) | Um catalog introduzido junto com um upgrade de versão mistura dois riscos num commit só; separar deixa o catalog (mecânico, zero risco) auditável isoladamente do upgrade (o item de maior risco da fase). |
| D2 | Escolher uma versão de cada vez e deixar o Gradle confirmar/falhar a resolução | Mesma técnica já usada para Konsist e ShedLock nas fases anteriores — sem acesso a rede para consultar o Maven Central diretamente, a resolução do próprio Gradle é a fonte de verdade sobre o que existe. |
| D3 | Testcontainers só em `adapter-persistence` (onde o H2-em-modo-Postgres já vive), não em `bootstrap` | O smoke E2E em `bootstrap` já teria custo alto rodando contra um container por execução; o valor de "testar contra Postgres de verdade" está concentrado exatamente nos testes de mapeamento/schema, que já vivem em `adapter-persistence`. |
| D4 | Correlation ID via filtro simples (`MDC` + header `X-Request-Id`), não Micrometer Tracing completo | Micrometer Tracing (Brave/OTel) traz exportadores, contexto de span/trace e overhead pensados para múltiplos serviços correlacionados. Um filtro que gera/propaga um ID e o injeta no `MDC` resolve o problema real de hoje (correlacionar linhas de log de uma mesma requisição) com uma fração do código. |
| D5 | Actuator expõe só `health`, `info`, `metrics` — não `env`/`beans`/`heapdump` | Esses três já cobrem o caso de uso (liveness/readiness de infra, métricas para scraping); os demais vazam detalhes internos e não têm consumidor hoje. |
| D6 | `kotlin("plugin.jpa")` (hoje fixado em `"1.9.25"` só em `adapter-persistence`, já que o root não o declara) entra no catalog como qualquer outro plugin Kotlin | Elimina o único lugar do projeto onde a versão do Kotlin não vinha de uma única fonte — motivo original de o catalog existir. |

## 3. Sequência de execução

### PR 4-A — Gradle version catalog
- Novo `gradle/libs.versions.toml`: versões de Kotlin, Spring Boot,
  Spring Dependency Management, foojay-resolver, jjwt, ShedLock, Konsist,
  JUnit Jupiter, mockito-kotlin — todas nos valores **atuais** (zero
  mudança de versão neste PR, só de mecanismo).
- `settings.gradle.kts` e os 6 `build.gradle.kts` passam a referenciar
  `libs.plugins.*`/`libs.*` em vez de strings literais.
- Nenhuma mudança de comportamento esperada; build deve ficar idêntico.

### PR 4-B — Upgrade de Kotlin, Spring Boot e JDK
- Catalog: Kotlin → 2.0.x, Spring Boot → 3.5.x, toolchain Java → 21 (em
  `subprojects` no root `build.gradle.kts` e em cada `kotlin { jvmToolchain(..) }`).
- Rodar build módulo a módulo se necessário para isolar qual dependência
  quebra primeiro (Kotlin 2.0 usa o compilador K2 por padrão — mudança de
  maior risco desta fase).
- README: já dizia "JDK 21+"; nenhuma mudança de texto necessária, só o
  código alcançando o que a doc já prometia.

### PR 4-C — Varredura de código morto
- Buscar imports/funções/classes sem uso introduzidos ou esquecidos ao
  longo das Fases 1–3 (o hábito de cada fase já limpa o que ela mesma
  gera; esta é uma segunda passada específica sobre o estado atual).

### PR 4-D — Testcontainers (Postgres) em `adapter-persistence`
- Trocar o H2-em-modo-Postgres dos testes `@DataJpaTest` por um Postgres
  real via Testcontainers (`@Testcontainers` + `@Container` +
  `@DynamicPropertySource`, ou `@ServiceConnection` se a versão do Boot
  suportar).
- `PersistenceTestApplication` e as migrações `common`/`postgresql`
  passam a ser exercitadas de verdade (hoje `h2`/`common` são o caminho
  testado; `postgresql`/`common` só roda em produção).
- Mantém o H2 do perfil `dev`/`bootstrap` intocado — é o ambiente de
  desenvolvimento local sem Docker, não o de teste automatizado.

### PR 4-E — Observabilidade mínima
- Actuator (`health`, `info`, `metrics`) + Micrometer no `bootstrap`.
- Filtro de correlation ID (`X-Request-Id`): gera um UUID se o header não
  vier, propaga na resposta, injeta em `MDC` para aparecer no log.
- Padrão de log (`logging.pattern.console`) passa a incluir o id do MDC.

### PR 4-F — Fechamento
- Atualizar `AUDITORIA.md` (recomendação 3 — schedulers duplicados — já
  resolvida na Fase 3; registrar aqui também, já que a auditoria não foi
  tocada naquela fase) e README (versões, endpoints do Actuator).
- Definition of Done.

## 4. Riscos e mitigações

| Risco | Mitigação |
|-------|-----------|
| Kotlin 2.0 (K2) muda inferência de tipo em algum ponto do código e quebra a compilação | Build roda módulo a módulo; qualquer erro de compilação é corrigido antes de prosseguir, igual às fases anteriores |
| Spring Boot 3.5.x muda default de alguma autoconfiguração usada (JPA, Security, WebFlux) | Suite completa (91 testes + smoke E2E) roda a cada PR; qualquer regressão de comportamento aparece imediatamente |
| Testcontainers exige Docker no ambiente onde o build roda | Já confirmado disponível neste ambiente (`docker ps` responde); documentar a exigência no README para quem for rodar localmente |
| Actuator exposto sem autenticação vaza informação | Restringir no `SecurityConfig` a rota `/actuator/**` (permitAll só para `health`, resto exige autenticação, coerente com o resto da API) |

## 5. Definition of Done

- [x] `libs.versions.toml` é a única fonte de versão de cada dependência/plugin
      (única exceção documentada: o plugin `foojay-resolver-convention` em
      `settings.gradle.kts`, que não pode referenciar o catalog antes dele
      mesmo estar disponível)
- [x] Kotlin, Spring Boot e toolchain Java atualizados; `./gradlew build` e
      `./gradlew check` verdes
- [x] Testes de `adapter-persistence` rodam contra Postgres real via
      Testcontainers (as migrações `postgresql/` passam a ser exercitadas em
      teste, não só em produção)
- [x] `/actuator/health` responde; logs de uma mesma requisição
      compartilham o mesmo correlation ID
- [x] AUDITORIA.md e README refletem o estado atual

**Status: Fase 4 concluída (2026-07-05).** Seis PRs (4-A a 4-F) implementados
e commitados sequencialmente. Descobertas ao longo da execução, não previstas
no plano original:

- Os três `@DataJpaTest` de `adapter-persistence` não usavam Flyway nem H2 em
  modo PostgreSQL como a proposta original presumia — rodavam com DDL gerado
  pelo Hibernate a partir das anotações `@Entity`, contra H2 puro. A pasta
  `db/migration/postgresql/` nunca tinha sido exercitada por nenhum teste
  automatizado. A PR 4-D fechou essa lacuna de verdade (Flyway habilitado
  explicitamente + Testcontainers), não apenas trocou o driver de um teste
  que já rodava migrações.
- O bump para o BOM do Spring Boot 3.5 expôs um descompasso de versão entre
  o `junit-platform-launcher` que o próprio Gradle empacota e o
  `junit-platform-engine` mais novo resolvido via BOM — corrigido declarando
  `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` nos módulos
  com `spring-boot-starter-test` (fix documentado pelo próprio Spring Boot
  para esse cenário, não workaround improvisado).
- `Event.markDone()` (core) era código morto — nenhuma referência em todo o
  repo; `Event.complete()` já fazia a mesma transição inline. Removido na
  PR 4-C.

Todas as seis PRs verificadas com `./gradlew clean check` (30 tasks, 6
módulos) e com verificação ao vivo específica de cada uma (bootRun + curl
para PR 4-B e PR 4-E; `docker ps` confirmando um único container Postgres
compartilhado para PR 4-D).
