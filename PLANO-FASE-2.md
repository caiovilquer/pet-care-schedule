# Fase 2 — Plano de implementação: DDD tático (agregados, invariantes, comportamento, erros tipados)

Data: 2026-07-05

## 1. Objetivo e escopo

Enriquecer o domínio conforme a Proposta A original: agregados que referenciam
uns aos outros por ID (não por composição), invariantes que tornam estado
inválido irrepresentável, comportamento de negócio movido para dentro do
agregado, e erros HTTP corretos (404 real, não 400 disfarçado).

**Entra nesta fase**
- Erros tipados: `NotFoundException` substitui `IllegalArgumentException` nos
  pontos onde o significado real é "não encontrado" — conserta o bug em que
  `GET /pets/999` respondia 400 em vez de 404.
- Linguagem ubíqua: `Pet.specie`→`species`, `Pet.race`→`breed` (elimina a
  colisão de vocabulário com `EventType.BREED` e corrige o inglês incorreto de
  "specie", que é termo de mercado financeiro, não biológico).
- Agregados por referência: `Tutor.pets` e `Pet.events` saem das entidades de
  domínio; as listas viram *read models* montados no service, consultando o
  repositório do filho por FK.
- Invariantes nos construtores: nome/species não-vazios em `Tutor`/`Pet`.
- Comportamento no agregado: `Event.complete()` gera a próxima ocorrência
  quando há recorrência ativa — conserta o bug em que marcar um evento
  recorrente como concluído nunca reagendava nada (`Recurrence.nextOccurrence`/
  `hasNext` existiam mas não tinham nenhum chamador).

**Fica fora (fases seguintes)**
- Modelo de ocorrências materializadas para lembretes (tabela dedicada,
  idempotência de envio) — Fase 3.
- `@Transactional` no boundary do use case, domain events/outbox — Fase 3.
- Sealed hierarchy completa de erros de domínio (`Result`/`Either` no lugar de
  exceptions) — avaliar em fase futura; aqui mantemos exceptions tipadas para
  não introduzir um segundo paradigma de tratamento de erro na mesma fase.

## 2. Decisões de design

| # | Decisão | Justificativa |
|---|---------|----------------|
| D1 | `NotFoundException` nova em `application.exception`, não reaproveitar `NoSuchElementException` | O código já tem um padrão estabelecido de uma exception própria por status HTTP (`ForbiddenException`, `ConflictException`, `RateLimitException`, `InvalidCredentialsException`); manter a simetria é mais legível que emprestar uma exception de iteração de coleção para semântica de domínio. O handler `NoSuchElementException→404`, hoje morto, é removido. |
| D2 | `PasswordResetService` mantém `IllegalArgumentException` para `invalid_token`/`expired_token` | Não é "entidade não encontrada por ID" — é um token malformado/expirado, 400 continua sendo a resposta correta. Escopo do fix é estritamente os lookups de Tutor/Pet/Event. |
| D3 | Rename de coluna via migração, não `@Column(name=...)` de compatibilidade | Manter o nome antigo da coluna via anotação explícita perpetuaria a inconsistência no banco; a migração (`ALTER TABLE ... RENAME COLUMN`) é reversível e documenta a mudança no histórico do schema. |
| D4 | Remoção das coleções `pets`/`events` das entidades de domínio, **mantendo** a FK `ON DELETE CASCADE` já existente (V5) como único mecanismo de cascade | O cascade de exclusão no banco já cobre a limpeza; o cascade do JPA (`@OneToMany(cascade=ALL, orphanRemoval=true)`) era redundante e é exatamente o que acopla a entidade a um grafo profundo. Remover as coleções da entidade JPA também (não só do domínio) é seguro porque a exclusão via `deleteById` continua disparando o `ON DELETE CASCADE` do Postgres/H2 independente de mapeamento JPA. |
| D5 | `PetRepositoryPort` ganha `findAllByTutor(tutorId): List<Pet>` (sem paginação), distinto de `listByTutor(tutorId, page, size)` | O detalhe de um tutor precisa da lista completa de pets para montar `TutorDetailResult`; reaproveitar o método paginado com `size=Int.MAX_VALUE` seria enganoso sobre a semântica de paginação. |
| D6 | `Event` ganha campo `occurrenceCount: Int = 0`; `complete()` retorna `EventCompletion(completed, next)` | `Recurrence.hasNext(executeCount, lastDate)` precisa saber quantas ocorrências já aconteceram — hoje esse número não é rastreado em lugar nenhum. Guardar o contador no próprio evento é o menor estado adicional que faz `hasNext`/`nextOccurrence` finalmente terem um chamador, sem entrar no modelo de ocorrências materializadas (isso é Fase 3). |
| D7 | Toggle preserva semântica bidirecional: PENDING→DONE chama `complete()` (pode gerar próxima ocorrência); DONE→PENDING continua sendo `markPending()` simples | Desfazer uma conclusão não deve tentar "desfazer" a ocorrência já criada — isso seria destrutivo e difícil de reverter corretamente. A próxima ocorrência, uma vez criada, é um evento independente. |
| D8 | `core` ganha suíte de testes própria (hoje zero) | Invariantes e `Event.complete()` são regra de negócio pura; testá-las no módulo que as declara (sem fakes de repositório) é o lugar certo, e formaliza `core` como testável isoladamente. |

