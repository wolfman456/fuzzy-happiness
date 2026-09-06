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

Session endpoints (members only; GM = creator, roles `GM`/`PLAYER`/`SPECTATOR`):

| Method & path | Description |
|---|---|
| `GET /api/games` | available games (`dnd-5e`, seeded) |
| `POST /api/sessions` | create a session (creator becomes GM) → invite code |
| `POST /api/sessions/join` | join by invite code (assigns `PLAYER`) |
| `GET /api/sessions/{id}` | session snapshot incl. `recentEvents` |
| `POST /api/sessions/{id}/leave` | leave the session |

Realtime (STOMP over `/ws?token=<jwt>`, membership-private):

| Destination | Direction | Payload |
|---|---|---|
| `/app/sessions/{id}` | subscribe (snapshot) | `SessionSummary` on connect |
| `/app/sessions/{id}/chat` | publish | `{"text": …}` chat message |
| `/topic/sessions/{id}` | watch | live `SessionEventDto` (PRESENCE, CHAT, TABLE) |

Battle map endpoints (all under `/api/sessions/{id}/map`; map ops broadcast a `TABLE`
session event carrying the full `BattleMapDto` state):

| Method & path | Description |
|---|---|
| `GET …/map` | current map (or `404` if none) |
| `POST …/map` | GM: create map (idempotent; auto-tokens non-spectator participants) |
| `POST …/map/tokens` | GM: add token (defaults `MONSTER_NPC`, 30 ft) |
| `PATCH …/map/tokens/{tokenId}` | GM: update token (name/color/speedFeet) |
| `DELETE …/map/tokens/{tokenId}` | GM: remove token |
| `POST …/map/tokens/{tokenId}/move` | members: move own token (GM: any) within budget (`{"x", "y"}`) |
| `POST …/map/turn` | GM: `{"action":"START","tokenId":…}｜"END"｜"NEW_ROUND"` |

Map house rules: 10 ft per square, per-turn budget `floor(speedFeet / 10)` squares
(race table 25/30/35/40 ft), diagonal moves cost Chebyshev distance, budget resets on turn
`START` / `END` / `NEW_ROUND`, over-budget moves rejected with `400`.

Status codes: `400` validation / `401` bad or missing JWT / `403` unverified or forbidden /
`404` not found / `409` duplicate / `429` resend cooldown / `500` fallback.

## Configuration

- Profiles: `dev` (default — H2, console email, bootstrap admin) and `prod`
  (`application-prod.properties` — PostgreSQL, SMTP, required secrets).
- Settings overridable via env: see `tabletopserv.*` keys in `application.properties`
  (JWT secret + expiry, verification TTL + cooldown, bootstrap admin defaults, CORS origins)
  and the `*_*` env placeholders in `application-prod.properties` (SMTP host/port/user/password).
  `tabletopserv.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default
  `http://localhost:5173`) lists the origins allowed to call `/api/**`; the prod profile
  defaults to an empty list (no cross-origin access) until overridden.