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

Estrutura em quatro módulos seguindo DDD:

| Módulo         | Papel                                                             |
|----------------|-------------------------------------------------------------------|
| **core**       | Entidades (Tutor, Pet, Event) e Value Objects (Email, Phone, etc.)|
| **usecase**    | Portas de entrada/saída, comandos e resultados das regras de negócio |
| **application**| Services, REST controllers, scheduler e mapeadores (MapStruct)    |
| **infra**      | Spring Boot: JPA, mail, configuração, adaptadores e entidade JPA  |

## Requisitos

- JDK 21+
- Docker (para MailHog)
- Gradle Wrapper (`./gradlew`)

## Executando o projeto

```bash
# Opcional: servidor de e‑mail fake
docker compose up -d mailhog

# Inicia a aplicação (módulo infra) com H2 e swagger
./gradlew :infra:bootRun
