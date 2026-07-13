# Ciclo 4 — cuidado compartilhado com responsabilidade

## Objetivo e decisões arquiteturais

O compartilhamento é modelado como uma fronteira explícita de autorização,
`household`, e não como uma lista informal de pessoas em um pet. Pet, plano,
ocorrência, registro clínico, medição e mídia compartilhada carregam
`household_id`; toda leitura e escrita resolve primeiro o contexto autenticado.

O ator continua sendo o tutor do JWT. O cabeçalho opcional `X-Household-Id`
escolhe uma das famílias das quais ele é membro; sem ele, o backend usa a
família padrão do tutor. Um UUID inválido, uma família externa ou um recurso de
outro contexto nunca faz fallback silencioso e é respondido como não encontrado.

Cadastros existentes e novos recebem automaticamente uma família pessoal com
papel `OWNER`. A migração V16 faz o backfill determinístico dos dados atuais e
preserva as referências de autoria existentes.

## Papéis e permissões

| Papel | Visualizar | Concluir cuidados | Registrar saúde | Gerir pets, planos e membros |
|---|---:|---:|---:|---:|
| `OWNER` | sim | sim | sim | sim |
| `CAREGIVER` | sim | sim | sim | não |
| `VIEWER` | sim | não | não | não |

As permissões vivem no core e são verificadas novamente pelos casos de uso. A
interface apenas reflete a autorização para evitar ações frustradas. O último
proprietário não pode ser removido nem rebaixado, e convites nunca concedem
`OWNER` diretamente.

## Convites seguros

- token aleatório de 256 bits, enviado uma única vez por e-mail;
- apenas SHA-256 do token é persistido;
- convite vinculado ao e-mail normalizado do destinatário;
- validade de sete dias, uso único e revogação explícita;
- somente um convite ativo por família e e-mail; uma nova tentativa revoga o
  anterior;
- limite de 10 convites por hora por IP, ator e família;
- falha do provedor de e-mail é devolvida ao proprietário. O convite salvo pode
  ser revogado, e repetir o envio invalida seu token antes de criar outro.

O link usa `app.frontend.base-url` (`FRONTEND_BASE_URL`) e direciona para
`/invite?token=...`. O frontend exige autenticação antes da aceitação; o backend
confirma que o e-mail da conta é exatamente o convidado.

## Responsabilidade, auditoria e troca de turno

Planos e ocorrências possuem um responsável pertencente à mesma família. A
troca de responsável atualiza somente ocorrências futuras pendentes e registra
atividade. Confirmações guardam quem realizou e quando; desfazer preserva a
trilha append-only existente.

`household_activity` reúne entrada, mudança de papel, remoção, atribuição,
conclusão, reabertura, registro de saúde, passagem de turno e escalonamento.
`household_handoff` preserva o texto e o remetente, com destinatário opcional.
Esses registros tornam o compartilhamento operacional: a próxima pessoa vê o
que aconteceu e o que ainda precisa assumir.

## Cuidado crítico e entrega confiável

Um proprietário pode marcar um plano como crítico, escolher atraso entre 15
minutos e sete dias e indicar um proprietário que receberá o alerta. Esses
valores são copiados para cada ocorrência, preservando a decisão histórica.

Quando uma ocorrência continua `SCHEDULED` após `due_at + delay`, a
materialização transacional cria uma mensagem em `care_escalation_outbox`. A
constraint única por ocorrência impede duplicidade. Um relay separado:

1. lê lotes limitados de mensagens pendentes;
2. verifica novamente se o cuidado ainda está pendente;
3. envia o e-mail ou incrementa a tentativa;
4. marca a mensagem como entregue e grava atividade somente após sucesso.

O relay tenta no máximo cinco vezes, em lotes de 50, roda a cada cinco minutos
e usa ShedLock para haver somente um executor quando o backend estiver
replicado. Reiniciar a aplicação entre persistência e envio não perde a
mensagem nem envia o mesmo alerta duas vezes.

## API

- `GET /api/v1/households`
- `GET /api/v1/households/current`
- `PUT /api/v1/households/{id}/default`
- `PATCH /api/v1/households/{id}`
- `POST|DELETE /api/v1/households/current/invitations[/{id}]`
- `POST /api/v1/households/invitations/accept`
- `PATCH|DELETE /api/v1/households/current/members/{id}`
- `POST /api/v1/households/current/handoffs`
- `PATCH /api/v1/care-occurrences/{id}/responsible`

As APIs compartilhadas de pets, cuidados, saúde, dashboard e mídia aceitam
`X-Household-Id`. Fotos públicas continuam sem autenticação somente na rota de
conteúdo promovido; anexos clínicos permanecem privados e exigem o contexto.

## Implantação e operação

1. gere backup e valide a restauração;
2. configure `FRONTEND_BASE_URL`, MailerSend e timezone no ambiente de preview;
3. implante o backend e aguarde o Flyway concluir a V16;
4. valide que cada tutor antigo vê sua família e seus dados anteriores;
5. teste convite com duas contas, papéis, passagem de turno e isolamento entre
   famílias;
6. crie um cuidado crítico curto em preview e confirme outbox, e-mail e
   atividade recente;
7. implante o frontend e monitore respostas `403`, `404`, `409`, `429` e falhas
   do provedor de e-mail.

Monitore mensagens de escalonamento com `sent_at IS NULL`, `attempts >= 5` e a
idade de `created_at`. Um crescimento contínuo indica indisponibilidade de
e-mail ou do scheduler. A V16 é expansiva; rollback do binário preserva os
dados, mas escritas compartilhadas devem ser suspensas enquanto uma versão
antiga não conhece a nova fronteira de autorização.
