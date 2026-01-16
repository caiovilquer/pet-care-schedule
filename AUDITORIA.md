# Auditoria do Projeto Pet Care Scheduler (revisada)

Data: 2025-02-14

## Escopo
- Revisao de seguranca, confiabilidade, performance, consistencia de API e manutencao.
- Baseado no estado atual do codigo.

## Resumo executivo
- Os pontos criticos identificados anteriormente foram tratados (JWT, rate limiting, reset de senha, scheduler, integridade).
- Permanecem riscos operacionais de escala e manutencao, com foco em cache JWT, rate limiting distribuidos e observabilidade.

## Achados de seguranca
**Medio**
- Cache de `passwordChangedAt` reduz carga, mas ainda ha acesso ao repositorio em casos de cache miss. Em alto volume, pode exigir cache externo ou revogacao dedicada. Arquivo: `application/src/main/kotlin/dev/vilquer/petcarescheduler/application/config/SecurityConfig.kt`.

**Baixo**
- Rate limiting agora persiste no banco, mas sem backoff progressivo. Em ataques massivos, pode pressionar o banco. Arquivo: `application/src/main/kotlin/dev/vilquer/petcarescheduler/application/service/RateLimiterService.kt`.

## Falhas funcionais e consistencias
**Baixo**
- Mensagens de conflito agora usam SQLState (23505). Em bancos que nao retornam SQLState, a mensagem cai no generico, reduzindo UX. Arquivo: `application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/ApiExceptionHandler.kt`.
- `PasswordResetController.validate` usa rate limit por IP apenas; usuarios atras de NAT podem ter throttling indevido. Arquivo: `application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/PasswordResetController.kt`.

## Performance e escalabilidade
**Baixo**
- Cleanup de rate limit e tokens foi agendado, mas o job roda em todas as instancias; em clusters pode haver concorrencia de deletes. Arquivo: `application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/scheduler/SecurityMaintenanceScheduler.kt`.

## Integridade de dados
- Flyway habilitado em dev e `ddl-auto` configurado para `validate`, reduzindo divergencia de schema. Sem novos achados.

## Overengineering
- Estrutura em multiplos modulos e mapeadores ainda adiciona complexidade para o tamanho atual do dominio.

## Testes e cobertura (gaps)
- Faltam testes de integracao para o cache de `passwordChangedAt` e invalidação de JWT.
- Faltam testes de concorrencia do rate limiter persistente.
- Falta teste para o job de limpeza de tokens e rate limit.

## Recomendacoes priorizadas
1) Avaliar cache externo (ex.: Redis) para `passwordChangedAt` em producao.
2) Adicionar backoff progressivo e/ou bloqueio temporario para rate limiting.
3) Garantir apenas uma instancia executando limpeza (leader election ou lock distribuidos).
4) Adicionar testes de integracao para JWT invalidation e scheduler de limpeza.
