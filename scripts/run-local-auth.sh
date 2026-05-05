#!/usr/bin/env bash
set -euo pipefail

export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5434/auth_db}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-auth}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-auth}"

export ADMIN_BOOTSTRAP_ENABLED="${ADMIN_BOOTSTRAP_ENABLED:-true}"
export ADMIN_BOOTSTRAP_USERNAME="${ADMIN_BOOTSTRAP_USERNAME:-admin}"
export ADMIN_BOOTSTRAP_PASSWORD="${ADMIN_BOOTSTRAP_PASSWORD:-admin12345}"

export JWT_ISSUER="${JWT_ISSUER:-http://localhost:8081}"
export JWT_KEY_ID="${JWT_KEY_ID:-bidmart-auth-local-key-1}"
export JWT_ACCESS_TOKEN_TTL_MINUTES="${JWT_ACCESS_TOKEN_TTL_MINUTES:-15}"
export AUTH_ACCESS_TTL_MINUTES="${AUTH_ACCESS_TTL_MINUTES:-15}"
export AUTH_GRPC_PORT="${AUTH_GRPC_PORT:-9091}"

if [[ -f ".local-keys/jwt_private.pem.b64" && -f ".local-keys/jwt_public.pem.b64" ]]; then
  export JWT_PRIVATE_KEY_BASE64="$(cat .local-keys/jwt_private.pem.b64)"
  export JWT_PUBLIC_KEY_BASE64="$(cat .local-keys/jwt_public.pem.b64)"
  echo "Using persistent local JWT RSA key from .local-keys/"
  echo "AUTH_GRPC_PORT=$AUTH_GRPC_PORT"
else
  echo "ERROR: .local-keys JWT key files not found."
  echo "Generate them first before running Auth Service."
  exit 1
fi

./gradlew bootRun
