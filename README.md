# Pet Care Scheduler

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

- **Agendamento de eventos**
  - Registro, atualização, listagem e exclusão de eventos por pet ou por tutor.
  - Tipos de evento: `VACCINE`, `MEDICINE`, `DIARY`, `BREED`, `SERVICE`.
  - Suporte a recorrência (diária, semanal, mensal, anual) e alternância
    de status *PENDING/DONE*.

- **Lembretes automáticos**
  - Scheduler diário (`@Scheduled`) envia e‑mails para eventos previstos
    para o dia atual.
  - Servidor SMTP fake (MailHog) para desenvolvimento.

- **Segurança e documentação**
  - Spring Security + JWT, senhas com BCrypt.
  - OpenAPI/Swagger UI em `/swagger-ui.html`.
  - Console H2 em `/h2-console` para inspeção do banco em memória.

## Arquitetura

Estrutura hexagonal (ports & adapters) em seis módulos. `core` e `application`
são Kotlin puro — nenhuma dependência de Spring, Jakarta ou qualquer
framework — e essa fronteira é verificada em tempo de teste (testes de
arquitetura com [Konsist](https://docs.konsist.lemonappdev.com/), rodando via
`./gradlew check`), não apenas por convenção.

| Módulo                  | Papel                                                                    |
|-------------------------|---------------------------------------------------------------------------|
| **core**                | Entidades (Tutor, Pet, Event) e Value Objects (Email, Phone, etc.). Zero dependências. |
| **application**         | Ports de entrada/saída, comandos, resultados e os use cases (services). Kotlin puro — sem Spring. |
| **adapter-rest**        | Controllers REST, DTOs, `ApiExceptionHandler`, Spring Security (JWT, CORS), emissão de token, hash de senha. |
| **adapter-persistence** | Entidades JPA, repositórios Spring Data, mappers, adapters de persistência e migrações Flyway. |
| **adapter-messaging**   | Cliente HTTP (WebClient) e adapters de e-mail (MailerSend).              |
| **bootstrap**           | Ponto de entrada Spring Boot: wiring manual dos use cases (`UseCaseWiring`), schedulers, configuração de ambiente (`application*.yml`, SSL, JVM). |

`core` e `application` não conhecem nenhum dos três adapters nem o
`bootstrap`; os adapters não se importam entre si; todo o grafo de beans dos
use cases é montado explicitamente em `UseCaseWiring` (bootstrap), já que os
services não carregam `@Service`/`@Value`/`@ConfigurationProperties`.

## Requisitos

- JDK 21+
- Docker (para MailHog)
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
