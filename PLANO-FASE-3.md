# Fase 3 — Plano de implementação: consistência e mensageria

Data: 2026-07-05

## 1. Objetivo e escopo

Fechar os três problemas de confiabilidade operacional identificados na
Proposta A original e na AUDITORIA.md: escrita não-atômica no reset de senha,
lembretes que se perdem silenciosamente se o processo cair ou a API de
e-mail falhar, e schedulers que rodariam duplicados num cluster.

**Entra nesta fase**
- `TransactionPort`: fronteira transacional explícita, sem reintroduzir
  Spring no módulo `application`.
- `PasswordResetService.reset()` (e a escrita dupla de token em
  `requestReset()`) atômicos.
- Outbox de lembretes: a detecção diária (`sendRemindersForToday`) só
  enfileira; um relay separado entrega com retry, e um lembrete nunca é
  perdido nem duplicado mesmo se o processo reiniciar no meio do dia.
- ShedLock nos três schedulers, para segurança em cluster (múltiplas
  instâncias do bootstrap).
- Revisão do estado de "CQRS leve nas leituras": verificar se a Fase 2 já
  resolveu isso (spoiler: já resolveu) e documentar em vez de inventar
  trabalho nesta frente.

**Fica fora**
- Domain events genéricos (`EventRegistered`/`EventCompleted` como tipos
  publicados por um barramento). Nada no app hoje precisa reagir a esses
  eventos além do próprio scheduler de lembretes, que já é resolvido pelo
  outbox específico abaixo — um barramento de eventos genérico seria
  abstração sem consumidor, o oposto do que essas instruções pedem.
- Sealed hierarchy de erros de domínio / `Result`/`Either` no lugar de
  exceptions — descartado também na Fase 2 pelo mesmo motivo (não misturar
  dois paradigmas de erro na mesma base).

## 2. Decisões de design

| # | Decisão | Justificativa |
|---|---------|----------------|
| D1 | `TransactionPort` como driven port (`fun <T> execute(block: () -> T): T`), implementado em `adapter-persistence` sobre `TransactionTemplate` | A Fase 1 tornou `application` livre de Spring de propósito (`UseCaseWiring` é o único lugar que conhece Spring). Adicionar `@Transactional` de volta ao service reverteria essa decisão; um port mantém a fronteira. |
| D2 | Outbox específico para lembretes (`ReminderOutboxPort`), não um barramento de domain events genérico | O único consumidor de "evento aconteceu" hoje é o e-mail de lembrete. Modelar um barramento pub/sub para um único assinante é indireção sem benefício — construiríamos para um requisito hipotético, não para o que o app faz. |
| D3 | Detecção (`sendRemindersForToday`) e entrega (`dispatchPendingReminders`) são dois use cases/schedulers separados | Hoje a detecção já bloqueia numa chamada HTTP síncrona (`WebClient.block()`) dentro do laço da varredura diária; se a API de e-mail estiver lenta, a varredura inteira trava. Separar detecção (grava no outbox, rápido) de entrega (lê o outbox, tenta enviar, tem retry) resolve isso sem precisar reescrever o adapter para reativo. |
| D4 | Idempotência via `UNIQUE(event_id)` na tabela do outbox, não um novo campo em `Event` | A Fase 2 já modela cada ocorrência recorrente como um novo `Event` (linha própria); um evento só precisa de **um** lembrete na vida dele. Uma constraint única no outbox é suficiente e não exige tocar na entidade `Event` de novo. |
| D5 | `NotificationPort.sendEventReminder` passa a devolver `Boolean` (sucesso/falha) em vez de engolir a exceção silenciosamente | O adapter atual (`EmailNotificationAdapter`) já captura toda exceção e só loga — do ponto de vista de quem chama, a função nunca falha. O relay precisa saber se deve tentar de novo; sem um retorno, não há como decidir. |
| D6 | Relay roda a cada poucos minutos (não diariamente) | Um lembrete que falhou às 8h porque a API de e-mail estava fora não deve esperar até o dia seguinte para ser tentado de novo — isso violaria o objetivo de confiabilidade desta fase. |
| D7 | ShedLock com `shedlock-provider-jdbc-template` (usa o datasource já existente) | Não introduz infraestrutura nova (Redis, Zookeeper); a tabela `shedlock` é só mais uma migração Flyway, coerente com o resto do projeto. |
| D8 | "CQRS leve nas leituras" não gera código novo nesta fase | A Fase 2 (PR 2-C) já eliminou o carregamento de grafos (`Tutor.pets`/`Pet.events`) e todas as listagens já usam projeções (`toSummary()`) sem filhos aninhados. Adicionar algo aqui só para preencher a lista do plano original seria trabalho artificial. |

## 3. Sequência de execução

### PR 3-A — `TransactionPort` e reset de senha atômico
- Novo port `usecase.contract.drivenports.TransactionPort`.
- `SpringTransactionPort` em `adapter-persistence` (constrói seu próprio
  `TransactionTemplate` a partir do `PlatformTransactionManager` injetado —
  sem precisar de um `@Bean` extra).
- `PasswordResetService`: `reset()` (update senha + bump passwordChangedAt +
  markUsed) e a dupla escrita de token em `requestReset()` (invalidar +
  criar) passam a rodar dentro de `transactionPort.execute { }`. O envio do
  e-mail continua **fora** da transação (chamada de rede não deve segurar
  uma conexão de banco aberta).
- `UseCaseWiring` atualizado com a nova dependência.
- Novo `PasswordResetServiceTest` (não existia nenhum teste unitário deste
  service) usando fakes, incluindo `FakeTransactionPort` que só executa o
  bloco diretamente.

