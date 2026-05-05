# Docker Evidence: Auth Service

## Purpose

This document records Docker evidence for `bidmart-auth-service`.

The Auth Service exposes two runtime ports:

~~~text
8081 = REST/JWKS/Auth HTTP API
9091 = internal gRPC server
~~~

## Build Check

~~~bash
./scripts/verify-docker-contract.sh
~~~

## Required Runtime Environment Variables

~~~text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_ISSUER
JWT_KEY_ID
JWT_PRIVATE_KEY_BASE64
JWT_PUBLIC_KEY_BASE64
AUTH_GRPC_PORT
~~~

## Docker Compose Note

Inside Docker Compose, the database URL should use the database service name:

~~~text
jdbc:postgresql://auth-db:5432/auth_db
~~~

Do not use `localhost` between containers.
