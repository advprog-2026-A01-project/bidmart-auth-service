# TDD Evidence: Auth Service gRPC Server

## Scope

This evidence covers the internal gRPC server inside `bidmart-auth-service`.

```text
bidmart-auth-service = gRPC server
bidmart-api-gateway  = gRPC client
```

## Red-Green-Refactor Commits

```text
d381537 [GREEN] Expose auth gRPC server with gRPC starter
a6b1dd6 [GREEN] Expose auth gRPC server with gRPC starter
20877aa feat: add internal grpc server for auth service
f90bfe7 docs: update auth service local run instructions
e6fa2bd Added README.md
eeda15b fix: prepare auth service for gateway integration
```

## Validation Commands

```bash
./scripts/start-local-auth-db.sh
./gradlew clean generateProto test pmdMain pmdTest
```

## Implementation Notes

The Auth Service exposes the internal gRPC endpoint through Spring using:

```java
@GrpcService
public class AuthInternalGrpcService
```

The old manual gRPC server approach is removed because the service is now managed by `grpc-server-spring-boot-starter`.

## Test Isolation

Integration tests clean auth-domain test data before each test method using:

```java
@CleanAuthDatabase
```

This prevents fixed usernames such as `user1`, `user2`, `u_limit`, and `u_revoke` from making repeated local test runs fail.

## Runtime Port

```text
AUTH_GRPC_PORT=9091
grpc.server.port=${AUTH_GRPC_PORT:9091}
```
