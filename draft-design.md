# Fuzzy Happiness — Tabletop Game Platform (Initial Design Draft)

> **Status:** Draft v0.1 — a starting point to iterate on as requirements become clearer.
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
  login/register, lobby, session (chat + table), character sheets.
- **Real-time:** server-authoritative. Clients send commands; the server updates state
  and broadcasts events to everyone subscribed to that session's topic.
- **Auth:** `spring-security` (already a dependency) for accounts; JWT bearer for REST +
  session token on WebSocket handshake.
- **Discord VoIP:** Discord has **no public API to programmatically join a voice
  channel** — see [§8 Discord VoIP Integration](#8-discord-voip-integration).

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
| `User` | id, display name, email, password hash, avatar |
| `Game` | registry entry: slug (`dnd-5e`), display name, sheet schema |
| `Character` | **abstract** base: id, name, portrait, description/backstory, game version. Contains only fields that translate across games. Serialized as a discriminated (TPH) entity per game. |
| `Dnd5eCharacter` | concrete D&D fields: ability scores, class, level, race, HP, AC, skills ... |
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
- **MVP ships D&D 5e only**; plug-in path documented so future games (e.g. Call of
  Cthulhu, Pathfinder) reuse the base.

## 8. Discord VoIP Integration

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

## 9. Database Strategy (H2 dev / PostgreSQL prod)

- **JPA entities** as in §5.
- **Spring profiles:**
  - `dev` (default): in-memory/file **H2**, `ddl-auto: update`, sample seed data.
  - `prod`: **PostgreSQL**, `ddl-auto: validate + Flyway migrations` (add later as a
    tracked, deliberate step), credentials via env vars, never in-repo.
- **Dependencies to add:** `com.h2database:h2` (runtime), `org.postgresql:postgresql`
  (runtime).
- Schema migrations deferred until the model stabilizes; until then H2 update mode is
  fine for iteration.

## 10. Proposed API Surface (initial)

```
POST   /api/auth/register          create account
POST   /api/auth/login             obtain token
POST   /api/sessions               create session (returns invite code)
GET    /api/sessions/{id}          snapshot (participants, game, status)
POST   /api/sessions/join          join by invite code
POST   /api/sessions/{id}/leave
GET    /api/games                  registered games + sheet schemas
GET    /api/users/me/characters
POST   /api/users/me/characters    create character (game + sheet payload)
GET    /api/users/me/characters/{id}
WS     /ws                          STOMP endpoint; topics as in §6
```

## 11. Phased Roadmap

| Stage | Scope | Exit criteria |
|---|---|---|
| 1. Sessions & chat | accounts, lobby, invite code, join/leave, live chat + presence | group can get in a room and talk |
| 2. Characters | abstract `Character`, registry, D&D sheet model + editor | create/read a D&D character via UI |
| 3. Game table | dice rolls, initiative/order, shared table state | dice events broadcast to the session |
| 4. Discord | OAuth connect + deep-link voice | "Connect Discord" flows to voice + table side-by-side |
| 5. Production | PostgreSQL profile, migrations, deploy | runs on Postgres behind CI |

## 12. Open Questions / Open Decisions

- Do we need friends list / permanent groups, or is invite-code enough for now?
- Should board/map/tokens be a stage after MVP, or explicitly out of scope?
- Exact D&D 5e sheet fields — confirm which sets matter for v1.
- Dice rolls: server-authoritative only, or allow GM-private rolls with reveal?
- Deployment target (containers? platform?), and whether Flyway migrations start in
  Stage 5 or earlier.
- Room persistence: sessions archived/joinable later, or ephemeral?

## 13. Tech Notes (existing repo context)

- Backend: Spring Boot 4.1.1, Java 21, package `com.gamer.fowever.tabletopserv`, JAR
  packaging (no servlet container), `spring-boot-starter-webmvc` / `webflux` present.
- Frontend: Vite 8 + React 19, plain JSX, oxlint, Vitest.
- CI: `node.js.yml`, `maven.yml`, `maven-publish.yml` (see `AGENTS.md`).
- To implement Stage 1-2 the immediate additions are `spring-boot-starter-websocket`,
  `h2`, `postgresql`, and an auth filter/JWT dependency.