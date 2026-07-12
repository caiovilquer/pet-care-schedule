# Object storage de fotos

O RotinaPet usa o mesmo adaptador S3 para Railway Buckets e Cloudflare R2. O bucket deve permanecer privado. O navegador recebe uma URL `PUT` válida por 3 minutos; depois do envio, a API confere tamanho, SHA-256, assinatura real, decodificação e dimensões da imagem antes de promovê-la de `staging/` para uma chave imutável em `media/`.

## Railway Bucket

1. Crie um Bucket no mesmo ambiente do backend.
2. Na aba **Variables** do backend, injete por referência as credenciais `ENDPOINT`, `REGION`, `BUCKET`, `ACCESS_KEY_ID` e `SECRET_ACCESS_KEY` do bucket.
3. Adicione `OBJECT_STORAGE_ENABLED=true`.
4. Use `OBJECT_STORAGE_PATH_STYLE=false` para buckets novos (virtual-hosted). Para um bucket antigo, copie o estilo indicado na aba **Credentials** e use `true` se ela indicar path-style.

Os nomes antigos `BUCKET_ENDPOINT`, `BUCKET_REGION`, `BUCKET_NAME`, `BUCKET_ACCESS_KEY_ID` e `BUCKET_SECRET_ACCESS_KEY`, além dos nomes AWS comuns, continuam aceitos por compatibilidade.

## Cloudflare R2

Configure no backend:

```text
OBJECT_STORAGE_ENABLED=true
OBJECT_STORAGE_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
OBJECT_STORAGE_REGION=auto
OBJECT_STORAGE_BUCKET=<NOME_DO_BUCKET>
OBJECT_STORAGE_ACCESS_KEY=<ACCESS_KEY_ID_DO_TOKEN_R2>
OBJECT_STORAGE_SECRET_KEY=<SECRET_ACCESS_KEY_DO_TOKEN_R2>
OBJECT_STORAGE_PATH_STYLE=true
```

Crie um token S3 restrito somente a esse bucket, com leitura e escrita de objetos. Não use o token global da conta e nunca exponha essas variáveis no frontend.

## CORS obrigatório para upload direto

Cadastre apenas as origens reais do frontend. Para R2, a política equivalente é:

```json
[
  {
    "AllowedOrigins": [
      "https://rotinapet.vilquer.dev",
      "http://localhost:4200"
    ],
    "AllowedMethods": ["PUT"],
    "AllowedHeaders": ["Content-Type", "x-amz-meta-sha256"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

Remova `localhost` da política de produção se ele não for necessário. Não use `AllowedOrigins: ["*"]`: uma URL pré-assinada funciona como credencial temporária.

No Railway, configure a mesma permissão de origem/método na opção de CORS do Bucket. Os cabeçalhos enviados devem ser exatamente os devolvidos por `POST /api/v1/media/uploads`, pois eles fazem parte da assinatura.

## Operação e recuperação

- `PENDING` expira em 15 minutos; um job com lock distribuído apaga uploads incompletos.
- substituições e exclusões viram `PENDING_DELETE` antes da remoção física;
- se o storage falhar durante a limpeza, o registro é mantido para uma nova tentativa;
- arquivos prontos são lidos por redirect para uma URL `GET` curta, sem tornar o bucket público;
- monitore respostas 429 (limite de uploads), 502 (storage indisponível) e o crescimento do prefixo `staging/`.

Antes de ativar em produção, valide criação, troca e remoção de foto em um ambiente de preview com um bucket isolado.
