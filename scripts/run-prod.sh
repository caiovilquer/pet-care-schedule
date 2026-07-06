#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JVM_OPTS_FILE="${ROOT_DIR}/config/jvm.options"

JAVA_OPTS="${JAVA_OPTS:-}"
if [[ -z "${JAVA_OPTS}" && -f "${JVM_OPTS_FILE}" ]]; then
  JAVA_OPTS="$(tr '\n' ' ' < "${JVM_OPTS_FILE}")"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

exec java ${JAVA_OPTS} -jar "${ROOT_DIR}/bootstrap/build/libs/rotinapet.jar"
