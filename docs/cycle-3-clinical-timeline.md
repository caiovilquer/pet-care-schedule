# Ciclo 3 — linha do tempo clínica

## Escopo e separação de responsabilidades

O histórico clínico registra o que já aconteceu; planos e ocorrências continuam
representando o que deve acontecer. Essa separação evita transformar uma ação
desfeita na agenda em um fato clínico permanente e mantém as duas fontes de
verdade com semânticas claras.

- `health_record`: vacina, medicamento, consulta, exame, sintoma ou cuidado
  diário, com instante, autoria e campos progressivos de produto, dose, lote,
  profissional, clínica, custo e moeda;
- `health_measurement`: séries de peso, temperatura e escore corporal, sempre
  em uma unidade canônica;
- `health_record_attachment`: vínculo entre um registro e um objeto privado;
- `media_asset.health_record_id`: escopo imutável usado na autorização e na
  organização do bucket.

A migração V15 é expansiva e forward-only. Nenhuma tabela do Ciclo 2 é
removida ou reinterpretada.

## Garantias de segurança e integridade

- todas as consultas e mutações são filtradas pelo tutor autenticado e pelo pet;
- acessos cruzados a registros e anexos retornam `404`, reduzindo enumeração;
- edições e exclusões exigem `version`; um lock pessimista serializa a leitura e
  a comparação, e uma versão antiga recebe `409`;
- datas futuras têm tolerância máxima de cinco minutos;
- tipos, unidades, faixas fisiológicas defensivas, tamanhos, precisão monetária
  e relevância de campos são validados no domínio e reforçados no banco;
- consultas têm períodos, paginação e limites máximos para evitar respostas sem
  limite e gráficos excessivos;
- cada registro aceita no máximo cinco anexos JPG, PNG ou PDF de até 10 MB;
- o backend confirma tamanho, SHA-256, assinatura do arquivo e decodificação de
  imagens antes da promoção; PDFs precisam de cabeçalho e trailer válidos;
- documentos nunca passam pela rota pública de fotos. A API autenticada emite
  uma URL GET pré-assinada de 15 minutos com `Content-Disposition: attachment`,
  tipo `application/octet-stream`, `no-store`, `nosniff` e `no-referrer`;
- uploads incompletos e exclusões usam os estados e a limpeza retry-safe da
  infraestrutura do Ciclo 1.

O bucket deve continuar privado. A validação estrutural reduz arquivos forjados,
mas não substitui antimalware. Se o produto aceitar documentos de terceiros em
grande escala, adicione uma porta assíncrona de varredura antes de promover o
objeto de quarentena.

## API

- `POST|GET /api/v1/pets/{petId}/health-records`
- `GET|PUT|DELETE /api/v1/health-records/{id}`
- `POST|GET /api/v1/pets/{petId}/health-measurements`
- `PUT|DELETE /api/v1/health-measurements/{id}`
- `GET /api/v1/health-attachments/{mediaId}/download-url`
- upload e remoção continuam em `/api/v1/media` com o propósito
  `HEALTH_ATTACHMENT` e `targetUuid` igual ao registro clínico.

## Implantação

1. gere backup e valide restauração;
2. implante o backend e aguarde Flyway chegar à V15;
3. faça smoke test de criação, edição concorrente e isolamento entre tutores;
4. valide upload e download com um bucket privado de preview;
5. implante o frontend e monitore `400`, `409`, `429` e `502` nas novas rotas;
6. acompanhe o volume de `PENDING`, `PENDING_DELETE` e do prefixo `staging/`.

Rollback do binário permanece possível porque a migração só adiciona estruturas.
Durante rollback, suspenda escritas clínicas se a versão anterior não conhecer a
V15; os dados permanecem preservados no schema.
