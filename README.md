# RotinaPet

Aplicação full‑stack para controlar cuidados recorrentes com animais de estimação.
Tutore(s) podem cadastrar seus pets, planejar eventos (vacinas, medicamentos,
banhos, serviços etc.) e receber lembretes por e‑mail no dia correto.

## Funcionalidades principais

- **Autenticação e cadastro de tutores**
  - Signup público (`/api/v1/public/signup`) e login com JWT (`/api/v1/auth/login`).
  - Perfil protegido, atualização e exclusão de tutores.

- **Gestão de pets**
  - CRUD completo de animais ligados a um tutor.
  - Paginação de listas e filtro por tutor.
  - Fotos de pets e avatar do tutor em object storage privado, com upload
    direto, validação de conteúdo e limpeza automática de órfãos.

- **Planos de cuidado e ocorrências**
  - Um plano guarda regra, recorrência, orientações e política de lembrete;
    cada execução concreta é uma ocorrência independente e auditável.
  - Tipos de evento: `VACCINE`, `MEDICINE`, `DIARY`, `BREED`, `SERVICE`.
  - Ocorrências dos próximos 90 dias são materializadas de forma idempotente,
    sem depender da conclusão da anterior. Edições preservam o histórico e
    substituem somente execuções futuras ainda pendentes.
  - Conclusão com lock pessimista, chave de idempotência, trilha de autoria e
    proteção contra registro em dobro. O próprio autor pode desfazer por até
    10 minutos. Escritas no endpoint legado `/events` retornam `410 Gone` para
    impedir duas fontes de verdade durante a transição.

- **Lembretes automáticos e confiáveis**
  - Um scheduler a cada 5 minutos estende o horizonte e detecta ocorrências no
    momento configurado, apenas
    enfileira o lembrete (outbox); um segundo scheduler, a cada 5 minutos,
    entrega de fato, com retry automático em caso de falha da API de e‑mail.
    Um lembrete nunca é enviado duas vezes (idempotência por evento) nem se
    perde silenciosamente se o processo reiniciar no meio do dia.
  - Servidor SMTP fake (MailHog) para desenvolvimento.
  - Todos os schedulers (lembretes, entrega, limpeza de segurança) usam
    ShedLock para rodar em uma única instância quando a aplicação estiver
    replicada.

- **Segurança e documentação**
  - Spring Security + JWT, senhas com BCrypt.
  - Redefinição de senha com token de uso único; a troca de senha e a
    emissão/invalidação de tokens são atômicas (`TransactionPort`).
  - OpenAPI/Swagger UI em `/swagger-ui.html`.
  - Console H2 em `/h2-console` para inspeção do banco em memória.

- **Observabilidade**
  - Spring Boot Actuator: `/actuator/health` (público), `/actuator/info` e
    `/actuator/metrics` (autenticados, como o resto da API).
  - Cada requisição recebe um `X-Request-Id` (gerado se o cliente não
    enviar um), propagado na resposta e no log, para correlacionar todas as
    linhas de uma mesma requisição sem precisar de tracing distribuído.

## Arquitetura

Estrutura hexagonal (ports & adapters) em sete módulos. `core` e `application`
são Kotlin puro — nenhuma dependência de Spring, Jakarta ou qualquer
framework — e essa fronteira é verificada em tempo de teste (testes de
arquitetura com [Konsist](https://docs.konsist.lemonappdev.com/), rodando via
`./gradlew check`), não apenas por convenção.

| Módulo                  | Papel                                                                    |
|-------------------------|---------------------------------------------------------------------------|
| **core**                | Entidades (Tutor, Pet, Event) e Value Objects (Email, Phone, etc.). Zero dependências. |
| **application**         | Ports de entrada/saída, comandos, resultados e os use cases (services). Kotlin puro — sem Spring. |
| **adapter-rest**        | Controllers REST, DTOs, `ApiExceptionHandler`, Spring Security (JWT, CORS), emissão de token, hash de senha. |
| **adapter-persistence** | Entidades JPA, repositórios Spring Data, mappers, adapters de persistência e migrações Flyway. Testes de integração sobem um Postgres real via Testcontainers (requer Docker). |
| **adapter-messaging**   | Cliente HTTP (WebClient) e adapters de e-mail (MailerSend).              |
| **adapter-storage**     | Adaptador S3 compatível com Railway Buckets e Cloudflare R2; URLs pré-assinadas curtas para leitura e escrita. |
| **bootstrap**           | Ponto de entrada Spring Boot: wiring manual dos use cases (`UseCaseWiring`), schedulers, configuração de ambiente (`application*.yml`, SSL, JVM). |

`core` e `application` não conhecem nenhum dos três adapters nem o
`bootstrap`; os adapters não se importam entre si; todo o grafo de beans dos
use cases é montado explicitamente em `UseCaseWiring` (bootstrap), já que os
services não carregam `@Service`/`@Value`/`@ConfigurationProperties`.

A configuração segura do bucket e do CORS está em
[`docs/object-storage.md`](docs/object-storage.md).
O desenho, a migração e a estratégia operacional do Ciclo 2 estão em
[`docs/cycle-2-care-plans.md`](docs/cycle-2-care-plans.md).

## Requisitos

- JDK 21+
- Kotlin 2.0 / Spring Boot 3.5 (versões centralizadas em `gradle/libs.versions.toml`)
- Docker (para MailHog e para os testes de `adapter-persistence`, que sobem
  um Postgres real via Testcontainers — `./gradlew check` falha sem Docker
  disponível)
- Gradle Wrapper (`./gradlew`)

## Executando o projeto

```bash
# Opcional: servidor de e‑mail fake
docker compose up -d mailhog

# Inicia a aplicação (módulo bootstrap) com H2 e swagger
./gradlew :bootstrap:bootRun
```

## Producao (sem Docker)

```bash
./gradlew :bootstrap:bootJar
scripts/run-prod.sh
```