### PR 3-B — Outbox de lembretes
- `ReminderOutboxPort` + `ReminderOutboxMessage` (driven port, `application`).
- `NotificationPort.sendEventReminder` retorna `Boolean`.
- `EventAppService` troca a dependência `NotificationPort` por
  `ReminderOutboxPort`; `sendRemindersForToday()` enfileira em vez de
  enviar diretamente.
- Novo `ReminderRelayService` (`DispatchPendingRemindersUseCase`): lê
  mensagens pendentes (não enviadas, abaixo do limite de tentativas),
  chama `NotificationPort`, marca sucesso ou incrementa tentativas.
- `adapter-persistence`: `ReminderOutboxJpa`, `ReminderOutboxJpaRepository`,
  `ReminderOutboxJpaAdapter` (idempotência reforçada por `UNIQUE(event_id)`
  + captura de `DataIntegrityViolationException` na corrida rara entre
  `existsByEventId` e `save`); migração `V9__create_reminder_outbox.sql`.
- `bootstrap`: novo `ReminderRelayScheduler` (a cada poucos minutos).
- Testes: `EventAppServiceTest` (enfileira em vez de notificar),
  `ReminderRelayServiceTest` (novo — sucesso marca enviado, falha
  incrementa tentativas, mensagem esgotada não é retornada de novo),
  `FakeNotifier`/`FakeReminderOutboxPort` em `TestDoubles.kt`.

### PR 3-C — ShedLock
- Dependências `shedlock-spring` + `shedlock-provider-jdbc-template` em
  `bootstrap`.
- Migração `V10__create_shedlock.sql`.
- `SchedulerLockConfig` (`@EnableSchedulerLock`, bean `LockProvider` via
  JDBC).
- `@SchedulerLock` nos três métodos agendados
  (`EventReminderScheduler`, `SecurityMaintenanceScheduler`,
  `ReminderRelayScheduler`), com `lockAtMostFor`/`lockAtLeastFor` coerentes
  com o intervalo de cada um.

### PR 3-D — Verificação de CQRS leve e fechamento
- Confirmar (sem alterar produção) que `listByTutor`/`listEvents` já
  devolvem projeções (`toSummary()`) sem carregar grafos — checar contra o
  que a Fase 2 mudou.
- Atualizar README (comportamento de retry de lembretes, ShedLock) e
  marcar o Definition of Done.

## 4. Riscos e mitigações

| Risco | Mitigação |
|-------|-----------|
| `TransactionTemplate` sem bean de `PlatformTransactionManager` disponível no escopo do teste (`@DataJpaTest`) | `SpringTransactionPort` só é exercitado pelo smoke E2E (contexto completo); testes unitários usam `FakeTransactionPort` |
| Corrida entre duas execuções do scheduler de detecção antes do ShedLock existir (PR 3-B roda antes de 3-C) | `UNIQUE(event_id)` no outbox torna a janela de risco inofensiva mesmo sem lock distribuído |
| Relay reprocessando uma mensagem indefinidamente se a API de e-mail cair por dias | `maxAttempts` limita tentativas; mensagens esgotadas somem da consulta (ficam no banco para inspeção manual, não bloqueiam o relay) |
| Versão exata do ShedLock não resolvida de antemão | Mesma técnica usada para o Konsist na Fase 1: tentar uma versão razoável, deixar o Gradle confirmar/falhar, ajustar |

## 5. Definition of Done

- [x] `PasswordResetService.reset()` e a dupla escrita de token em
      `requestReset()` rodam dentro de uma transação (`TransactionPort` +
      `SpringTransactionPort`, verificado ao vivo via smoke test contra o
      `PlatformTransactionManager` real, não só com fakes)
- [x] Lembrete de evento nunca enviado duas vezes (`UNIQUE(event_id)`) nem
      perdido silenciosamente (relay reprocessa falhas) — verificado ao vivo
      contra o H2 real: duas varreduras no mesmo dia não duplicam a
      mensagem, e uma falha real de entrega (API de e-mail indisponível no
      ambiente de teste) incrementa `attempts` sem marcar como enviada
- [x] Detecção diária não bloqueia mais numa chamada de rede síncrona por
      lembrete (`sendRemindersForToday` só enfileira; `ReminderRelayService`
      entrega em um scheduler separado)
- [x] `@SchedulerLock` protege os três schedulers contra execução
      concorrente em cluster — aplicação subida de verdade (`bootRun`) sem
      erro de wiring do `LockProvider`/tabela `shedlock`
- [x] "CQRS leve nas leituras" confirmado como já satisfeito pela Fase 2
      (PR 2-C): `listByTutor`/`listEvents` usam projeções (`toSummary()`)
      sem grafo aninhado; nenhum código novo necessário nesta frente
- [x] `./gradlew build` e `./gradlew check` verdes a cada PR
- [x] README e este plano atualizados

**Status: Fase 3 concluída (2026-07-05).** Quatro PRs (3-A a 3-D)
implementados e commitados sequencialmente. 91 testes passando no total
(core: 17, application: 38, adapter-rest: 9, adapter-persistence: 14,
bootstrap: 13), incluindo dois bugs reais encontrados e corrigidos durante
a verificação ao vivo com o smoke test (não apenas testes com fakes):
`MailSenderAdapter.sendResetLink` deixava exceção de rede vazar para a
requisição HTTP (virava 401 sem relação com o problema real), e o próprio
teste de outbox tinha uma data fixada ao meio-dia que falhava
`@FutureOrPresent` em execuções à tarde.
