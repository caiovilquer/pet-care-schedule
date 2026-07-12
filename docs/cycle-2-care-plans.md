# Ciclo 2 — planos de cuidado e ocorrências

## Decisão arquitetural

O modelo antigo misturava a definição recorrente e a execução concreta em
`event`. Isso fazia a próxima dose depender da conclusão da anterior e tornava
edição, autoria e histórico ambíguos. O Ciclo 2 separa:

- `care_plan`: regra do cuidado, recorrência, responsável, orientações,
  lembrete, revisão do agendamento e estado ativo;
- `care_occurrence`: execução imutavelmente vinculada a uma revisão, com data,
  snapshot do cuidado, estado e auditoria da conclusão;
- `care_occurrence_action`: trilha append-only de `COMPLETE`, `UNDO` e futuros
  atos, identificada por uma chave idempotente fornecida pelo cliente;
- `care_reminder_outbox`: entrega confiável e deduplicada por ocorrência.

Core e application continuam sem Spring. REST, JPA, mensageria e schedulers
implementam ports próprias e o wiring permanece explícito no bootstrap.

## Garantias de segurança

- UUIDs de ocorrências são determinísticos por plano, revisão e sequência.
- Uma constraint única reforça `(plan_id, schedule_revision, sequence_number)`.
- A materialização usa horizonte móvel de 90 dias e limite defensivo de 500
  ocorrências por plano/horizonte.
- Conclusão e desfazer adquirem lock pessimista da ocorrência. Sob concorrência,
  uma administração vence e as demais recebem conflito; a mesma chave recebe a
  mesma resposta mesmo quando aguardou o lock.
- A chave de requisição é globalmente única e registrada junto a autor, estado
  anterior, estado novo e instante.
- Desfazer só é aceito para o mesmo tutor, em até 10 minutos.
- Editar incrementa `schedule_revision`, cancela somente ocorrências futuras
  pendentes e nunca reescreve uma conclusão. Encerrar faz o mesmo para todas as
  pendentes.
- Todas as consultas são filtradas pelo tutor autenticado; acesso cruzado é
  respondido como não encontrado.

## Migração e compatibilidade

A migração V14 é expansiva: cria as novas tabelas, converte cada evento antigo
em plano + ocorrência, transfere o outbox pendente e mantém `event` para leitura
e rollback operacional. Escritas HTTP em `/api/v1/events` retornam `410 Gone`:
aceitá-las depois da migração criaria registros invisíveis na nova agenda.

Sequência recomendada de implantação:

1. gerar backup e validar restore;
2. implantar o backend e aguardar Flyway + healthcheck;
3. implantar o frontend do Ciclo 2;
4. acompanhar conflitos, atraso do outbox e duração do scheduler;
5. após a janela de retenção definida pelo produto, remover API e tabelas
   legadas em uma migração contrativa separada.

O rollback do binário é possível enquanto o schema expandido estiver presente,
mas novas escritas devem permanecer suspensas durante um rollback para evitar
divergência entre os dois modelos.

## API nova

- `POST|GET /api/v1/care-plans`
- `GET|PUT|DELETE /api/v1/care-plans/{id}`
- `GET /api/v1/care-occurrences` — período, pet, tipo, status e paginação
- `GET /api/v1/care-occurrences/today`
- `POST /api/v1/care-occurrences/{id}/complete`
- `POST /api/v1/care-occurrences/{id}/undo`

## Operação

O scheduler de detecção roda a cada cinco minutos com ShedLock e gera o
horizonte idempotentemente antes de enfileirar lembretes vencidos. O relay roda
separadamente, também com ShedLock, tenta novamente falhas transitórias e ignora
ocorrências já concluídas ou canceladas.
