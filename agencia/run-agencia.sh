#!/usr/bin/env bash
set -euo pipefail

id_agencia="${1:?Uso: ./run-agencia.sh <id-agencia> [porta]}"
porta="${2:-$((4000 + id_agencia))}"

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Erro: arquivo .env não encontrado em $(pwd)" >&2
  exit 1
fi

expiration_override="${JWT_EXPIRATION_SECONDS-}"
secret_override="${JWT_SECRET-}"
username_override="${AUTH_USERNAME-}"
password_override="${AUTH_PASSWORD-}"

set -a
source .env
set +a

if [[ -n "$expiration_override" ]]; then export JWT_EXPIRATION_SECONDS="$expiration_override"; fi
if [[ -n "$secret_override" ]]; then export JWT_SECRET="$secret_override"; fi
if [[ -n "$username_override" ]]; then export AUTH_USERNAME="$username_override"; fi
if [[ -n "$password_override" ]]; then export AUTH_PASSWORD="$password_override"; fi

exec env AGENCIA_ID="$id_agencia" SERVER_PORT="$porta" ./gradlew bootRun
