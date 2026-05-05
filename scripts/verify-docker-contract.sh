#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-bidmart-auth-service:local-test}"

if [[ ! -f Dockerfile ]]; then
  echo "ERROR: Dockerfile is missing."
  exit 1
fi

if ! grep -Eq "EXPOSE .*8081" Dockerfile; then
  echo "ERROR: Dockerfile must expose REST port 8081."
  exit 1
fi

if ! grep -Eq "EXPOSE .*9091" Dockerfile; then
  echo "ERROR: Dockerfile must expose gRPC port 9091."
  exit 1
fi

docker build -t "$IMAGE_NAME" .

echo "OK: Auth Docker image built as $IMAGE_NAME and exposes REST + gRPC ports."