## 3. Sequência de execução (PRs pequenos, build verde em cada um)

### PR 2-A — Erros tipados (404 real)
- `NotFoundException` em `application.exception`.
- Substituir todos os `throw IllegalArgumentException(".. not found")` em
  `PetAppService`, `TutorAppService`, `EventAppService` (7 pontos).
- `ApiExceptionHandler`: trocar o handler morto de `NoSuchElementException`
  por `NotFoundException` → 404.
- Atualizar `ExceptionHandlerTest` e `PetAppServiceTest` (o teste
  `createPet should throw when tutor is missing` esperava
  `IllegalArgumentException`).
- Adicionar testes de not-found que hoje faltam em `TutorAppServiceTest`/
  `EventAppServiceTest` (nenhum serviço tinha esse caso coberto).
- Estender o smoke E2E: `GET /pets/{id-inexistente}` deve responder 404.

### PR 2-B — Linguagem ubíqua: specie/race → species/breed
- `core`: `Pet.specie`→`species`, `Pet.race`→`breed`.
- `usecase` (dentro de `application`): `CreatePetCommand`, `UpdatePetCommand`,
  `PetDetailResult`, `PetSummary`.
- `application`: `ResultMapper` (`Pet.toSummary`/`toDetailResult`),
  `PetAppService`.
- `adapter-rest`: `PetDtoMapper.CreateRequest`/`UpdateRequest` (os campos JSON
  públicos passam a ser `species`/`breed`).
- `adapter-persistence`: `PetJpa`, `PetMapper`; migração `V7__rename_pet_specie_and_race.sql`
  (`common`, já que `ALTER TABLE ... RENAME COLUMN` é sintaxe idêntica em
  Postgres e H2 — verificar antes de assumir).
- Todos os testes que usam `specie`/`race` (Kotlin e JSON): `PetControllerTest`,
  `PetAppServiceTest`, `PetMapperTest`, `TutorMapperTest`,
  `TutorMapperIntegrationTest`, `ResultMappingTest`, `CommandsTest`,
  `SmokeE2ETest`.
- **Mudança de contrato de API:** o corpo de `POST/PUT /pets` passa a usar
  `species`/`breed` em vez de `specie`/`race`. Documentar no README.

### PR 2-C — Agregados por referência (remover composição)
- `core`: `Tutor` perde `pets`; `Pet` perde `events`.
- `adapter-persistence`: `TutorJpa` perde `pets` (+ mapeamento `@OneToMany`);
  `PetJpa` perde `events` (+ mapeamento); `TutorMapper`/`PetMapper` perdem
  `mapPets`/`mapEvents`.
- `usecase`: `PetRepositoryPort.findAllByTutor(tutorId): List<Pet>` (novo).
- `application`: `ResultMapper.Pet.toDetailResult(events: List<Event>)` e
  `Tutor.toDetailResult(pets: List<Pet>)` passam a receber a lista como
  parâmetro; `PetAppService` ganha dependência de `EventRepositoryPort`;
  `TutorAppService` ganha dependência de `PetRepositoryPort`; ambos os
  `get()` compõem o resultado com uma consulta adicional ao filho.
