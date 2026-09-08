# fuzzy-happiness

A web platform for playing tabletop games with friends remotely — without having to
re-buy materials you already own. Multiple people join a shared session and interact
in real time, voice is provided through Discord, and character sheets are modelled per
game on top of a shared, portable base. Development targets **Dungeons & Dragons** first;
the core (users, sessions, characters, game registry) stays generic so other systems can
be added later.

## Why

This project came up as my son has friends moving away and they want to continue to play
DnD together. But not have to re-buy materials they already own from Roll20. So I told him
I would give it a try and see what I can do. I would call it an interesting side project
if nothing else.

## Core ideas

- **Real-time sessions** — multiple people in one room, sharing chat, presence, dice and
  table state over a WebSocket.
- **Games as plug-ins** — an abstract `Character` base holds what translates across games;
  each game (starting with D&D 5e) contributes its own concrete sheet.
- **Rules data from the 5e SRD API** — the D&D plug-in is backed by the open, no-auth
  [5e-bits SRD API](https://5e-bits.github.io/docs/introduction) (dnd5eapi.co): races,
  classes, spells, equipment and more are fetched through the **egress gateway** (single
  outbound path) and cached, so no rules data is hard-coded into the app.
- **Discord for voice** — connect your Discord account and jump into a voice channel;
  the web app runs beside it as the shared game table.
- **Database** — H2 while developing; PostgreSQL once deployed to production.

## Tech stack

- Frontend: `tabletopweb/` — React 19, Vite 8, plain JSX (oxlint, Vitest), Tailwind CSS v4, react-router, Node 24
- Backend: `tabletopserv/` — Spring Boot 4.1.1, Java 21, **multi-module Maven reactor**
  - `tabletopapi` — inbound REST contract only (interfaces + DTOs, `com.gamer.fowever.tabletopapi`)
  - `tabletopservice` — implementations, domain, repos, security, STOMP glue, runnable JAR (`com.gamer.fowever.tabletopservice`)
  - `tabletopfunctionaltest` — blank / commented out; future functional/E2E suites (§18)
- Gateway: `tabletopgateway/` — Express 5, Node 24, ESM; egress-only proxy (SRD + future LLM, deny-by-default, `X-Gateway-Token`)
- Auth: Spring Security — 24h JWT bearer (jjwt), bcrypt, `USER`/`MODERATOR`/`ADMIN` roles, email verification; CORS for the Vite dev origin
- Rules data: D&D 5e SRD API (5e-bits/dnd5eapi.co) → **gateway** (`/api/srd/*`) → Spring; cached at the gateway (long TTL on lists)
- Persistence: JPA (H2 dev / PostgreSQL prod via Spring profiles)

See `AGENTS.md` for repo layout, commands, and conventions.

## Status

Iterative build; design draft in [`draft-design.md`](draft-design.md) (Draft v0.10).

Delivered:

- Core domain model + JPA persistence (users, games, characters, sessions) — `feature/java-start` (PR #9).
- Accounts & auth — `feature/spring-security`: register / login (username or email), 24h JWT,
  roles, bcrypt, email verification with resend; bootstrap admin in dev. 61 backend tests,
  jacoco line-coverage gate ≥ 90%.
- Frontend auth slice + app shell — `feature/frontend-auth`: register / login / email-verify
  pages, protected dashboard with stub session & character cards, auth state in
  `src/auth/` (localStorage + context), Tailwind UI. Backend CORS added for the dev origin.
  Node 24 enforced across the frontend (`.nvmrc`, `engines`, CI). 32 frontend tests (Vitest).
- Sessions & live chat — `feature/sessions`: **Stage 1 complete**. Backend: session REST
  (`POST /api/sessions`, `POST /api/sessions/join`, `POST /api/sessions/{id}/leave`).
  `GET /api/sessions/{id}` snapshot, `GET /api/games`, and STOMP over `/ws?token=<jwt>` with
  membership-private topics (`/topic/sessions/{id}`) and `@SubscribeMapping` snapshot replay
  (`/app/sessions/{id}`). `StompAuthChannelInterceptor` returns STOMP ERROR frames to
  unauthenticated/non-member clients. 101 backend tests, jacoco gate met. Frontend: lobby
  (`/sessions`) with create/join-by-invite-code and a live session screen (`/sessions/:id`)
  with roster, invite code and chat/presence over `@stomp/stompjs` (`src/lib/stomp.js`),
  Sessions nav enabled. 57 frontend tests (Vitest), oxlint + build clean.
- Game table slice 1 — battle map — `feature/battle-map`: **Stage 3 track 1**. Backend: grid
  map per session (`GET|POST /api/sessions/{id}/map`), tokens
  (`POST|PATCH|DELETE /api/sessions/{id}/map/tokens[/{id}]`), movement
  (`POST …/map/tokens/{id}/move`) enforced server-side against a per-turn budget
  (`floor(speed/10ft)` squares, Chebyshev diagonal cost), turn control
  (`POST …/map/turn` — `START|END|NEW_ROUND`), participant auto-tokens, and a new `TABLE`
  session-event type broadcasting the full map state on `/topic/sessions/{id}`. 131 backend
  tests, jacoco gate met. Frontend: `src/lib/mapGeometry.js` (race→speed table, reachability),
  `src/lib/battleMap.js`, and `BattleMapPanel` on the session screen — render grid + tokens,
  select-to-move with reachable-square overlay, GM add/edit/remove tokens + turn bar, TABLE
  events kept out of the chat feed and applied to the live map. 88 frontend tests (Vitest),
  oxlint + build clean.
- Game table slice 2 — dice & initiative — `feature/dice-initiative`: **Stage 3 track 2**.
  Backend: `POST /api/sessions/{id}/roll` with a validated dice grammar (`(\d+)?d(\d{1,3})
  ([+-]\d{1,3})?`, ≤ 20 dice / 999 sides / mod ±100) — public rolls persist a `DICE`
  `SessionEvent` broadcasting the full result; GM-private rolls broadcast a hidden frame on
  the topic and deliver the full frame only to the GM's `/user/queue/dice`. Initiative per
  map: `POST …/map/initiative` (replace order from labels/tokens, blank score auto-rolls d20),
  `POST …/map/initiative/{entryId}/reroll`, `POST …/map/initiative/next` (advances the pointer
  and activates the entry's token), `DELETE …/map/initiative/{entryId}`; `BattleMapDto` now
  carries `initiativeIndex` + ordered `initiative`. 164 backend tests, jacoco gate met.
  Frontend: `DiceTray` + `DICE` feed rows (hidden rolls shown fully only to the GM, merged by
  `rollId`), `InitiativeRail` (GM add/reroll/remove/advance, current-turn highlight on the map
  DTO), `/user/queue/dice` subscription in `src/lib/stomp.js`. 115 frontend tests (Vitest),
  oxlint + build clean.
- Design flush (no code) — `feature/gateway-monster-3d`: Draft v0.9 documents three researched
  areas in `draft-design.md`: homebrew **monster generation** (§9b — replicate the Cros.land
  CR-driven "chassis" math engine + our own LLM; the original has no public API), a dedicated
  **Express egress gateway** (§16 — `tabletopgateway/`, all outbound SRD/LLM calls route
  through it), and an optional **3D battle-map viewport** via React Three Fiber + drei (§17).
- **Backend restructure + gateway + SRD rewire** — `feature/backend-modules-gateway`
  (Draft v0.10, §18): `tabletopserv/` is now a **multi-module Maven reactor** — `tabletopapi`
  (interfaces + DTOs) on top of `tabletopservice` (implementations + runnable; jacoco ≥ 90%
  on service only). The **Express egress gateway** (`tabletopgateway/`) is the only outbound
  path (SSRF guard, `X-Gateway-Token`, SRD TTL cache, `502 {status,message}` on upstream
  failure). SRD data now flows `Spring → gateway → dnd5eapi.co`: `GET /api/srd/{collection}[/{index}]`
  with curated query passthrough.

Next: character generation backed by the SRD (Stage 2), homebrew monster generation (§9b),
and the optional 3D viewport (§17) plus the rest of the game table (multi-map, fog of war,
turn timers, conditions).
