# Fuzzy Happiness — Tabletop Game Platform (Initial Design Draft)

> **Status:** Draft v0.5 — accounts & auth implemented end-to-end (backend `feature/spring-security` +
> web UI in `feature/frontend-auth`).
> A starting point to iterate on as requirements become clearer.
> Open questions and things to decide are flagged inline and collected in [Open Questions](#open-questions--open-decisions).

## 1. Overview

A web platform for playing tabletop games with friends remotely, without needing to
re-buy digital copies of rulebooks or materials (e.g. Roll20). Both players and the
game master (GM) join a shared **session** over the web; voice lives in Discord as a
companion service.

The project starts with support for **Dungeons & Dragons (5e)** as the first concrete
game, but the core (users, sessions, characters, games) is designed to be generic so
additional games can be plugged in later.

## 2. Vision / Guiding Principles

- **Real-time, social, multiplayer.** Multiple people are in one session at a time,
  seeing the same table state and interacting with each other.
- **Vendor-neutral for games.** The `Character` model must allow porting/share common
  things from one game to another — hence an abstract character base shared by all games.
- **Play what you own.** The platform is a table + tooling, not a content shop.
- **Start DnD, stay generic.** Concrete D&D support proves the pattern; a game registry
  lets new systems be added without redesigning the core.
- **Simple by default.** Iterate on one working vertical slice before adding breadth.

## 3. Requirements (as user stories)

| # | As a user I want ... | so I can ... |
|---|---|---|
| R1 | register / log in with an account | keep my characters and settings across sessions |
| R2 | create a game session (room) and get an invite code | invite my friends to play with me |
| R3 | join a session with others | all be in the same room at the same time |
| R4 | see who is in the session and chat live | interact with the group in real time |
| R5 | create a character for the game being played | play that game |
| R6 | have characters share a common base across games | reuse identity/portrait/backstory when starting in a new game |
| R7 | connect to Discord | use it to provide voice while we play |
| R8 | keep progress in H2 while developing, PostgreSQL in production | develop without installing a DB and run reliably in prod |

## 4. System Architecture

```
                        ┌──────────────────────────────────────────────┐
                        │                 Frontend (SPA)              │
                        │        React 19 + Vite (tabletopweb/)        │
                        │  Lobby │ Session view │ Sheet editor │ Auth  │
                        └───────┬──────────────┬──────────────┬───────┘
                                │ REST (CRUD)  │  WS/STOMP    │
       ┌──────────┐             ▼              ▼              ▼         ┌────────────────┐
       │ Discord  │  OAuth /   ┌─────────────────────────────────────┐  │    Database     │
       │ (voice)  │  deep-link │          Spring Boot Backend        │  │                 │
       │ client   │◄───────────┤    (tabletopserv/, Java 21, Boot 4) │  │  Profile-based: │
       │  runs in │            │ REST controllers │ STOMP broker      │  │   dev  → H2     │
       │ user OS  │            │ SessionService  │ Auth/accounts      │  │   prod → Postgres│
       └──────────┘            │ Game registry   │ Character service  │  │                 │
                               └─────────────────────────────────────┘  └─────────────────┘
```

**Decisions:**

- **Backend:** Spring Boot 4 (Java 21), existing `tabletopserv/`. REST (via
  `spring-boot-starter-webmvc`) for queries/mutations + **STOMP over WebSocket**
  (add `spring-boot-starter-websocket`) for real-time session events.
- **Frontend:** existing React 19 + Vite app in `tabletopweb/`, plain JSX. Views:
  login/register, lobby, session (chat + table), character sheets. Now running Tailwind CSS v4
  (via `@tailwindcss/vite`) and `react-router-dom`, pinning Node 24; auth pages + protected
  dashboard shell implemented in `feature/frontend-auth`.
- **Real-time:** server-authoritative. Clients send commands; the server updates state
  and broadcasts events to everyone subscribed to that session's topic.
- **Auth (implemented):** `spring-security` accounts with **JWT bearer** (24h, HS-signed,
  jjwt) for REST, roles `USER`/`MODERATOR`/`ADMIN`, bcrypt password hashing, and email
  verification (login is blocked until the address is confirmed). Auth endpoints landed in
  `feature/spring-security` — see [§12 API Surface](#12-proposed-api-surface-initial).
- **Discord VoIP:** Discord has **no public API to programmatically join a voice
  channel** — see [§10 Discord VoIP Integration](#10-discord-voip-integration).
- **Rules data:** the D&D plug-in consumes the open 5e-bits SRD API through a backend
  proxy with caching — see [§9 D&D 5e SRD Integration](#9-dd-5e-srd-integration-5e-bits--dnd5eapico).

## 5. Core Domain Model

```
User ── owns ──> Character (abstract base)
                   ▲
        ┌──────────┴───────────┐
        │ implemented by      │ registered per game
   Dnd5eCharacter            (future) OtherGameCharacter
User ── joins ──> Session ──< Game (registry entry, e.g. "dnd-5e")
                    │
                    └─> Participant (user + role: GM / player / spectator)
Session ── emits ──> SessionEvent (presence, chat, dice, table state)
```

**Entities (JPA):**

| Entity | Notes |
|---|---|
| `User` | id, display name, username (unique), email, date of birth, email-verified flag, auth role (`USER`/`MODERATOR`/`ADMIN`), password hash |
| `EmailVerificationToken` | single-use, 24h expiry; token delivered by email on signup / resend |
| `Game` | registry entry: slug (`dnd-5e`), display name, sheet schema |
| `Character` | **abstract** base: id, name, portrait, description/backstory, game version. Contains only fields that translate across games. Serialized as a discriminated (TPH) entity per game. |
| `Dnd5eCharacter` | concrete D&D fields: ability scores + score source, level, class, race, HP, AC, skills ...; rule data held as SRD `index` references (see §9) |
| `CharacterDraft` | transient builder payload: ability score set (house-rule d20) + starting level 1–3 + SRD `index` references; compiled & validated before a `Character` is persisted (see §8) |
| `Session` | id, name, invite code, game slug, status (open/active/closed), created by |
| `Participant` | join row: session + user + role + joinedAt |
| `SessionEvent` / chat | history of events for late joiners / audit |

**Character abstraction (R6):** the base is intentionally an abstract class (or an
interface + base class). Translatable attributes (name, portrait, backstory, creator,
game history) live on the base; game-specific mechanics are scoped to the concrete
subclass. A `GameDefinition` describes the sheet schema so the UI can render a sheet
for any registered game without hard-coded per-game forms.

## 6. Real-Time Sessions

- Lobby: create session → returns invite code; join by code → WebSocket subscription
  to `/topic/sessions/{id}`.
- Server-authoritative events broadcast on the topic:
  - presence (`participant joined/left`)
  - chat (`message`)
  - dice roll (server-generated random, for trust) (`dice`)
  - table state snapshot / updates (`table`)
- Late joiners get a snapshot event (state replay) on subscribe.
- Colocation with security: only members of the session can subscribe.

## 7. Character & Game Plug-ins

- `Character` (abstract) — common base, framework-agnostic.
- Per game: concrete subclass + registration in the `Game` registry.
- Sheet rendering driven by the game's schema (fields, types, groups) so the frontend
  needs no per-game form code for MVP-level sheets.
- **Generation:** a guided wizard + random quick-build, validated by a pure server
  compile step — see [§8 Character Generation](#8-character-generation-guided-wizard--quick-build).
- **MVP ships D&D 5e only**; plug-in path documented so future games (e.g. Call of
  Cthulhu, Pathfinder) reuse the base.

## 8. Character Generation (guided wizard + quick-build)

Character creation is a **draft → compile → finalize** flow. The frontend assembles a
draft of choices (as SRD `index` references, §9); the server compiles it into a valid
sheet with derived stats.

**Two entry modes:**

- **Guided wizard** — step sequence: name → ability scores → race → class → subclass →
  background → skill picks → starting equipment → spells. Each step loads its options
  from the SRD proxy.
- **Random quick-build ("surprise me")** — the server assembles a legal random draft
  (random scores/race/class/subclass/background, permitted skill & spell picks) and
  returns a preview; the user can push it into the wizard to edit, or save as-is.

**Ability scores (v1 house rule):** the server rolls **6 × d20** (server-side, for
trust) and returns an **unassigned set**; the player assigns the values to
STR/DEX/CON/INT/WIS/CHA. This is a deliberate table house rule; standard array, point
buy, and 4d6-drop-lowest are noted as future "score source" strategies.

**Starting level:** configurable **1–3** at creation. HP = class hit die + CON per
level (MVP simplification: max each level's roll); subclass timing and spell slots
follow the chosen class's rules (some subclasses start at class level 1, others at 3).

**Server compile (`POST /api/characters/compile`)** — a pure, idempotent step that

- validates every choice against the SRD allow-lists (skill picks legal per
  class/background, spells vs the class list, subclass legal for the class);
- computes derived stats: ability modifiers, proficiency bonus, saving throws, HP, AC,
  spell slots, skills;
- returns a sheet preview, or field-level violations.

**Lifecycle:** `draft (client)` → `compile (validate + derive)` → `finalize` persists a
`Dnd5eCharacter` (references + display snapshot). Later edits re-run the same compile.

**Data:** a draft is a plain JSON payload. The persisted `Dnd5eCharacter` stores ability
scores (with their score source), starting level, and SRD references, plus cached display
snapshots so the sheet renders without replaying the whole data set.

## 9. D&D 5e SRD Integration (5e-bits / dnd5eapi.co)

**The data source for the D&D plug-in.** The character builder and game table are backed
by the [D&D 5e SRD API](https://5e-bits.github.io/docs/introduction) (dnd5eapi.co):

- Open API — **no authentication**, GET-only.
- REST base `https://www.dnd5eapi.co/api`, versioned today as `/api/2014` (pinned now;
  a future `/api/2024` is the upgrade path).
- Generous rate limit (10,000 req/s per IP); we still cache to keep the sheet editor fast.
- Resources map directly to character creation: `races`, `classes`, `subclasses`,
  `subraces`, `ability-scores`, `skills`, `proficiencies`, `equipment` (+ categories),
  `spells` (filter by `level` / `school`), `features`, `traits`, `feats`, `conditions`,
  `languages`, and `monsters` (GM table later).

**Integration approach (decided): live proxy + cache over REST.**

- **Backend only:** a `SrdClient` service (Spring `WebClient` — `webflux` already
  present) proxies a curated, allowlisted set of endpoints. The frontend never calls
  dnd5eapi.co directly (no CORS, single place to cache/validate).
- **Caching:** Spring Cache + Caffeine; long TTL on list endpoints (races, classes,
  ability scores, skills, spells, equipment). Sheet options stay fast after first fetch.
- **Fallback:** if the SRD API is unreachable, serve the cached copy; if the cache is
  cold, return a clear "rules data unavailable" error instead of a partial sheet.
- **Security:** no raw URL forwarding — only curated paths and allowlisted indexes are
  exposed to clients.

**Model linkage:** `Dnd5eCharacter` stores **SRD `index` references** (e.g.
`raceIndex`, `classIndex`, `subclassIndex`, `spellIndexes[]`, `featureIndexes[]`)
instead of copying rule data; the server validates a sheet's choices against SRD
resources so characters stay consistent with the rules.

## 10. Discord VoIP Integration

**Reality check:** Discord exposes no public API that lets an application join or drop
into a voice channel programmatically (OAuth SCIM only covers user/guild admin; the
voice client is closed). Therefore:

- **Connect Discord** — OAuth2 "connect account" link so the app can show the user's
  Discord name and lets them pick/enter a voice channel of a server.
- **Launch companion** — the app opens the chosen voice channel in the user's installed
  Discord client (deep link) and the web session runs beside it as the shared game table.
- **UX copy:** "Voice runs in Discord; this tab is your table." Keep a status hint in
  the session view.

This is an acceptable MVP trade-off and keeps the platform out of the voice business.

## 11. Database Strategy (H2 dev / PostgreSQL prod)

- **JPA entities** as in §5.
- **Spring profiles:**
  - `dev` (default): in-memory/file **H2**, `ddl-auto: update`, sample seed data.
  - `prod`: **PostgreSQL**, `ddl-auto: validate + Flyway migrations` (add later as a
    tracked, deliberate step), credentials via env vars, never in-repo.
- **Dependencies to add:** `com.h2database:h2` (runtime), `org.postgresql:postgresql`
  (runtime).
- Schema migrations deferred until the model stabilizes; until then H2 update mode is
  fine for iteration.

## 12. Proposed API Surface (initial)

```
POST   /api/auth/register            create account                          ✓
POST   /api/auth/login               obtain token (username or email)        ✓
GET    /api/auth/verify?token=       confirm email (single-use, 24h)         ✓
POST   /api/auth/resend-verification resend verification (60s cooldown)      ✓
GET    /api/users/me                 current user profile (JWT)              ✓
GET    /api/admin/users              admin-only user listing                 ✓
POST   /api/sessions               create session (returns invite code)
GET    /api/sessions/{id}          snapshot (participants, game, status)
POST   /api/sessions/join          join by invite code
POST   /api/sessions/{id}/leave
GET    /api/games                  registered games + sheet schemas
GET    /api/srd/races              SRD reference lists (proxied + cached, §9)
GET    /api/srd/races/{index}
GET    /api/srd/classes            + /classes/{index}
GET    /api/srd/ability-scores
GET    /api/srd/skills
GET    /api/srd/equipment          + /equipment-categories
GET    /api/srd/spells             ?level=1&school=evocation (mirrors SRD filters)
GET    /api/srd/features|traits    + associated index detail
GET    /api/srd/monsters           GM reference (later stage)
POST   /api/characters/compile     validate draft + compute derived stats (pure)
POST   /api/characters/generate    random quick-build (valid draft + preview)
GET    /api/users/me/characters
POST   /api/users/me/characters    create character (game + sheet payload)
GET    /api/users/me/characters/{id}
WS     /ws                          STOMP endpoint; topics as in §6
```

`✓` = implemented in `feature/spring-security` (Draft v0.4); the web client for auth is done in
`feature/frontend-auth` (register/login/verify pages + protected dashboard). The rest is pending.
Unverified users get `403` on login until `/api/auth/verify` confirms their email; the
`resend` endpoint is intentionally enumeration-safe (always `202`).

## 13. Phased Roadmap

| Stage | Scope | Exit criteria |
|---|---|---|
| 1. Sessions & chat | accounts, lobby, invite code, join/leave, live chat + presence | group can get in a room and talk |

> Stage 1 status: the **accounts/auth** slice is done end-to-end (register, login, verify,
> roles, JWT, web UI + app shell — Draft v0.5). Remaining: lobby, invite code, join/leave,
> chat + presence over STOMP.
| 2. Characters | abstract `Character`, registry, D&D sheet model + **generation** (guided wizard + quick-build) backed by the SRD proxy, server compile validation | create a validated level 1–3 D&D character via wizard or quick-build |
| 3. Game table | dice rolls, initiative/order, shared table state | dice events broadcast to the session |
| 4. Discord | OAuth connect + deep-link voice | "Connect Discord" flows to voice + table side-by-side |
| 5. Production | PostgreSQL profile, migrations, deploy | runs on Postgres behind CI |

## 14. Open Questions / Open Decisions

**Decided so far:** D&D 5e-first MVP · user accounts · Discord VoIP = OAuth + companion
client · SRD integration = backend live proxy + cache over REST · character generation =
guided wizard + quick-build, house-rule d20 scores assigned by the player, starting
levels 1–3, client-draft + pure server compile · **auth (implemented):** login by
username **or** email, age ≥ 13 at signup, strict password policy (≥ 8 chars with upper,
lower, digit, special), 24h JWT with no refresh token, single-use 24h email-verification
tokens with a 60s resend cooldown, roles `USER`/`MODERATOR`/`ADMIN` with a dev-seeded
bootstrap admin · **frontend (implemented):** Tailwind CSS v4, react-router, JWT in
`localStorage` restored via `GET /api/users/me`, Node 24 pinned, backend CORS restricted to
the configured `tabletopserv.cors.allowed-origins` (default the Vite dev origin); no Vite
`/api` proxy — the SPA calls the backend cross-origin with `VITE_API_URL`.

- Do we need friends list / permanent groups, or is invite-code enough for now?
- Should board/map/tokens be a stage after MVP, or explicitly out of scope?
- Exact D&D 5e sheet fields — confirm which sets matter for v1.
- Dice rolls: server-authoritative only, or allow GM-private rolls with reveal?
- Deployment target (containers? platform?), and whether Flyway migrations start in
  Stage 5 or earlier.
- Room persistence: sessions archived/joinable later, or ephemeral?
- SRD: keep the live proxy, or eventually mirror 5e-bits data into our own DB?
- SRD version pinning: stay on `2014` — when to consider the `2024` ruleset?
- SRD multilingual (`?lang=`): worth supporting beyond English?
- Score sources: add standard array / point-buy / 4d6 alongside the house-rule d20?
- House-rule d20: always on, or a configurable table/room option?
- Beyond level 3: leveling up existing characters (not just creating at 1–3)?

## 15. Tech Notes (existing repo context)

- Backend: Spring Boot 4.1.1, Java 21, package `com.gamer.fowever.tabletopserv`, JAR
  packaging (no servlet container), `spring-boot-starter-webmvc` / `webflux` present.
- Frontend: Vite 8 + React 19, plain JSX, oxlint, Vitest. Tailwind CSS v4
  (`@tailwindcss/vite`), react-router, Node 24 (`tabletopweb/.nvmrc`, `engines`, CI).
- Backend CORS: `CorsConfigurationSource` bean wired into the Security filter chain for
  `/api/**`, origins from `tabletopserv.cors.allowed-origins`
  (env `CORS_ALLOWED_ORIGINS`, dev default `http://localhost:5173`, prod default empty).
- CI: `node.js.yml`, `maven.yml`, `maven-publish.yml` (see `AGENTS.md`).
- Dice/rolls (incl. the d20 score rolls) use a cryptographically secure RNG
  (server-side `java.security.SecureRandom`).
- **Dependencies now present:** JPA (+ `-test`), JDBC, security (+ `security-test`),
  validation, mail, webflux/webmvc (+ test starters), websocket, cache + caffeine,
  H2 + PostgreSQL (runtime), jjwt 0.12.6 (JWT signing), and jacoco with a ≥ 90% line
  coverage gate on the `test` phase.
- **Auth stack in place:** stateless JWT filter chain, BCrypt, roles
  `USER`/`MODERATOR`/`ADMIN` on `User`, email verification via `EmailVerificationToken`
  (`ConsoleEmailSender` in dev, SMTP in prod), and a dev-only bootstrap admin
  (credentials from `tabletopserv.admin.*` props, overridable via env). Explicit JSON
  `401`/`403` responses; business errors handled by `GlobalExceptionHandler`.
- Still to build for stage 1-2 runtime: STOMP broker config + presence/chat wiring, and
  the SRD `SrdClient` WebClient proxy with the Caffeine cache.