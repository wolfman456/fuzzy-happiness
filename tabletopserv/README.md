# tabletopserv

Spring Boot 4 (Java 21) backend for fuzzy-happiness. Builds as an executable JAR with
embedded Tomcat. Source package: `com.gamer.fowever.tabletopserv`.

## Commands (run from this directory)

```sh
./mvnw spring-boot:run   # dev server (dev profile is default: in-memory H2, bootstrap admin)
./mvnw test              # tests + jacoco line-coverage gate (>= 90%)
./mvnw package           # builds target/tabletopserv-0.0.1-SNAPSHOT.jar
```

## Implemented API

Auth endpoints (JSON; business errors via `GlobalExceptionHandler`):

| Method & path | Description |
|---|---|
| `POST /api/auth/register` | create account (username, email, DoB ≥ 13, strict password) → `201` + verification email |
| `POST /api/auth/login` | login by username **or** email → 24h JWT; `403` until email verified |
| `GET /api/auth/verify?token=` | confirm email (single-use, 24h expiry) |
| `POST /api/auth/resend-verification` | resend verification link (60s cooldown; always `202`, enumeration-safe) |
| `GET /api/users/me` | current user profile (JWT required) |
| `GET /api/admin/users` | admin-only user listing |

Status codes: `400` validation / `401` bad or missing JWT / `403` unverified or forbidden /
`409` duplicate username/email / `429` resend cooldown / `500` fallback.

## Configuration

- Profiles: `dev` (default — H2, console email, bootstrap admin) and `prod`
  (`application-prod.properties` — PostgreSQL, SMTP, required secrets).
- Settings overridable via env: see `tabletopserv.*` keys in `application.properties`
  (JWT secret + expiry, verification TTL + cooldown, bootstrap admin defaults) and the
  `*_*` env placeholders in `application-prod.properties` (SMTP host/port/user/password).