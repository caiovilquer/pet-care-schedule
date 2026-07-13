#!/usr/bin/env bash
# Sobe o bootstrap em perfil dev com MailHog + MinIO locais.
# Pré-requisito: docker compose up -d
set -euo pipefail

cd "$(dirname "$0")/.."

export MAIL_FROM="${MAIL_FROM:-noreply@localhost}"
export OBJECT_STORAGE_ENABLED=true
export OBJECT_STORAGE_ENDPOINT="${OBJECT_STORAGE_ENDPOINT:-http://localhost:9000}"
export OBJECT_STORAGE_REGION="${OBJECT_STORAGE_REGION:-us-east-1}"
export OBJECT_STORAGE_BUCKET="${OBJECT_STORAGE_BUCKET:-rotinapet}"
export OBJECT_STORAGE_ACCESS_KEY="${OBJECT_STORAGE_ACCESS_KEY:-minioadmin}"
export OBJECT_STORAGE_SECRET_KEY="${OBJECT_STORAGE_SECRET_KEY:-minioadmin}"
export OBJECT_STORAGE_PATH_STYLE=true

exec ./gradlew :bootstrap:bootRun "$@"
