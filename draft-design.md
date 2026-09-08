# Fuzzy Happiness — Tabletop Game Platform (Initial Design Draft)

> **Status:** Draft v0.9 — accounts/auth end-to-end (backend `feature/spring-security` + web UI
> in `feature/frontend-auth`), Stage 1 "Sessions & chat" (lobby with invite codes,
> create/join/leave, live chat + presence over STOMP, in `feature/sessions`), the first
> slice of Stage 3 "Game table": a **grid battle map** with tokens, movement budget, and turn
> control in `feature/battle-map`, and the second slice — server-authoritative **dice
> (public + GM-private)** and a per-map **initiative order** — in `feature/dice-initiative`.
> Draft v0.9 adds a **design flush** for three areas under research (no code yet, in
> `feature/gateway-monster-3d`): homebrew **monster generation** by replicating a deterministic
> CR→statblock "chassis" math engine (§9b), a dedicated **Express egress gateway** for all
> outbound/upstream calls (§16), and an optional **3D view** of the battle map via React Three
> Fiber (§17). A starting point to iterate on as requirements become clearer.
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
                          │                 Frontend (SPA)                │
                          │        React 19 + Vite (tabletopweb/)         │
                          │  Lobby │ Session view │ Sheet editor │ Auth   │
                          │  2D battle map │ (future) 3D viewport (R3F)   │
                          └───────┬──────────────┬──────────────┬────────┘
                                  │ REST (CRUD)  │  WS/STOMP    │
         ┌──────────┐             ▼              ▼              ▼         ┌────────────────┐
         │ Discord  │  OAuth /   ┌─────────────────────────────────────┐  │    Database     │
         │ (voice)  │  deep-link │          Spring Boot Backend        │  │                 │
         │ client   │◄───────────┤    (tabletopserv/, Java 21, Boot 4) │  │  Profile-based: │
         │  runs in │            │ REST controllers │ STOMP broker      │  │   dev  → H2     │
         │ user OS  │            │ SessionService  │ Auth/accounts      │  │ prod → Postgres │
         └──────────┘            │ Game registry   │ Character service  │  └────────────────┘
                                 └─────────────────────────────────────┘
                                         │  outbound (server-held keys)
                                  ┌──────▼───────────────┐
                                  │  Express egress GW   │   (tabletopgateway/, §16)
                                  │  SRD │ LLM │ future   │
                                  └──────┬───────────────┘
                                         ▼  internet upstreams
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

## 9b. Homebrew Monster Generation (CR-driven statblock engine)

