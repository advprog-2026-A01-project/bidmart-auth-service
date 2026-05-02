#!/usr/bin/env bash
set -euo pipefail

if docker ps --format '{{.Names}}' | grep -qx "bidmart-auth-db"; then
  echo "bidmart-auth-db is already running."
else
  docker rm -f bidmart-auth-db 2>/dev/null || true

  docker run --name bidmart-auth-db \
    -e POSTGRES_DB=auth_db \
    -e POSTGRES_USER=auth \
    -e POSTGRES_PASSWORD=auth \
    -p 5434:5432 \
    -d postgres:16
fi

echo "Waiting for auth_db..."
until docker exec bidmart-auth-db pg_isready -U auth -d auth_db; do
  sleep 1
done

echo "auth_db is ready at localhost:5434"
