# Fuzzy Happiness — Tabletop Game Platform (Initial Design Draft)

> Draft v0.15 (in `feature/auth-hardening`) ships the **auth hardening batch (R15–R18, R20)**:
> a themed sign-in/sign-up background (R15), password typed twice on registration (R16),
> a separate required **real name** field distinct from username and display name (R17),
> **PII encryption at rest** — email, display name, real name and date of birth are
> AES-256-GCM field-level encrypted with a deterministic **email blind index (`emailKey`,
> HMAC-SHA256)** backing uniqueness and lookup (R18), and the **bootstrap admin** now seeds on
> all profiles (guarded on a non-blank `tabletopserv.admin.password`), enabling production
> seeding via `ADMIN_USERNAME`/`ADMIN_EMAIL`/`ADMIN_PASSWORD` (R20). Frontend register/login
> gain the real-name + confirm-password fields and the themed background. `email` is no longer
> unique in the DB — a new `email_key` column (unique) carries the deterministic blind index.
> **R19 (social OAuth login) is deferred to a follow-up PR** until provider creds exist.
> Draft v0.13 (in `feature/functional-tests`) brings the **functional/E2E testing module
> (`tabletopfunctionaltest`) live (§18)**: the packaged `tabletopservice` JAR boots as a
> subprocess against a Testcontainers Postgres and a recorded-fixture gateway stub, with
> `*IT` journey suites (auth, session/STOMP, dice, battle map/initiative, monster, SRD) bound
> to `./mvnw verify` and CI (`maven.yml` runs `verify` on every push/PR to `develop`).
> Jenkins stays advisory-only.
> Draft v0.14 (in `feature/chargen`) ships **character generation (R13, §8)** end-to-end: a
> draft→compile→finalize server flow (CharacterApi + CharacterService + ChargenRules, 200
> CompileResult with violations when illegal, persisted snapshot) with functional coverage
> (CharacterJourneyIT, 6 journeys), plus the frontend character builder — `CharactersPage` (list
> + quick-build "surprise me"), `CharacterWizardPage` (10-step guided wizard with client caps
> mirroring the server) and `CharacterSheetPage`. The SRD allowlists are completed (gateway +
> SrdClient include `backgrounds`). The same PR prepares **Railway auto-deploy (R14, §19)** by
> wiring services to the GitHub repo source with per-service `rootDirectory`.
> Draft v0.15 (in `feature/chargen-wizard-rolls`) hardens the **character wizard (R23/R25, §8)**:
> rolled score sources are enforced — the server owns the dice (`POST /api/characters/roll-scores`)
> and the wizard's score inputs are locked until a roll lands — while standard-array and
> point-buy stay constrained assignment with client caps. The wizard now tracks **base scores**
> and applies the **race ability bonus** when the race is chosen, so characters with racial
> bonuses compile without manual score inflation. Starting equipment becomes a **gold-budget
> shop**: the class's PHB starting wealth is the purse, every item shows its SRD price, and the
> sheet carries `startingGoldGp` / `spentGoldGp` (snapshot-only — no schema change), with the
> server compile rejecting over-budget kits. Expanded classes beyond the SRD core (R24) stay
> deferred pending a data-source/licensing decision (§8).
> **Status:** Draft v0.10 — accounts/auth end-to-end (backend `feature/spring-security` + web UI
> in `feature/frontend-auth`), Stage 1 "Sessions & chat" (lobby with invite codes,
> create/join/leave, live chat + presence over STOMP, in `feature/sessions`), the first
> slice of Stage 3 "Game table": a **grid battle map** with tokens, movement budget, and turn
> control in `feature/battle-map`, and the second slice — server-authoritative **dice
> (public + GM-private)** and a per-map **initiative order** — in `feature/dice-initiative`.
> Draft v0.9 added a **design flush** (in `feature/gateway-monster-3d`): homebrew **monster
> generation** by replicating a deterministic CR→statblock "chassis" math engine (§9b), a
> dedicated **Express egress gateway** for all outbound/upstream calls (§16), and an optional
> **3D view** of the battle map via React Three Fiber (§17).
> Draft v0.10 (in `feature/backend-modules-gateway`) ships a **backend restructuring** — the single
> Spring module becomes a **multi-module Maven reactor** (`tabletopapi` = inbound REST interfaces +
> DTOs only, `tabletopservice` = implementations + runtime, `tabletopfunctionaltest` blank, §18) —
> plus the first outbound integration: a **Node egress gateway** (`tabletopgateway/`, §16) and the
> **SRD rewire** through it (§9). A starting point to iterate on as requirements become clearer.
> Draft v0.11 (in `feature/monster-generation`) ships **homebrew monster generation (R9/R11, §9b)**:
> a deterministic CR→statblock **math engine** (`MonsterMathEngine`), a persisted `Monster` entity
> owned by the creating GM, `POST /api/monsters/generate` (201) + `GET /api/monsters/mine`, and a
> GM-only generator panel on the session screen (statblock preview + add-to-map + "my monsters"
> reuse). Flavor is templated server-side; the LLM flavor route stays deferred (§9b).
> Draft v0.12 (in `feature/railway-deploy`) locks the **production target (Railway)** and aligns
> **character generation with the 2014 Player's Handbook**: three-service topology + managed
> Postgres with private networking (§19), prod port/datasource wiring, a health-check endpoint,
> Dockerfiles + **Infrastructure-as-Code** (`.railway/railway.ts`), and the PHB-standard ability-
> score sources (standard array default; point-buy and 4d6-drop-lowest selectable; the 6×d20
> house rule demoted to an optional score source — §8/§14).
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
         │ (voice)  │  deep-link │      Spring Boot Backend (multi-    │  │                 │
         │ client   │◄───────────┤  module tabletopserv/, Java 21,     │  │  Profile-based: │
         │  runs in │            │  Boot 4) — tabletopapi (interfaces  │  │   dev  → H2     │
         │ user OS  │            │  + DTOs) on top of tabletopservice  │  │ prod → Postgres │
         └──────────┘            │  (impls, domain, repos, security,   │  └────────────────┘
                                 │  STOMP). REST + STOMP broker        │
                                 └─────────────────────────────────────┘
                                         │  outbound (server-held keys)
                                  ┌──────▼───────────────┐
                                  │  Express egress GW   │   (tabletopgateway/, §16)
                                  │  SRD │ LLM │ future   │
                                  └──────┬───────────────┘
                                         ▼  internet upstreams