- Testes: `TutorMapperTest`/`IntegrationTest`, `PetMapperTest`,
  `ResultMappingTest`, `PetAppServiceTest`, `TutorAppServiceTest`
  (construtores dos fakes/`InMemory*Repo` não mudam de contrato).

### PR 2-D — Invariantes de domínio
- `Tutor`: `require(firstName.isNotBlank())`.
- `Pet`: `require(name.isNotBlank())`, `require(species.isNotBlank())`.
- Novo módulo de teste `core/src/test`: `TutorTest`, `PetTest`, `EventTest`
  (este último preparando o terreno para o PR 2-E).
- `core/build.gradle.kts` já declara JUnit (nunca teve arquivo de teste
  algum) — confirmar antes de mexer.

### PR 2-E — `Event.complete()` e a próxima ocorrência
- `core`: `Event` ganha `occurrenceCount: Int = 0`; novo `EventCompletion
  (completed: Event, next: Event?)`; `Event.complete()` usa
  `recurrence.nextOccurrence`/`hasNext` (finalmente com um chamador).
- Migração `V8__add_occurrence_count_to_event.sql` (`common`).
- `adapter-persistence`: `EventJpa`/`EventMapper` ganham o campo.
- `application`: `EventAppService.execute(ToggleEventCommand, ...)` —
  transição PENDING→DONE chama `complete()` e salva `completed` e (se
  existir) `next`; transição DONE→PENDING continua `markPending()`.
- Testes: `EventTest` (core, casos com/sem recorrência, com `repetitions`
  esgotado, com `finalDate` vencida), `EventAppServiceTest` (toggle gera
  próxima ocorrência quando aplicável), `EventMapperTest`/
  `EventMapperIntegrationTest`.
- Smoke E2E: opcional — o fluxo já cobre toggle; adicionar asserção de que
  um evento recorrente gera uma segunda linha na listagem após o toggle.

## 4. Riscos e mitigações

| Risco | Mitigação |
|-------|-----------|
| Rename de coluna (PR 2-B) quebra `ddl-auto=validate` em prod se a migração não rodar antes do deploy | Migração é `common` (roda em ambos os bancos); testar via `adapter-persistence` + smoke E2E antes de commitar |
| `PetAppService`/`TutorAppService` ganhando novas dependências de construtor quebra `UseCaseWiring` (bootstrap) | `UseCaseWiring` é `@Bean` explícito — o compilador aponta o construtor errado imediatamente; ajustar no mesmo PR |
| `Event.complete()` muda comportamento observável do endpoint de toggle | Documentado como fix de bug (não regressão) na seção 1; coberto por teste novo em `EventAppServiceTest` |
| Konsist (Fase 1) barra novos imports por engano | Rodar `./gradlew check` a cada PR, não só `build` |

## 5. Definition of Done

- [x] `GET /pets/{id-inexistente}` e equivalentes para tutor/evento respondem 404 (smoke E2E)
- [x] `POST /pets` aceita `species`/`breed`; nenhum resquício de `specie`/`race` no código ou no schema
- [x] `Tutor`/`Pet` não têm mais `pets`/`events` embutidos; `TutorDetailResult`/`PetDetailResult` continuam corretos via consulta explícita
- [x] `core` tem suíte de testes própria cobrindo invariantes e `Event.complete()` (17 testes: Tutor/Pet/Event/Recurrence)
- [x] Marcar um evento recorrente como concluído cria a próxima ocorrência quando `Recurrence.hasNext` permite
- [x] `./gradlew build` e `./gradlew check` verdes a cada PR
- [x] README atualizado onde description de API mudou (species/breed, recorrência)

**Status: Fase 2 concluída (2026-07-05).** Cinco PRs (2-A a 2-E) implementados
e commitados sequencialmente, build e `check` verdes a cada etapa. 65 testes
passando no total (core: 17, application: 27, adapter-rest: 9,
adapter-persistence: 14, bootstrap: 11).
