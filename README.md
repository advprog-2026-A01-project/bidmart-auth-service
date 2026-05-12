# BidMart Auth Service
***By Neal Guarddin - Advance Programming A - 2406348282*** \
`bidmart-auth-service` adalah microservice yang menangani autentikasi, otorisasi awal, manajemen user, session, role, permission, dan penerbitan token untuk sistem BidMart.

Service ini adalah salah satu core backend service dalam arsitektur microservices BidMart.

## Role dalam Arsitektur BidMart

Auth Service bertanggung jawab untuk:

- registrasi user;
- login user;
- bootstrap admin lokal;
- issue JWT access token;
- issue opaque refresh token;
- menyimpan session di database Auth;
- expose public key JWT melalui JWKS endpoint;
- menyediakan endpoint terkait user, role, permission, dan admin.

Dalam arsitektur microservice BidMart, Auth Service **tidak dipanggil langsung oleh frontend**. Frontend harus memanggil API Gateway terlebih dahulu.

Alur normal:

```text
Frontend
   ↓
bidmart-api-gateway
   ↓
bidmart-auth-service
   ↓
auth_db
```

## Batasan Service

Auth Service hanya boleh memiliki database sendiri:

```text
Auth Service → auth_db
```

Service lain seperti Catalog Service dan Auction-Wallet Service tidak boleh mengakses `auth_db` secara langsung.

Service lain cukup menyimpan external id seperti:

```text
user_id
seller_id
bidder_id
```

Tidak boleh ada foreign key lintas database/service.

---

# Local Port

Secara lokal, Auth Service berjalan di:

```text
http://localhost:8081
```

Database lokal Auth Service berjalan di:

```text
localhost:5434
```

---

# Tech Stack

- Java 21
- Spring Boot
- Spring Security
- PostgreSQL
- Gradle
- Docker untuk local database
- JWT access token
- Opaque refresh token
- JWKS endpoint

---

# Environment Variables

Auth Service memakai environment variable berikut:

| Variable | Default Local | Keterangan |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5434/auth_db` | URL database Auth |
| `SPRING_DATASOURCE_USERNAME` | `auth` | Username database |
| `SPRING_DATASOURCE_PASSWORD` | `auth` | Password database |
| `ADMIN_BOOTSTRAP_ENABLED` | `true` | Enable pembuatan admin lokal |
| `ADMIN_BOOTSTRAP_USERNAME` | `admin` | Username admin lokal |
| `ADMIN_BOOTSTRAP_PASSWORD` | `admin12345` | Password admin lokal |
| `JWT_ISSUER` | `http://localhost:8081` | Issuer JWT |
| `JWT_KEY_ID` | `bidmart-auth-local-key-1` | Key ID untuk JWKS |
| `JWT_ACCESS_TOKEN_TTL_MINUTES` | `15` | TTL access token |
| `AUTH_ACCESS_TTL_MINUTES` | `15` | TTL yang dikirim di response login |
| `JWT_PRIVATE_KEY_BASE64` | dari `.local-keys/` | Private key untuk sign JWT |
| `JWT_PUBLIC_KEY_BASE64` | dari `.local-keys/` | Public key untuk JWKS |

---

# Setup Local Database

Jalankan PostgreSQL lokal memakai Docker:

```bash
docker rm -f bidmart-auth-db 2>/dev/null || true

docker run --name bidmart-auth-db \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth \
  -e POSTGRES_PASSWORD=auth \
  -p 5434:5432 \
  -d postgres:16
```

Cek container:

```bash
docker ps | grep bidmart-auth-db
```

---

# Setup Persistent Local JWT Key

Auth Service menggunakan RSA key untuk sign JWT. Untuk local development, gunakan persistent key agar Gateway tidak mengalami JWKS stale setelah Auth Service restart.

Generate key lokal:

```bash
mkdir -p .local-keys

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out .local-keys/jwt_private.pem

openssl rsa \
  -pubout \
  -in .local-keys/jwt_private.pem \
  -out .local-keys/jwt_public.pem

base64 -w 0 .local-keys/jwt_private.pem > .local-keys/jwt_private.pem.b64
base64 -w 0 .local-keys/jwt_public.pem > .local-keys/jwt_public.pem.b64
```

Pastikan file berikut muncul:

```bash
ls -lah .local-keys
```

Expected:

```text
jwt_private.pem
jwt_public.pem
jwt_private.pem.b64
jwt_public.pem.b64
```

Folder `.local-keys/` **tidak boleh di-commit** ke GitHub.

Pastikan `.gitignore` memiliki:

```text
.local-keys/
```

---

# Run Auth Service Locally

Pastikan database sudah hidup, lalu jalankan:

```bash
./scripts/run-local-auth.sh
```

Script ini akan:

- memakai database lokal `auth_db`;
- membuat admin lokal jika belum ada;
- memakai persistent JWT key dari `.local-keys/`;
- menjalankan Auth Service di port `8081`.

Jika muncul error:

```text
ERROR: .local-keys JWT key files not found.
```

berarti key lokal belum digenerate. Jalankan bagian "Setup Persistent Local JWT Key" terlebih dahulu.

---

# Verify Auth Service

Cek DB ping:

```bash
curl -s http://localhost:8081/api/db/ping | jq
```

Expected:

```json
{
  "db": 1
}
```

Cek JWKS:

```bash
curl -s http://localhost:8081/.well-known/jwks.json | jq
```

Expected terdapat field:

```json
{
  "keys": [...]
}
```

Cek `kid` key:

```bash
curl -s http://localhost:8081/.well-known/jwks.json | jq '.keys[0].kid'
```

Expected local:

```json
"bidmart-auth-local-key-1"
```

---

# Local Admin Account

Untuk local development, admin bootstrap memakai:

```text
username: admin
password: admin12345
```

Admin ini dibuat ketika Auth Service dijalankan dengan:

```text
ADMIN_BOOTSTRAP_ENABLED=true
```

---

# JWT dan Refresh Token Design

Auth Service menerbitkan dua jenis token:

## Access Token

Access token berbentuk JWT.

Karakteristik:

- short-lived;
- signed oleh Auth Service;
- divalidasi oleh API Gateway menggunakan JWKS;
- tidak perlu introspection ke Auth Service untuk setiap request.

JWT berisi claim utama:

```text
sub          = user id
username     = username
role         = user role
permissions  = list permission
jti          = session id
iss          = issuer
iat          = issued at
exp          = expiration time
```

## Refresh Token

Refresh token bersifat opaque/random.

Karakteristik:

- tidak berbentuk JWT;
- disimpan di `auth_db`;
- digunakan untuk mendapatkan access token baru;
- dapat dicabut melalui session management.

---

# JWKS Endpoint

Auth Service expose public key JWT melalui:

```text
GET /.well-known/jwks.json
```

Endpoint ini digunakan oleh API Gateway melalui environment variable:

```text
JWT_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json
```

API Gateway akan memakai JWKS ini untuk memvalidasi JWT access token.

---

# Integrasi dengan API Gateway

Auth Service tidak dirancang sebagai public entry point untuk frontend.

Frontend harus call:

```text
http://localhost:8080
```

yaitu API Gateway.

API Gateway kemudian meneruskan endpoint berikut ke Auth Service:

```text
/api/auth/**
/api/admin/**
/api/rbac/**
/api/db/**
/api/users/**
```

Contoh:

```text
Frontend call:
GET http://localhost:8080/api/auth/captcha

Gateway forward ke:
GET http://localhost:8081/api/auth/captcha
```

Contoh login:

```text
Frontend call:
POST http://localhost:8080/api/auth/login

Gateway forward ke:
POST http://localhost:8081/api/auth/login
```

Protected endpoint:

```text
Frontend call:
GET http://localhost:8080/api/users/me/profile
Authorization: Bearer <access_token>

Gateway:
1. validasi JWT memakai JWKS dari Auth Service;
2. forward request ke Auth Service;
3. inject trusted user context headers.
```

---

# Hubungan dengan Service Lain

Nantinya BidMart memiliki service lain:

```text
bidmart-catalog-service
bidmart-auction-wallet-service
```

Service-service tersebut tidak perlu mengakses Auth Service secara langsung untuk validasi user pada setiap request.

Alur yang diinginkan:

```text
Frontend
   ↓
API Gateway
   ↓
Catalog Service / Auction-Wallet Service
```

API Gateway akan memvalidasi JWT dan meneruskan user context melalui header:

```text
X-User-Id
X-Username
X-User-Role
X-User-Permissions
X-Gateway-Secret
```

Downstream service harus:

1. menerima request hanya dari API Gateway;
2. memvalidasi `X-Gateway-Secret`;
3. menggunakan `X-User-Id`, `X-Username`, `X-User-Role`, dan `X-User-Permissions` sebagai trusted user context;
4. tidak query langsung ke `auth_db`.

---

# Test

Jalankan unit test:

```bash
./gradlew clean test
```

Jalankan PMD:

```bash
./gradlew pmdMain pmdTest
```

Expected:

```text
BUILD SUCCESSFUL
```

---

# Common Local Run Order

Urutan menjalankan sistem lokal:

## Terminal 1 — Database

```bash
docker rm -f bidmart-auth-db 2>/dev/null || true

docker run --name bidmart-auth-db \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth \
  -e POSTGRES_PASSWORD=auth \
  -p 5434:5432 \
  -d postgres:16
```

## Terminal 2 — Auth Service

```bash
cd bidmart-auth-service
./scripts/run-local-auth.sh
```

## Terminal 3 — API Gateway

```bash
cd bidmart-api-gateway
./scripts/run-local-gateway.sh
```

## Terminal 4 — Smoke Test dari Gateway

```bash
cd bidmart-api-gateway
./scripts/smoke-auth-gateway.sh
```

---

# Troubleshooting

## `Connection to localhost:5434 refused`

Database belum hidup.

Fix:

```bash
docker rm -f bidmart-auth-db 2>/dev/null || true

docker run --name bidmart-auth-db \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth \
  -e POSTGRES_PASSWORD=auth \
  -p 5434:5432 \
  -d postgres:16
```

## `.local-keys JWT key files not found`

JWT key lokal belum dibuat.

Fix: jalankan bagian "Setup Persistent Local JWT Key".

## Login berhasil, tapi protected endpoint via Gateway 401

Kemungkinan Gateway masih cache JWKS lama.

Fix:

1. Pastikan Auth Service memakai persistent key.
2. Restart API Gateway.
3. Jalankan smoke test ulang.

## `invalid_credentials`

Cek admin bootstrap dan pastikan login memakai:

```text
username: admin
password: admin12345
```

Jika memakai smoke script, gunakan variable berikut jika ingin override:

```bash
BIDMART_LOGIN_USERNAME=admin \
BIDMART_LOGIN_PASSWORD=admin12345 \
./scripts/smoke-auth-gateway.sh
```

Jangan memakai env variable `USERNAME`, karena di beberapa OS nilainya bisa berisi username Linux.