```

**Decisions:**

- **Backend:** Spring Boot 4 (Java 21), existing `tabletopserv/` as a **multi-module Maven
  reactor**: `tabletopapi` (inbound REST interfaces + DTOs, package
  `com.gamer.fowever.tabletopapi`) and `tabletopservice` (implementations + domain + runtime,
  `com.gamer.fowever.tabletopservice`). REST (via `spring-boot-starter-webmvc`) for
  queries/mutations + **STOMP over WebSocket** (`spring-boot-starter-websocket`) for real-time
  session events. See **§18** for the functional-test module.
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
- **Rules data:** the D&D plug-in consumes the open 5e-bits SRD API — **through the egress
  gateway** (§16) with caching — see [§9 D&D 5e SRD Integration](#9-dd-5e-srd-integration-5e-bits--dnd5eapico).
- **Deployment target (decided):** [Railway](https://railway.com) — three services (backend,
  egress gateway, static web) plus a managed **Postgres** database in one project, connected
  over private networking; see [§19 Deployment (Railway)](#19-deployment-railway).

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

**Ability scores (score sources):** the **2014 PHB standard array is the v1 default**
(15, 14, 13, 12, 10, 8 — PHB p.13). Selectable alternatives: **27-point point-buy**
(scores 8–15 per the PHB cost table), **4d6-drop-lowest** rolled six times, and the
existing house rule — **6 × d20**. **Rolls are server-authoritative (`SecureRandom`)
and the wizard enforces them**: picked a rolled method and the dice simply aren't handed
to the player — `POST /api/characters/roll-scores` returns the six **base scores** (a seeded
variant exists for functional tests) and the score inputs stay locked until a roll lands.
Standard array and point-buy are **constrained assignment** (the client mirrors the server's
legality check so Next stays gated). Choosing a race applies its **ability bonus** to the base
scores client-side — the draft the server compiles always carries final scores once the race
is added, which is how characters with racial bonuses stay legal.

**Starting gold & equipment (equipment shop):** starting equipment is **tied to the class's
starting wealth** and **bought**, not granted free. Each class gets a purse equal to the
**average of its PHB wealth-by-class pool** (PHB p.143 — e.g. fighter 5d4×10 gp averages
125 gp; needle-fine amounts are server-derived). The equipment step is a **budgeted shop**:
every SRD item shows its price (from the SRD `cost`, converted to gp), a running spent /
remaining total gates Next, the class's recommended starter kit (from the class record's
`starting_equipment` + first `starting_equipment_options` choice) is offered as one-click
auto-select, and **compile rejects any kit over budget**. Gold is carried on the sheet as
`startingGoldGp` / `spentGoldGp` **in the snapshot only** — no new DB columns, so nothing
drifts against prod's `ddl-auto=validate` schema.

**Starting level:** configurable **1–3** at creation. **Hit points follow the PHB (p.15):**
level 1 = max hit die + CON modifier, then the hit die + CON per level. **Proficiency
bonus is +2** (the PHB value across levels 1–4). Subclass timing and spell slots follow the
chosen class's rules (some subclasses start at class level 1, others at 3).

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

**Integration approach (decided): live proxy + cache over REST, through the gateway (§16).**

- **Backend only:** a `SrdClient` service (Spring `WebClient` — `webflux` already present in
  `tabletopservice`) calls the **egress gateway** (`tabletopgateway/`, §16), which forwards a
  curated, allowlisted set of endpoints to dnd5eapi.co. The frontend never calls dnd5eapi.co
  directly, and the backend never opens a raw connection to the internet (single place to
  cache/validate, one egress owner). `GatewayClient` carries the shared WebClient +
  `X-Gateway-Token`/correlation-id plumbing; `SrdClient` maps gateway errors to `ApiException`.
- **Caching:** the **gateway owns the SRD TTL cache** (long TTL on list endpoints — races,
  classes, ability scores, skills, spells, equipment). Sheet options stay fast after first
  fetch; Spring-side Caffeine remains for other uses (one cache owner).
- **Fallback:** if the gateway can't reach the SRD API, it serves the cold-cache-clear error
  `502 {status,message}`; the backend maps that to a "rules data unavailable" shape instead of
  a partial sheet.
- **Security:** no raw URL forwarding — the gateway route table only allows curated SRD
  collections and query params (`level`/`school`/`name`); `SrdClient` forwards only allowlisted
  paths and params upstream.

**Model linkage:** `Dnd5eCharacter` stores **SRD `index` references** (e.g.
`raceIndex`, `classIndex`, `subclassIndex`, `spellIndexes[]`, `featureIndexes[]`)
instead of copying rule data; the server validates a sheet's choices against SRD
resources so characters stay consistent with the rules.

**PHB authority vs SRD data:** the **2014 Player's Handbook** is the *rules authority* we
align mechanics to (§8, page-referenced); the SRD API is the *data feed*. The SRD covers
most PHB options but **not every subclass/feature** — v1 chargen offers only SRD-backed
choices rather than guessing at non-SRD data. We reference rule mechanics and page numbers
but never embed rulebook text (licensing; see §14).

## 9b. Homebrew Monster Generation (CR-driven statblock engine)

**Status: implemented** in `feature/monster-generation` (Draft v0.11). The deterministic
`MonsterMathEngine` (CR table + combat-role shifts, §9b) lives in
`tabletopservice/.../service/monster/`, monsters persist as a `Monster` JPA entity owned by the
creating GM, and the web generator is a GM-only panel on the session screen. The **LLM
flavor/abilities piece is deferred** for MVP — the engine emits templated, deterministic flavor
(description, traits, actions) locally, so generation is **fully local and requires no gateway
call**; when the LLM route arrives it will flow through the egress gateway (§16). More detail in
the "Decided" notes in §14.

**Research note (decided):** the [Cros.land AI statblock generator](https://cros.land/ai-powered-dnd-5e-monster-statblock-generator/)
was evaluated as a source for homebrew monster generation and **rejected as a direct API
integration** — it is a client-side tool with **no public REST API**. It saves monsters to
browser `localStorage`, exposes only exports (Homebrewery markdown, Foundry VTT, Improved
Initiative JSON, and a Roll20 Chrome extension), gates generation behind a daily limit
(5 free/24h; $5/mo Patreon), and there is no documented HTTP endpoint a backend proxy could
call. We therefore **replicate its approach rather than consume it**: the deterministic
"chassis" math engine is implemented server-side in Spring, and (future) flavor/ability text
goes through **our own LLM** provider behind the egress gateway (§16).

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
in the classic 2014 layout first, with a 2024 toggle later. Statblock numbers stay consistent
with **2014 PHB conventions** for player-facing values (HP, AC, ability modifiers, and the
+2 proficiency bonus for levels 1–3 PCs) while CR math follows the DMG curves above.

**Persistence & reuse (R11):** implemented. Homebrew monsters persist as a `Monster` entity (CR,
role, edition, cached statblock snapshot) owned by the creating GM (`monsters` table, `owner_id`),
reusable across encounters/sessions via the generator's "My monsters" list.

**Surface:**
```
POST /api/monsters/generate    {name?, cr, role|auto, edition?, concept?, seed?} -> statblock   (201) ✓
GET  /api/monsters/mine        my homebrew monsters                                                  ✓
```
Generation runs **locally in `tabletopservice`** (deterministic engine, no outbound call). The
future LLM flavor request will be the gateway-backed piece.

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
  - `prod`: **PostgreSQL** — on Railway a **managed Postgres service** reached over the
    private network (credentials from `PGHOST`/`PGPORT`/`PGDATABASE`/`PGUSER`/`PGPASSWORD`,
    never in-repo), `ddl-auto: validate` with **Flyway migrations** added in Stage 5 as a
    tracked, deliberate step (§19).
- **Dependencies to add:** `com.h2database:h2` (runtime, present), `org.postgresql:postgresql`
  (runtime, present).
- Schema migrations deferred until the model stabilizes; until then H2 update mode is
  fine for iteration.

## 12. Proposed API Surface (initial)

```
POST   /api/auth/register            create account                          ✓
POST   /api/auth/login               obtain token (username or email)        ✓
GET    /api/auth/verify?token=       confirm email (single-use, 24h)         ✓
POST   /api/auth/resend-verification resend verification (60s cooldown)      ✓
GET    /api/users/me                 current user profile (JWT)              ✓
PATCH  /api/users/me/username         change own username                       ✓
GET    /api/admin/users              admin-only user listing                 ✓
POST   /api/sessions               create session (returns invite code)      ✓
GET    /api/sessions/{id}          snapshot (participants, game, status)      ✓
POST   /api/sessions/join          join by invite code                        ✓
POST   /api/sessions/{id}/leave                                               ✓
GET    /api/games                  registered games + sheet schemas           ✓
GET    /api/srd/races              SRD reference lists (gateway-proxied + cached, §9/§16) ✓
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
POST   /api/monsters/generate      homebrew statblock from CR + role (deterministic, §9b) ✓
GET    /api/monsters/mine          my homebrew monsters                            ✓
WS     /ws                          STOMP endpoint; topics as in §6
```

`✓` = implemented. Auth landed in `feature/spring-security` (Draft v0.4, web client in
`feature/frontend-auth`); the sessions slice landed in `feature/sessions` (Draft v0.6); the
SRD slice landed in `feature/backend-modules-gateway` (Draft v0.10 — `/api/srd/*` now routes
through the egress gateway, §16); the monster slice landed in `feature/monster-generation`
(Draft v0.11 — §9b). Unverified users get `403` on login until
`/api/auth/verify` confirms their email; the `resend` endpoint is intentionally
enumeration-safe (always `202`). Outbound SRD calls exit the backend via the Express egress
gateway (§16) — the `/api/srd/*` controllers are frontends (api interfaces in `tabletopapi`,
impls in `tabletopservice`) for gateway-backed data; the `/api/monsters/*` endpoints are
fully local (deterministic engine in `tabletopservice`), with the future LLM flavor route as
the gateway-backed piece.

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
> Stage 2 foundation: **complete** (Draft v0.10) — the backend is now a **multi-module Maven
> reactor** (`tabletopapi` interfaces/DTOs + `tabletopservice` runtime, see §18), and the
> outbound path lands: **Express egress gateway** (`tabletopgateway/`) + the **SRD rewire**
> through it (`/api/srd/*` via `GatewayClient`/`SrdClient`), in `feature/backend-modules-gateway`.
| 2. Characters | abstract `Character`, registry, D&D sheet model + **generation** (guided wizard + quick-build) backed by the SRD proxy, server compile validation | create a validated level 1–3 D&D character via wizard or quick-build |
> Stage 2 status (characters): **complete** (Draft v0.14, `feature/chargen`) — backend
> chargen (draft → compile → finalize, `CharacterApi` + `ChargenRules`, functional tests via
> `CharacterJourneyIT`) and the web UI (list, wizard, quick-build, sheet, `/characters` routes)
> shipped; frontend 154 tests green, jacoco 92.87%.
| 3. Game table | dice rolls ✓, initiative/order ✓, shared table state — battle map track 1 (grid, tokens, per-turn movement budget) ✓: see §14; 3D viewport (R3F) is a later enhancement to this stage (§17) | grid battle map synced, movement budget, server dice (public + GM-private hidden frames), initiative order with auto d20 |
| 3b. Gateway + monsters | Express egress gateway ✓ (SRD flows through it, §16) + homebrew monster generation ✓ (deterministic engine, §9b — LLM flavor deferred) | all outbound calls flow through the gateway; CR+role → valid statblock, persisted `Monster` reused across sessions (R9-R11) |
| 4. Discord | OAuth connect + deep-link voice | "Connect Discord" flows to voice + table side-by-side |
| 5. Production | Railway deploy: managed Postgres (private network), three services, health checks, env via dashboard, Flyway migrations (§19) | runs on Railway behind CI |

## 14. Open Questions / Open Decisions

**Decided so far:** D&D 5e-first MVP · user accounts · Discord VoIP = OAuth + companion
client · SRD integration = backend live proxy + cache over REST · character generation =
guided wizard + quick-build, house-rule d20 scores assigned by the player, starting
levels 1–3, client-draft + pure server compile · **auth (implemented):** login by
username **or** email, age ≥ 13 at signup, strict password policy (≥ 8 chars with upper,
lower, digit, special) with a **confirm-password field** (R16), separate required **real
name** (R17), **PII encrypted at rest** with an `emailKey` blind index (R18), 24h JWT with
no refresh token, single-use 24h email-verification tokens with a 60s resend cooldown, roles
`USER`/`MODERATOR`/`ADMIN` with a **bootstrap admin seeded on every profile when
`tabletopserv.admin.password` is set** (R20) · **frontend (implemented):** Tailwind CSS v4,
react-router, JWT in
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
**Monster generation (implemented, §9b):** the deterministic "chassis" CR→statblock
`MonsterMathEngine` is implemented server-side in `tabletopservice` (official DMG CR curve
0–30 incl. fractions, role shifts + Auto keyword resolution per the §9b table, size ladder,
templated traits/actions with **deterministic templated flavor** — the LLM flavor/abilities
piece is **deferred**), persisted `Monster` entity owned by the creating GM, exposed via
`POST /api/monsters/generate` (returns 201) and `GET /api/monsters/mine` (not a direct Cros.land
integration — it has no public API) · **egress gateway (decided, §16):** a dedicated Express
service (`tabletopgateway/`, Node 24, native `fetch`-based forwarder) sits **behind** Spring as the
only path out to upstreams (SRD, monster-gen LLM, future integrations); curated route table,
deny-by-default, server-held keys, SSRF guard, timeouts, correlation IDs; the backend's own
`SrdClient`/`GatewayClient` calls it · **3D rendering (decided, §17):** React Three Fiber +
drei as an optional viewport sharing the existing 2D `BattleMapDto` state (same
server-authoritative tokens/movement), glTF models for avatars/monsters, WebGL2 now with the
WebGPU renderer as the future path.
**Backend structure (decided, §18):** `tabletopserv` becomes a **multi-module Maven reactor** —
`tabletopapi` holds the inbound REST contract (controller interfaces + DTOs + shared error
types, package `com.gamer.fowever.tabletopapi`) and `tabletopservice` holds the implementations,
domain, repositories, security, config, STOMP glue and the runnable app (package
`com.gamer.fowever.tabletopservice`); the jacoco ≥ 90% line gate applies to `tabletopservice`
only. · **egress gateway (landed, §16):** `tabletopgateway/` (Express 5, Node 24, ESM,
Vitest+supertest) is now the only outbound path; SRD forwards to `https://www.dnd5eapi.co/api/2014`
through an allowlisted route table with `X-Gateway-Token`, TTL cache and `502 {status,message}`
on upstream failure; the Spring side routes `/api/srd/*` through it via `GatewayClient`/`SrdClient`
(config `tabletopserv.gateway.url`/`.token`, env `GATEWAY_URL`/`GATEWAY_TOKEN`).

- **Deployment target (decided):** **Railway** (§19) — one project: a managed **Postgres**
  database plus three services (**backend** Spring Boot JAR via `tabletopserv/Dockerfile`,
  **egress gateway** Node via Railpack, **web** static SPA via the nginx `tabletopweb/Dockerfile`
  with React-Router fallback). Services talk over **private networking**
  (`<service>.railway.internal`) so the gateway keeps **no public domain**; WebSockets/STOMP
  are supported. Secrets live in the dashboard per service (`.railway/railway.ts` marks them
  `preserve()` so `config apply` never clobbers them); deploys are **manual CLI**:
  `railway up` from each app directory. Done on the **Hobby** plan with Railway-provided
  `*.up.railway.app` domains. Railway's legacy `railway.toml` config-as-code is **deprecated
  (cutoff 2026-12-01)**, so we use **Infrastructure-as-Code** (`.railway/railway.ts` + a root
  `railway` devDependency).
- **Chargen rules (decided, 2014 PHB):** the PHB is the rules authority and the SRD is the
  data feed (§8/§9); standard array is the default score source with point-buy and
  4d6-drop-lowest selectable; the 6×d20 house rule becomes an optional score source; HP = max
  at level 1 + CON per level; proficiency bonus +2 for levels 1–3. The 2024 ruleset stays
  deferred alongside the SRD `/api/2024` upgrade path.
- **Chargen (implemented, §8):** server-side flow in `CharacterService` + `ChargenRules`:
  `POST /api/characters/compile` (idempotent validate + derive; returns 200 `CompileResult`
  with `valid`, `violations`, `sheet` even when illegal), `POST /api/characters/generate`
  (random quick-build), `POST /api/characters/roll-scores` (server-authoritative base-score
  roll; rejects standard array / point-buy), `GET|POST /api/users/me/characters` and
  `GET /api/users/me/characters/{id}` with per-user access; `CharacterDraftDto` /
  `CharacterSheetDto` (persisted snapshot) / `ScoreSource` live in `tabletopapi`.
  `CharacterJourneyIT` covers the full journey including standard array, point-buy, 4d6,
  illegal drafts and the generate happy path. Web UI in `feature/chargen`: `CharactersPage`
  (list + quick-build with preview/save/revise) and `CharacterWizardPage` (10-step guided
  flow with skill/spell pick caps mirroring the server) and `CharacterSheetPage`; routes at
  `/characters`, `/characters/new`, `/characters/:id`.
- **Starting gold & equipment (decided, §8):** starting equipment is **bought from the class's
  PHB starting wealth** and never granted free; the purse uses the **average of the PHB
  wealth-by-class pool** (deterministic, server-side) and the **server validates the budget on
  compile** — SRD `cost` is the price authority, the wizard's bundled price map is display-only.
  Gold travels as `startingGoldGp` / `spentGoldGp` **in the sheet snapshot only**, so no DDL
  changes touch the Railway Postgres (`ddl-auto=validate`). Expanded classes beyond the SRD
  core (Artificer, blood hunters, ...) need a **non-SRD data feed** (homebrew import vs licensed
  API) and are **deferred (R24)** pending that decision.

- Do we need friends list / permanent groups, or is invite-code enough for now?
- Exact D&D 5e sheet fields — confirm which sets matter for v1.
- ~~Dice rolls: server-authoritative only, or allow GM-private rolls with reveal?~~
  **Decided:** server-authoritative, with GM-private rolls delivering the full result only to
  the GM (hidden frame for the rest of the table).
- ~~Deployment target (containers? platform?)~~ **Decided:** Railway (§19); Flyway migrations
  begin in Stage 5 alongside the Railway Postgres wiring.
- Room persistence: sessions archived/joinable later, or ephemeral?
- SRD: keep the live proxy, or eventually mirror 5e-bits data into our own DB?
- SRD version pinning: stay on `2014` — when to consider the `2024` ruleset?
- SRD multilingual (`?lang=`): worth supporting beyond English?
- ~~Score sources: add standard array / point-buy / 4d6 alongside the house-rule d20?~~
  **Decided:** all four are implemented; the rolled sources are server-authoritative (§8).
- **Expanded classes (R24):** the SRD only ships the 12 core classes — where should an
  Artificer / homebrew class catalog come from (curated import into our DB, a third-party
  licensed API, or a GM-facing homebrew class editor)?
- House-rule d20: always on, or a configurable table/room option?
- Beyond level 3: leveling up existing characters (not just creating at 1–3)?
- Monster-generation **LLM provider**: which vendor/key to standardize on for the gateway (§9b)?
  The deterministic chassis math is **decided + implemented** — only the deferred
  flavor/abilities LLM remains open.
- 3D: which glTF asset source/style for avatars/monsters, and do we ship a bundled starter pack (§17)?

## 15. Tech Notes (existing repo context)

- Backend: Spring Boot 4.1.1, Java 21, **multi-module Maven reactor** rooted at
  `tabletopserv/` (`packaging pom` parent; `<modules>` = `tabletopapi`, `tabletopservice`,
  `tabletopfunctionaltest` commented out). REST APIs live as interfaces in
  `com.gamer.fowever.tabletopapi`; implementations + domain +
  runtime in `com.gamer.fowever.tabletopservice` (executable JAR, no servlet container,
  `spring-boot-starter-webmvc` / `webflux` present).
- Frontend: Vite 8 + React 19, plain JSX, oxlint, Vitest. Tailwind CSS v4
  (`@tailwindcss/vite`), react-router, Node 24 (`tabletopweb/.nvmrc`, `engines`, CI).
- Gateway: `tabletopgateway/`, Express 5 + native `fetch`-based forwarder, Node 24, ESM,
  plain JS, Vitest + supertest; the only outbound path (see §16).
- Backend CORS: `CorsConfigurationSource` bean wired into the Security filter chain for
  `/api/**`, origins from `tabletopserv.cors.allowed-origins`
  (env `CORS_ALLOWED_ORIGINS`, dev default `http://localhost:5173`, prod default empty).
- CI: `gateway.yml`, `node.js.yml`, `maven.yml`, `maven-publish.yml` (see `AGENTS.md`).
- Dice/rolls (incl. the d20 score rolls) use a cryptographically secure RNG
  (server-side `java.security.SecureRandom`).
- **Dependencies now present (service module):** JPA (+ `-test`), JDBC, security
  (+ `security-test`), validation, mail, webflux/webmvc (+ test starters), websocket,
  cache + caffeine, H2 + PostgreSQL (runtime), jjwt 0.12.6 (JWT signing), and jacoco with a
  ≥ 90% line coverage gate on the `test` phase (service module only).
- **Auth stack in place:** stateless JWT filter chain, BCrypt, roles
  `USER`/`MODERATOR`/`ADMIN` on `User`, email verification via `EmailVerificationToken`
  (`ConsoleEmailSender` in dev, SMTP in prod), and a bootstrap admin seeded on **all**
  profiles when `tabletopserv.admin.password` is non-blank (credentials from
  `tabletopserv.admin.*` props, overridable via env). **PII at rest (R18):** `email`,
  `displayName`, `realName` and `dateOfBirth` are encrypted with AES-256-GCM
  (`PiiCrypto` + JPA `AttributeConverter`s, key from `tabletopserv.pii.secret` / `PII_SECRET`,
  sha-256 derived 256-bit key, fail-fast when blank in prod); `email` is **no longer unique**
  — a deterministic `email_key` blind index (HMAC-SHA256) column carries uniqueness and lookup,
  and login/register/resend/userDetails all resolve via username **or** `emailKey`. Dev/test
  run on a fixed fallback key so JPA slices round-trip; prod must set `PII_SECRET`. Explicit
  JSON `401`/`403` responses; business errors handled by `GlobalExceptionHandler`.
- **Sessions stack in place:** STOMP over `/ws` (`spring-boot-starter-websocket`), client
  inbound channel on `SyncTaskExecutor` + `StompAuthChannelInterceptor` (throws
  `MessageDeliveryException`), `TokenHandshakeHandler` for the `?token=` handshake param,
  `SessionEventsController` (`@SubscribeMapping` snapshot replay) + the `tabletopapi`
  interfaces implemented by `*ControllerImpl` in `tabletopservice`;
  `SessionService`/`SessionPresenceService` persist `Session`/`Participant`/`SessionEvent`
  and broadcast on `/topic/sessions/{id}`.
- **Outbound stack (landed):** `GatewayClient` (WebClient → `tabletopserv.gateway.url`,
  `X-Gateway-Token` + correlation id) and `SrdClient`, backing `SrdControllerImpl`
  (`GET /api/srd/{collection}[/{index}]`, curated query passthrough).
- **Deployment surface (added, §19):** the backend prod profile binds `server.port=${PORT:8080}`
  (`application-prod.properties`), wires a Postgres datasource from
  `PGHOST`/`PGPORT`/`PGDATABASE`/`PGUSER`/`PGPASSWORD`, `ddl-auto: validate`, and exposes
  Spring Actuator's `/actuator/health` (`permitAll` in the security chain) for health checks.
  The gateway binds `PORT` → `GATEWAY_PORT` → 3001 and binds `0.0.0.0` when a container `PORT`
  is present (`resolveListenConfig`). The web app ships as nginx serving the Vite `dist/` with
  a React-Router SPA fallback (`tabletopweb/nginx.conf.template`) and a `VITE_API_URL` build
  arg. `tabletopserv/Dockerfile` (Maven build → temurin JRE), `tabletopweb/Dockerfile`, and
  `.railway/railway.ts` (+ root `railway` devDependency) define the Railway project.
- Still to build (Stage 2-3 runtime): the custom `Character` model + generation (§8) and the
  optional 3D viewport (§17) — homebrew monster generation (§9b) landed in
  `feature/monster-generation`.

## 16. Egress API Gateway (Express)

**Why:** every external/upstream call — the SRD proxy (§9), monster-generation LLM (§9b), and
future integrations (Discord OAuth, image/3D asset services) — leaves the backend. Instead of
letting the Spring backend open connections to arbitrary hosts, all outbound traffic flows
through a dedicated gateway that enforces allowlisting, secrets, timeouts, and caching in
**one** place.

**Decision (landed):** an **egress proxy service** sits **behind** the Spring backend
(`tabletopgateway/`, Express 5 + Node 24 + a native `fetch`-based forwarder), on the *outbound* path:

```
Frontend (React SPA)
      │  REST /api/**  +  STOMP /ws?token=…    (unchanged)
      ▼
Spring Boot backend (authoritative: auth, sessions, SRD allowlists, character/monster logic)
      │  server-to-server, server-held keys (X-Gateway-Token)
      ▼
Express egress gateway ──►  internet upstreams
   • dnd5eapi.co SRD          (route `srd` → https://www.dnd5eapi.co/api/2014)
   • LLM provider (future)    (route `llm-monsters`, disabled until keys exist)
   • Discord OAuth (future) / asset services (future)
```

- **Not client-facing.** Only the Spring backend calls it (loopback bind in dev, or **private
  networking with no public domain** on Railway — §19), so only server-held credentials/keys
  are present there. Ingress stays as today: SPA → Spring
  directly (no Vite `/api` proxy, cross-origin via `VITE_API_URL`).
- **Route table (deny-by-default):** `src/routes.js` maps a named route (`srd`, future
  `llm-monsters`, …) to a fixed target base URL and an **allowlisted set of path prefixes**
  and **query params**. Any path/query/route outside the table returns `403`.
  - SRD allowlist mirrors the curated collections in §9: `races`, `classes`, `subclasses`,
    `subraces`, `ability-scores`, `skills`, `proficiencies`, `equipment`,
    `equipment-categories`, `spells`, `features`, `traits`, `feats`, `conditions`,
    `languages`, `monsters` — each `[/{index}]`; allowed query params `level`, `school`,
    `name`, `index`.
- **Auth:** Spring sends `X-Gateway-Token` (env `GATEWAY_TOKEN`, server-held) on every call;
  the gateway rejects requests without it (`401`) so the egress is not an open proxy.
- **Caching (single owner):** the gateway TTL-caches SRD **GET** responses (long TTL on list
  endpoints — races, classes, ability scores, skills, spells, equipment) keyed on
  path + query; Spring keeps its Caffeine for other uses. Provides cached-copy fallback if
  the upstream blips (per §9 fallback).
- **Reliability:** per-route **timeout** + **response-size cap**, one retry for idempotent
  GETs on `5xx`/network errors, `X-Correlation-Id` pass-through + structured request/response
  logs so a chain (client → Spring → gateway → upstream) is trailable.
- **SSRF defense-in-depth:** targets are fixed in the route table (no dynamic hostname from
  requests); the gateway additionally resolves and rejects private/link-local/metadata
  address ranges.
- **Error shape:** upstream unreachable → `502 {"status":502,"message":"upstream unavailable: <route>"}`;
  gateway errors keep the same `{status,message}` contract Spring's `GlobalExceptionHandler` emits.
- **Health:** `GET /health` returns `200 {status:"ok"}` for dev/CI checks.
- **Spring side:** `GatewayClient` (WebClient; base `tabletopserv.gateway.url`, default
  `http://localhost:3001`; token `tabletopserv.gateway.token`, env `GATEWAY_TOKEN`, sent as
  `X-Gateway-Token`; + `X-Correlation-Id`) is the only outbound WebClient. `SrdClient` builds
  curated `/api/srd/…` calls on it and maps failures to the usual `{status,message}` shapes.
  No other raw WebClient goes to the internet (AGENTS.md rule).
- **Package:** Express 5 + a native `fetch`-based forwarder (no `http-proxy-middleware`) —
  full response buffering is needed for the TTL-cached stale fallback, the response-size cap
  and the single retry. Tests: Vitest + supertest. SRD route mounted at **`/api/srd`**.

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

## 18. Testing Strategy (unit → slice → functional)

**Why:** unit tests (≥ 90% jacoco line gate on `tabletopservice`) already cover the domain; a
separate module gives us a home for **functional/E2E** suites that exercise the running app
the way a browser/API client would, without polluting the service module's coverage gate.

| Layer | Where | Tools | Scope |
|---|---|---|---|
| Unit | `tabletopservice` | JUnit 5 + Mockito/MockMvc (existing) | domain logic, services, controllers; ≥ 90% line gate |
| Slice/integration | `tabletopservice` | `@SpringBootTest` + `@AutoConfigureMockMvc`, slice tests | app wiring, security filter chain, STOMP sessions, outbound gateway mocks |
| Functional/E2E | `tabletopfunctionaltest` (live) | JUnit 5 + Testcontainers (Postgres) + JAR-subprocess harness vs the executable JAR; recorded-fixture gateway stub (Java `HttpServer`) | end-to-end API/WebSocket journeys against a real runtime + real DB — auth → session/STOMP → battle map → dice → monster → gateway-wrapped SRD |

- **How it runs:** `maven-failsafe-plugin` binds `*IT` classes to the `integration-test`/`verify`
  phases, so **`./mvnw test` stays unit-only** and **`./mvnw verify`** builds the packaged
  `tabletopservice` JAR, boots it as a **subprocess** (`java -jar`) against a
  **Testcontainers Postgres** (dev profile + Postgres env overrides; zero app-code changes),
  and runs the journeys over the real HTTP/STOMP interfaces. Email-verification tokens are
  parsed from the child JVM's dev-console emails. CI runs `./mvnw -B verify` on every push/PR
  to `develop`.
- **Upstream determinism:** SRD flows route through a **recorded-fixture gateway stub** built
  from `com.sun.net.httpserver` (`src/test/resources/fixtures/srd/*.json`) that mimics the real
  gateway's surface (`X-Gateway-Token` check on `/api/srd/*`) — no internet access, fully
  deterministic. Phase 2 will move this stub to the real gateway app and Phase 3 adds
  Playwright web E2E.
- **Docker-less dev escape hatch:** if Docker is unavailable (constrained environments), pass
  `-Dtabletopserv.functional.db-url=jdbc:h2:mem:...` to point the harness at any JDBC URL;
  CI always exercises the Testcontainers Postgres path.
- **Module rules:** `tabletopfunctionaltest` has **no jacoco gate**; it depends on the
  packaged `tabletopservice` and runs against a started server, so it never feeds the covered
  code's gate.
- **Coverage philosophy:** unit + slice coverage stays in `tabletopservice`; functional tests
  are **journey coverage**, not branch coverage.
- **VSC (versioned) contract:** the SRD/gateway DTO shape is pinned by the API module and
  verified by both the service tests (against a mocked gateway) and functional tests (against
  the recorded fixture).
- **CI/CD decision (Jenkins — advisory only):** no Jenkins pipeline is adopted. GitHub Actions
  covers build/test/E2E on every PR and the Railway CLI covers deploys (`railway up`); Jenkins
  would only be reintroduced for org policy, a broader OS/JDK matrix, on-prem infrastructure,
  or scheduled smoke runs.

## 19. Deployment (Railway)

**Decision (Draft v0.12 / updated v0.14):** host the MVP on **Railway** — one project with a managed
**Postgres** database and three deployable services. Config is **Infrastructure-as-Code**
(`.railway/railway.ts`, evaluated/ applied by the Railway CLI) because Railway's legacy
`railway.toml` config-as-code is deprecated (hard cutoff **2026-12-01**). Each service now has
a **`source: github(..., { rootDirectory })`** so pushes to `master` trigger an automatic
build in the correct app directory; manual `railway up` remains available as an escape hatch.

```
                    ┌──────────────────────────────────────────────┐
                    │  Railway project "fuzzy-happiness" (Hobby)    │
                    │                                              │
                    │  web (public gamenight.bond / *.up.railway.app)  nginx + dist │
                    │        │ REST /api/** + STOMP /ws?token=      │
                    │        ▼                                     │
                    │  backend  ──►  gateway          <service>.railway.internal
                    │  (Spring   │   (Node, egress-  │   private network, no public
                    │   JAR)     │    only)          │   domain for gateway
                    │        │   │        │          │
                    │        ▼   ▼        ▼           ▼
                    │  Postgres (managed, private DB connection)
                    └──────────────────────────────────────────────┘
```

### Services

| Service | App dir | Build | Runtime | Health check |
|---|---|---|---|---|
| `backend` | `tabletopserv/` | `Dockerfile` (Maven → temurin JRE) | `java -jar app.jar`, `SPRING_PROFILES_ACTIVE=prod` | `/actuator/health` |
| `gateway` | `tabletopgateway/` | Railpack (Node 24) | `node src/index.js` on `$PORT` | `/health` |
| `web` | `tabletopweb/` | `Dockerfile` (node build → nginx) | nginx SPA fallback on `$PORT` | `/` |
| `postgres` | managed | Railway Postgres | private network | — |

### Networking

- Services in the same project reach each other over the **private network** at
  `<service>.railway.internal`; traffic never egresses. The backend's `GATEWAY_URL` points at
  `http://gateway.railway.internal` (`${{gateway.RAILWAY_PRIVATE_DOMAIN}}`).
- Only **web** and **backend** get public domains. Since 2026-09 the **web** service also
  serves the **custom domain `gamenight.bond`** (registered **through Railway**, whose
  nameservers auto-Manage DNS for domains purchased in-product: HTTPS certs auto-issue, no
  external CNAME/TXT to hand-cut) — apex + `www`, both verified and live with valid TLS
  (Hobby allows 2 custom domains per service, so both fit). The **backend** keeps its
  Railway-provided `*.up.railway.app` domain. The **gateway gets none** — it stays
  egress-only by construction.
- `CORS_ALLOWED_ORIGINS` on the backend now lists `https://gamenight.bond`,
  `https://www.gamenight.bond`, and the web service's `*.up.railway.app` URL.
- WebSockets/STOMP work on Railway as plain HTTP upgrades; no proxy config needed.

### Ports & binds

- Backend: `server.port = ${PORT:8080}` (`application-prod.properties`) — Railway injects
  `PORT`.
- Gateway: `resolveListenConfig()` binds `PORT` → `GATEWAY_PORT` → 3001 and picks `0.0.0.0`
  when a container `PORT` is present (loopback stays the local-dev default).
- Web: official nginx entrypoint renders `/etc/nginx/templates/default.conf.template` via
  `envsubst`, binding `listen ${PORT}` (default 8080) with `try_files` SPA fallback so
  react-router routes deep-link.

### Variables (per service, dashboard)

Backend needs (prod profile refuses to boot clean without them): `SPRING_PROFILES_ACTIVE=prod`,
`JWT_SECRET`, `PII_SECRET`, `ADMIN_USERNAME`/`ADMIN_PASSWORD`, `SMTP_HOST`/`SMTP_PORT`/`SMTP_USER`/`SMTP_PASSWORD`,
`CORS_ALLOWED_ORIGINS` (the web's public URLs: `https://gamenight.bond`,
`https://www.gamenight.bond`, plus the `*.up.railway.app` URL), `FRONTEND_URL=https://gamenight.bond`,
`GATEWAY_URL`, `GATEWAY_TOKEN`, and the
datasource `PGHOST`/`PGPORT`/`PGDATABASE`/`PGUSER`/`PGPASSWORD` (referenced from the Postgres
service). The gateway needs the **same** `GATEWAY_TOKEN`. The web service needs `VITE_API_URL`
(backend public URL) set **before** its build, then a redeploy. `.railway/railway.ts` marks the
secrets `preserve()` so a later `railway config apply` never overwrites dashboard values.

### Lifecycle

```
railway login
railway link                 # link repo ↔ project
railway config plan          # preview IaC diff (safe)
railway config apply         # create Postgres + 3 services (needs user go-ahead)
# per app: cd <dir> && railway up      # manual escape hatch
# auto-deploy: pushes to master trigger a build per service rootDirectory
```

Set the dashboard secrets above, give the backend a public domain (or set `CORS_ALLOWED_ORIGINS`
to the generated URL — both are already in place with `gamenight.bond` live), then deploy
**gateway → backend → web** (the bearer token must match and
`GATEWAY_URL` must resolve first). Rollback = one-click previous deploy; PR preview environments
are available if later wanted.

### Database & migrations

`ddl-auto: validate` in prod; **Flyway** migrations arrive in Stage 5 (first tracked schema
change set). Managed Postgres has automated backups; connect over the private network
(`DATABASE_URL` or the `PG*` variables) to avoid egress charges.

### Cost posture

**Hobby** plan ($5/mo, includes $5 of usage; up to 2 custom domains, 6 replicas, 5 GB volumes).
Keep internal traffic private and the gateway port-less to stay inside the included usage;
upgrade to **Pro** only if real traffic demands it.