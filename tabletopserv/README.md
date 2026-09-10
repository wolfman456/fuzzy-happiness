# tabletopserv

Spring Boot 4 (Java 21) backend for fuzzy-happiness. **Multi-module Maven reactor** that
builds as an executable JAR with embedded Tomcat.

## Modules

| Module | Package | What it is |
|---|---|---|
| `tabletopapi` | `com.gamer.fowever.tabletopapi` | Inbound REST contract — controller **interfaces** + DTOs + shared error types. No business logic. |
| `tabletopservice` | `com.gamer.fowever.tabletopservice` | `*ControllerImpl implements *Api` + services, domain, repos, security, config, STOMP glue, `application*.properties`, runnable JAR. jacoco ≥ 90% line gate here. |
| `tabletopfunctionaltest` | *(blank)* | Future functional/E2E suites (§18) — commented out of parent `<modules>` until first suite is written. |

## Commands (run from this directory)

```sh
./mvnw test                                           # build + test all modules, jacoco gate (service only ≥ 90%)
./mvnw -pl tabletopservice -am spring-boot:run         # dev server (dev profile: in-memory H2, bootstrap admin)
./mvnw -pl tabletopapi -am package                     # build just the API library
./mvnw package                                        # package the whole reactor
```

## Implemented API

Auth endpoints (JSON; business errors via `GlobalExceptionHandler`):

| Method & path | Description |
|---|---|
| `POST /api/auth/register` | create account (display name + **real name**, email, DoB ≥ 13, strict password entered twice) → `201` + verification email |
| `POST /api/auth/login` | login by username **or** email → 24h JWT; `403` until email verified |
| `GET /api/auth/verify?token=` | confirm email (single-use, 24h expiry) |
| `POST /api/auth/resend-verification` | resend verification link (60s cooldown; always `202`, enumeration-safe) |
| `GET /api/users/me` | current user profile (JWT required) |
| `GET /api/admin/users` | admin-only user listing |

PII (email, names, DoB) is **encrypted at rest** (AES-256-GCM field converters,
`tabletopserv.pii.secret`); `email` is not unique — a deterministic `email_key` blind index
backs uniqueness and username/email login.

Session endpoints (members only; GM = creator, roles `GM`/`PLAYER`/`SPECTATOR`):

| Method & path | Description |
|---|---|
| `GET /api/games` | available games (`dnd-5e`, seeded) |
| `POST /api/sessions` | create a session (creator becomes GM) → invite code |
| `POST /api/sessions/join` | join by invite code (assigns `PLAYER`) |
| `GET /api/sessions/{id}` | session snapshot incl. `recentEvents` |
| `POST /api/sessions/{id}/leave` | leave the session |

SRD endpoints (routed through the **gateway** — see `draft-design.md` §9/§16):

| Method & path | Description |
|---|---|
| `GET /api/srd/{collection}` | list — allowlisted collections: races, classes, subclasses, subraces, ability-scores, skills, proficiencies, equipment, equipment-categories, spells, features, traits, feats, conditions, languages, monsters |
| `GET /api/srd/{collection}/{index}` | detail by SRD index |
| `GET /api/srd/spells?level=&school=` | spells with optional curated query passthrough |

Realtime (STOMP over `/ws?token=<jwt>`, membership-private):

| Destination | Direction | Payload |
|---|---|---|
| `/app/sessions/{id}` | subscribe (snapshot) | `SessionSummary` on connect |
| `/app/sessions/{id}/chat` | publish | `{"text": …}` chat message |
| `/topic/sessions/{id}` | watch | live `SessionEventDto` (PRESENCE, CHAT, DICE, TABLE) |
| `/user/queue/dice` | watch | GM-private dice result (full frame, only to the rolling GM) |

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
| `POST …/map/initiative` | GM: replace order — `{"entries":[{label｜tokenId, score?}]}` (≤ 30; blank score auto-rolls d20; resets turn pointer to `−1`) |
| `POST …/map/initiative/{entryId}/reroll` | GM: re-roll one entry's score (d20) |
| `POST …/map/initiative/next` | GM: advance the turn pointer (wraps); entry with a token becomes the current turn (resets its movement budget) |
| `DELETE …/map/initiative/{entryId}` | GM: drop an entry (pointer clamped) |

Dice endpoints:

| Method & path | Description |
|---|---|
| `POST /api/sessions/{id}/roll` | member: `{"expression":"2d6+3","label":"Perception","privateRoll":true}` — grammar `(\d+)?d(\d{1,3})([+-]\d{1,3})?` (≤ 20 dice, ≤ 999 sides, mod −100..100) |

Dice rules: rolls are server-side `SecureRandom`. Public rolls persist a `DICE` `SessionEvent`
and broadcast the full result on `/topic/sessions/{id}`. GM-private rolls broadcast a **hidden**
frame on the topic (roller, expression, label — no rolls/total) and deliver the full frame only
to the rolling GM on `/user/queue/dice`; the REST response carries the full frame with `id: null`
(never persisted).

Map house rules: 10 ft per square, per-turn budget `floor(speedFeet / 10)` squares
(race table 25/30/35/40 ft), diagonal moves cost Chebyshev distance, budget resets on turn
`START` / `END` / `NEW_ROUND`, over-budget moves rejected with `400`.

Status codes: `400` validation / `401` bad or missing JWT / `403` unverified or forbidden /
`404` not found / `409` duplicate / `429` resend cooldown / `500` fallback.

## Configuration

- Profiles: `dev` (default — H2, console email, bootstrap admin) and `prod`
  (`application-prod.properties` — PostgreSQL, SMTP, required secrets).
- Settings overridable via env: see `tabletopserv.*` keys in `application.properties`
  (JWT secret + expiry, verification TTL + cooldown, bootstrap admin defaults, PII secret,
  frontend URL, CORS origins, gateway URL/token) and the `*_*` env placeholders in
  `application-prod.properties` (SMTP host/port/user/password).
- **PII encryption:** `tabletopserv.pii.secret` (env `PII_SECRET`) — required in prod
  (startup fails fast if blank). Dev/test fall back to a fixed key.
- **Bootstrap admin:** seeded on any profile, guarded on a non-blank
  `tabletopserv.admin.password` (env `ADMIN_PASSWORD`), username/email via
  `ADMIN_USERNAME`/`ADMIN_EMAIL` (email optional in prod — falls back to
  `<username>@tabletop.local`).
- Gateway keys: `tabletopserv.gateway.url` (env `GATEWAY_URL`, default
  `http://localhost:3001`) and `tabletopserv.gateway.token` (env `GATEWAY_TOKEN`,
  sent as `X-Gateway-Token`).
- `tabletopserv.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default
  `http://localhost:5173`) lists the origins allowed to call `/api/**`; the prod profile
  defaults to an empty list (no cross-origin access) until overridden.