**Research note (decided):** the [Cros.land AI statblock generator](https://cros.land/ai-powered-dnd-5e-monster-statblock-generator/)
was evaluated as a source for homebrew monster generation and **rejected as a direct API
integration** — it is a client-side tool with **no public REST API**. It saves monsters to
browser `localStorage`, exposes only exports (Homebrewery markdown, Foundry VTT, Improved
Initiative JSON, and a Roll20 Chrome extension), gates generation behind a daily limit
(5 free/24h; $5/mo Patreon), and there is no documented HTTP endpoint a backend proxy could
call. We therefore **replicate its approach rather than consume it**: implement the same
deterministic "chassis" math engine server-side, and drive the flavor/ability text through
**our own LLM** provider, all reached via the egress gateway (§16).

**The math engine (deterministic, non-AI):** given a **Challenge Rating** and a **combat
role**, every number is computed on curves calibrated against the published SRD monsters —
the AI never chooses numbers:

- Baselines derived from CR: **Armor Class**, **hit points**, **attack bonus**, and **save
  DC** follow curves fitted to official monsters; **total damage per round (DPR)** follows
  the DMG's damage bands.
- A **multiattack** splits that budget across attacks; an **area effect** gets about half;
  a once-per-fight **nova** can spend most of it; **legendary** monsters deal ~60% of budget
  on their own turn and the rest via legendary actions.
- **Combat roles** shift the baselines while XP stays tied to CR:

  | Role | Math adjustment |
  |---|---|
  | Balanced | raw baseline |
  | Brute | −1 AC, ~+25% HP, harder single hits |
  | Defender | +2 AC, +HP, −~15% damage, usually a protection reaction |
  | Skirmisher | baseline + a movement trick (bonus-action disengage, flyby, burrow) |
  | Artillery | thin defenses, long-range attacks, an escape tool |
  | Controller | +1 save DC, −~30% damage; restrains/slows/blinds |
  | Lurker | fragile/weak in a stand-up fight; an opening strike spends the missing damage at once |
  | Support | buffs/heals/enables; deliberately underperforms CR solo |
  | Swarm | Tiny mass w/ weapon resistances, +HP, damage that halves below half HP |

  An **Auto** role picks the role from the concept/name (e.g. "tomb guardian" → defender,
  "dune ambusher" → lurker). Save DCs always derive from `8 + proficiency + ability modifier`.

- **Validation ("linter" + "audit"):** every generated statblock passes an automated review
  (~90 checks calibrated so no official SRD monster trips them): an area effect that deals
  damage cannot also stun without a recharge gate; a grapple must carry an escape DC; a
  restrained condition needs a way out; Prone never takes a duration; a damage rider is extra
  dice rather than a flat bonus; no entry references an ability that does not exist. The linter
  stamps in deterministic fixes where possible; the audit must **quote exact text** for each
  finding (a hallucinated complaint is thrown away), and remaining issues go back to the LLM as
  a short, specific problem list.

**2014 vs 2024:** one math engine produces identical numbers for both editions; only the
*wording/layout* differs (2024 prints Initiative on the block, an ability MOD/SAVE grid, merged
immunities, Bonus Action section, "Emanation"/"Bloodied" vocabulary). We render the statblock
in the classic 2014 layout first, with a 2024 toggle later.

**Persistence & reuse (R11):** homebrew monsters are persisted as a `Monster` entity (CR, role,
edition, SRD `index` references, cached display snapshot) owned by the creating GM, reusable
across encounters/sessions.

**Surface (via the gateway, §16):**
```
POST /api/monsters/generate    {name?, cr, role|auto, edition?, concept?} -> statblock
GET  /api/monsters/mine        my homebrew monsters
```

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
POST   /api/sessions               create session (returns invite code)      ✓
GET    /api/sessions/{id}          snapshot (participants, game, status)      ✓
POST   /api/sessions/join          join by invite code                        ✓
POST   /api/sessions/{id}/leave                                               ✓
GET    /api/games                  registered games + sheet schemas           ✓
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
POST   /api/monsters/generate      homebrew statblock from CR + role (via gateway, §9b)
GET    /api/monsters/mine          my homebrew monsters
WS     /ws                          STOMP endpoint; topics as in §6
```

`✓` = implemented. Auth landed in `feature/spring-security` (Draft v0.4, web client in
`feature/frontend-auth`); the sessions slice landed in `feature/sessions` (Draft v0.6).
Unverified users get `403` on login until `/api/auth/verify` confirms their email; the
`resend` endpoint is intentionally enumeration-safe (always `202`). All outbound SRD +
monster-generation calls exit the backend via the Express egress gateway (§16) — the
`/api/srd/*` and `/api/monsters/*` controllers are frontends for gateway-backed data.

**STOMP surface (sessions + dice slice, implemented):**

```
SUB   /app/sessions/{id}           @SubscribeMapping → SessionSummary snapshot replay
SUB   /topic/sessions/{id}         live SessionEventDto broadcast (PRESENCE / CHAT / DICE / TABLE)
SEND  /app/sessions/{id}/chat      POST {text} from a participant → CHAT event on the topic
SUB   /user/queue/dice             GM-private dice results (full frame, only to the rolling GM)
CONNECT /ws?token=<jwt>            standard WebSocket handshake; token = JWT auth
```

The WebSocket handshake (`/ws/**` is `permitAll()` in the security filter chain) is
authenticated purely by the `token` query param, which `TokenHandshakeHandler` resolves to
the STOMP principal (an `AuthenticatedUser` whose name is the **username**, which user
destinations like `/user/queue/dice` route on). A `StompAuthChannelInterceptor` (registered
on the **client inbound** channel) enforces: a principal must exist (`Authentication
required`), the user must be a participant of the session for `/topic/sessions/{id}` and
`/app/sessions/{id}/*` (`You are not a participant of this session`), and only those
destinations plus `/user/queue/*` are subscribable (`Unsupported subscription
destination`). Rejections surface to clients as **STOMP ERROR frames** — which requires
(a) running the client inbound channel **inline**
(`SyncTaskExecutor`) so interceptor exceptions propagate to the `StompSubProtocolHandler`,
and (b) throwing `MessageDeliveryException` (a `MessagingException`) so
`AbstractMessageChannel` rethrows the original message instead of wrapping it in the
generic `Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]` text.

Snapshot replay semantics: subscribing to `/app/sessions/{id}` (the `@SubscribeMapping`)
returns the current `SessionSummary`; clients treat that as the initial roster + recent
event history, then apply the `/topic/sessions/{id}` stream on top. Every `SessionEvent`
(PRESENCE from join/leave, CHAT, and DICE/TABLE) is persisted and replayed to late
joiners via `recentEvents` — except **GM-private dice**, which are deliberately **not**
persisted (the GM's full result arrives on `/user/queue/dice`, the table only ever saw the
hidden frame).

## 13. Phased Roadmap

| Stage | Scope | Exit criteria |
|---|---|---|
| 1. Sessions & chat | accounts, lobby, invite code, join/leave, live chat + presence | group can get in a room and talk |

> Stage 1 status: **complete** (Draft v0.6). Accounts/auth end-to-end (register, login,
> verify, roles, JWT, web UI + app shell), plus the sessions slice in `feature/sessions`:
> lobby with create/join-by-invite-code, session screen with participant roster + live chat
> and presence over STOMP (private-by-membership topics, snapshot replay on subscribe).
> Stage 3 (first slice): **complete** (Draft v0.7). The grid battle map lives in
> `feature/battle-map` — see the "Decided" notes in §14.
> Stage 3 (second slice, dice + initiative): **complete** (Draft v0.8). Server-authoritative
> dice (`POST /api/sessions/{id}/roll`) with public + GM-private rolls, and a per-map
> initiative order (`…/map/initiative`) in `feature/dice-initiative` — see §14.
> Draft v0.9: **design flush** (no code) in `feature/gateway-monster-3d` for the Express
> egress gateway (§16), homebrew monster generation (§9b), and the optional 3D viewport (§17).
| 2. Characters | abstract `Character`, registry, D&D sheet model + **generation** (guided wizard + quick-build) backed by the SRD proxy, server compile validation | create a validated level 1–3 D&D character via wizard or quick-build |
| 3. Game table | dice rolls ✓, initiative/order ✓, shared table state — battle map track 1 (grid, tokens, per-turn movement budget) ✓: see §14; 3D viewport (R3F) is a later enhancement to this stage (§17) | grid battle map synced, movement budget, server dice (public + GM-private hidden frames), initiative order with auto d20 |
| 3b. Gateway + monsters | Express egress gateway (SRD all calls exit through it) + homebrew monster generation (§9b) — **enables** Stage 2's SRD-backed wizard | all outbound calls flow through the gateway; CR+role → valid statblock, persisted `Monster` reused across sessions (R9-R11) |
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
`/api` proxy — the SPA calls the backend cross-origin with `VITE_API_URL` · **sessions &
live chat (implemented):** STOMP over `/ws` with the JWT in a `?token=` query param
(handshake principal + channel interceptor), snapshot replay via `@SubscribeMapping` on
`/app/sessions/{id}` + live `SessionEventDto` broadcasts on `/topic/sessions/{id}`,
per-membership subscription enforcement (interceptor throws `MessageDeliveryException` with
the client inbound channel running on `SyncTaskExecutor` so clients receive STOMP ERROR
frames), invite codes `[A-Z0-9]{6}`, roles `GM`/`PLAYER`/`SPECTATOR` (invite join assigns
`PLAYER`, creator `GM`), session status `OPEN`/`ACTIVE`/`CLOSED`, `BootstrapGameRunner`
seeding the `dnd-5e` game, frontend uses `@stomp/stompjs` via `src/lib/stomp.js` with the
lobby + session screens in `src/pages/LobbyPage.jsx` / `SessionPage.jsx` · **battle map
(implemented):** one grid map per session (`BattleMap`, unique `session_id`) at 24×18 by
default with `squareFeet = 10` per square; auto-token for every non-spectator participant
(linked by `linkedUserId`, name = display name) when the GM creates the map; GM-added tokens
default to `MONSTER_NPC` / 30 ft; movement budget per turn = `floor(speedFeet / squareFeet)`
squares (starter race→speed table: 25/30/35/40 ft), diagonal moves cost Chebyshev distance,
and a token may only move while `movedFeet + cost ≤ speedFeet` — budget resets on turn
`START`, `END`, or `NEW_ROUND`; permissions: GM manages any token, players move only their
own linked token, spectators read-only; every mutation persists a `SessionEvent` of a new
`TABLE` type carrying the full `BattleMapDto` and broadcasts it on `/topic/sessions/{id}`
(clients replace map state from the payload; TABLE rows are filtered out of the chat feed);
REST surface `GET|POST /api/sessions/{id}/map`, `POST|PATCH|DELETE …/map/tokens[/{tokenId}]`,
`POST …/map/tokens/{tokenId}/move`, `POST …/map/turn`; implemented in `feature/battle-map`.

**Dice & initiative (implemented):** dice are **server-authoritative** (secure `SecureRandom`
RNG) over `POST /api/sessions/{id}/roll` with grammar `(\d+)?d(\d{1,3})([+-]\d{1,3})?` — up to
20 dice, 999 sides, modifier −100..+100; expressions are normalized (lowercased, one-die count
omitted) before rolling. Public rolls persist a `DICE` `SessionEvent` (id set, `hidden:false`)
and broadcast the full result on `/topic/sessions/{id}`. **GM-private rolls** (GM-only; the GM
passes `privateRoll:true`) broadcast a *hidden* frame on the topic (roller, expression, label —
no `rolls`/`total`) and route a full frame only to the rolling GM's `/user/queue/dice` (via
`convertAndSendToUser` on the username-based principal); the REST response carries the full
frame with `id:null` and private rolls are **never persisted**, so late joiners only ever see
public history. The frontend merges the hidden topic frame + full user-queue frame by `rollId`.
Initiative is one ordered list per map (`initiative_entries`, `initiativeIndex` on the map,
`-1` = none current): `POST …/map/initiative` replaces the order from `entries` (`label`
**xor** `tokenId`, ≤ 30 entries, explicit score 1..999 or blank = auto d20), resetting the
pointer to `-1`; `POST …/map/initiative/{entryId}/reroll` re-rolls one d20 score;
`POST …/map/initiative/next` advances the pointer (wrapping) and — when the entry is a token —
activates it as the current turn (resets its movement budget); `DELETE …/map/initiative/{entryId}`
removes an entry (pointer clamped). GM-only, GM overrides/rerolls allowed; movement stays
independent of the order (not time-gated). `BattleMapDto` carries `initiative` + `initiativeIndex`
and rides the existing `TABLE` broadcasts. Implemented in `feature/dice-initiative`.
**Monster generation (decided, §9b):** replicate the Cros.land "chassis" approach server-side
(deterministic CR→statblock math engine calibrated to the SRD + combat-role variants, an
LLM only for flavor/abilities, a linter + quote-verbatim audit), persisted `Monster` entity
owned by the creating GM, exposed via `POST /api/monsters/generate` (not a direct Cros.land
integration — it has no public API) · **egress gateway (decided, §16):** a dedicated Express
service (`tabletopgateway/`, Node 24, `http-proxy-middleware`) sits **behind** Spring as the
only path out to upstreams (SRD, monster-gen LLM, future integrations); curated route table,
deny-by-default, server-held keys, SSRF guard, timeouts, correlation IDs; the backend's own
`SrdClient`/`GatewayClient` calls it · **3D rendering (decided, §17):** React Three Fiber +
drei as an optional viewport sharing the existing 2D `BattleMapDto` state (same
server-authoritative tokens/movement), glTF models for avatars/monsters, WebGL2 now with the
WebGPU renderer as the future path.

- Do we need friends list / permanent groups, or is invite-code enough for now?
- Exact D&D 5e sheet fields — confirm which sets matter for v1.
- ~~Dice rolls: server-authoritative only, or allow GM-private rolls with reveal?~~
  **Decided:** server-authoritative, with GM-private rolls delivering the full result only to
  the GM (hidden frame for the rest of the table).
- Deployment target (containers? platform?), and whether Flyway migrations start in
  Stage 5 or earlier.
- Room persistence: sessions archived/joinable later, or ephemeral?
- SRD: keep the live proxy, or eventually mirror 5e-bits data into our own DB?
- SRD version pinning: stay on `2014` — when to consider the `2024` ruleset?
- SRD multilingual (`?lang=`): worth supporting beyond English?
- Score sources: add standard array / point-buy / 4d6 alongside the house-rule d20?
- House-rule d20: always on, or a configurable table/room option?
- Beyond level 3: leveling up existing characters (not just creating at 1–3)?
- Monster-generation LLM provider: which vendor/key to standardize on for the gateway (§9b)?
- Monster generation: build the "chassis" curves from SRD ourselves, or vendor an off-the-shelf
  statblock math library?
- 3D: which glTF asset source/style for avatars/monsters, and do we ship a bundled starter pack (§17)?

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
- **Sessions stack in place:** STOMP over `/ws` (`spring-boot-starter-websocket`), client
  inbound channel on `SyncTaskExecutor` + `StompAuthChannelInterceptor` (throws
  `MessageDeliveryException`), `TokenHandshakeHandler` for the `?token=` handshake param,
  `SessionEventsController` (`@SubscribeMapping` snapshot replay) + `SessionController`
  (create/join/leave) + `GameController`; `SessionService`/`SessionPresenceService` persist
  `Session`/`Participant`/`SessionEvent` and broadcast on `/topic/sessions/{id}`.
- Still to build (Stage 2-3 runtime): the Express egress gateway + `GatewayClient` (§16),
  the SRD proxy path rewired through it, the custom `Character` model + generation (§8),
  homebrew monster generation (§9b), and the optional 3D viewport (§17).

## 16. Egress API Gateway (Express)

**Why:** every external/upstream call — the SRD proxy (§9), monster-generation LLM (§9b), and
future integrations (Discord OAuth, image/3D asset services) — leaves the backend. Instead of
letting the Spring backend open connections to arbitrary hosts, all outbound traffic flows
through a dedicated gateway that enforces allowlisting, secrets, and timeouts in **one** place.

**Decision:** a new **egress proxy service** sits **behind** the Spring backend
(`tabletopgateway/`, Express + Node 24 + `http-proxy-middleware`), on the *outbound* path:

```
Frontend (React SPA)
      │  REST /api/**  +  STOMP /ws?token=…    (unchanged)
      ▼
Spring Boot backend (authoritative: auth, sessions, SRD allowlists, character/monster logic)
      │  server-to-server, server-held API keys
      ▼
Express egress gateway  ──►  internet upstreams
   • dnd5eapi.co SRD
   • LLM provider (monster-gen flavor/abilities)
   • future: Discord OAuth, asset/image services
```

- The gateway **does not see client traffic** — only the Spring backend calls it, so only
  server-held credentials/keys are present there. (Ingress stays as today: SPA → Spring
  directly; no Vite `/api` proxy, cross-origin via `VITE_API_URL`.)
- **Curated route table:** each upstream is a named route with its target URL; anything not in
  the table is refused (deny-by-default).
- **Per-route controls:** allowlisted paths, API key / header injection (server supplies the
  secret), response timeout, retry policy, response size cap, and optional response
  caching (Caffeine moves server-side to the gateway for SRD list endpoints, or stays in
  Spring — either way, one cache owner).
- **SSRF protection:** the gateway validates target hostnames against the route table and
  blocks private/link-local/metadata IP ranges.
- **Observability:** structured request/response logs + correlation ID per forwarded call so a
  chain (client → Spring → gateway → upstream) is trailable.
- Spring keeps a thin `GatewayClient` (WebClient) that forwards curated requests and maps
  gateway errors to the same `GlobalExceptionHandler` shapes.

**Recommended package:** `http-proxy-middleware` (RFC-compatible, Express middleware, supports
path rewrite + `onProxyReq`/`onProxyRes` hooks + WebSocket upgrade for future needs). Replace
the current direct `SrdClient` WebClient→dnd5eapi.co path so **all** outbound calls use the
gateway — including the SRD proxy and monster-gen LLM.

## 17. 3D Rendering (avatars, board, monsters)

**Decision:** React Three Fiber (R3F) + drei, wrapping Three.js declaratively to match the
existing React 19 + plain JSX stack. Added as an **optional 3D viewport** sharing the exact
same state as the existing 2D grid map (same `BattleMapDto`, tokens, movement, turn) — the 3D
layer is a camera/view on the same server-authoritative state, not a parallel system.

- **Dependencies:** `three`, `@react-three/fiber`, `@react-three/drei` (Node 24, plain JSX,
  matching the existing frontend; no TypeScript).
- **Renderer:** WebGL2 today (covers ~97%+ browsers); Three.js r182+ makes
  `WebGPURenderer` the recommended renderer with automatic WebGL fallback — wire WebGPU via
  R3F's async `gl` prop when our needs justify it (later slice).
- **Scene content:**
  - **Player avatars** — a low-poly character mesh per player token (drawn from the same
    token list; reuse the existing auto-token name/`linkedUserId` mapping).
  - **Game board** — a rendered floor plane derived from the `BattleMap` grid (24×18 default,
    `squareFeet` scale), walls/obstacles later.
  - **Monsters** — meshes for monster/NPC tokens.
- **Models:** standard glTF/GLB assets (free packs; e.g. a shared asset library in
  `tabletopweb/public/models/`), loaded via drei's `useGLTF`, cached across components.
- **Interaction:** drei `OrbitControls` for the camera; selection/movement gestures mapped to
  the existing `POST …/map/tokens/{tokenId}/move` (server still authoritative). No client-side
  physics in v1.
- **Level of detail (LOD):** drei performance controls to keep 60fps on weak devices; the 2D
  map remains the guaranteed playable path with the 3D view as an enhancement.
- **Roadmap fit:** a later slice ("3D battle-map viewport") on Stage 3, *after* the SRD proxy
  (gateway) and character/monster generation land — see §13.