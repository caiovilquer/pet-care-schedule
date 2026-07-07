#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JVM_OPTS_FILE="${ROOT_DIR}/config/jvm.options"

JAVA_OPTS="${JAVA_OPTS:-}"
if [[ -z "${JAVA_OPTS}" && -f "${JVM_OPTS_FILE}" ]]; then
  # ignora comentários e linhas vazias para permitir documentar os flags
  JAVA_OPTS="$(grep -vE '^\s*(#|$)' "${JVM_OPTS_FILE}" | tr '\n' ' ')"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

# glibc cria até 8 arenas de malloc por core; a JVM só precisa de poucas e o
# excesso fragmenta memória nativa (RSS maior sem uso real). 2 é o valor
# clássico para JVMs em servidor pequeno.
export MALLOC_ARENA_MAX="${MALLOC_ARENA_MAX:-2}"

exec java ${JAVA_OPTS} -jar "${ROOT_DIR}/bootstrap/build/libs/rotinapet.jar"
