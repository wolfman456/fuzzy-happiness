# Session

Pick up where we left off: opencode session resumes via `/sessions`; if starting fresh, read this file plus `draft-design.md` (§8 Character Generation, §9 SRD Integration, §13 Roadmap are the strategic sections) and `AGENTS.md`.

## Where we are

- Current branch: **`feature/backend-modules-gateway`** (created off `develop` for the backend restructure + gateway + SRD rewire).
- **Draft v0.10** (backend restructure + gateway + SRD rewire) landed on this branch. The prior branch (`feature/gateway-monster-3d`) delivered design flush v0.9 in `draft-design.md`.
- **Backend structure:** `tabletopserv/` is now a **multi-module Maven reactor**:
  - `tabletopapi` — inbound REST contract only (interfaces + DTOs, `com.gamer.fowever.tabletopapi`, no business logic).
  - `tabletopservice` — `*ControllerImpl implements *Api`, services, domain, repos, security, config, STOMP glue, runnable JAR (`com.gamer.fowever.tabletopservice`). jacoco ≥ 90% line gate **here only**.
  - `tabletopfunctionaltest` — blank / commented out of parent `<modules>`; future functional/E2E suites (§18).
- **Egress gateway:** `tabletopgateway/` (Express 5, Node 24, ESM, plain JS, Vitest+supertest). The only outbound path. SRD route forwards curated collections + query params to `https://www.dnd5eapi.co/api/2014` through an allowlisted route table; `X-Gateway-Token`, SSRF guard, TTL cache, `502 {status,message}` on upstream failure. Dev port `GATEWAY_PORT` (default 3001).
- **SRD rewire:** `GatewayClient` (WebClient → `tabletopserv.gateway.url`, default `http://localhost:3001`; `X-Gateway-Token` from `tabletopserv.gateway.token`/`GATEWAY_TOKEN`) + `SrdClient`; `SrdControllerImpl` backs `SrdApi`: `GET /api/srd/{collection}[/{index}]` with curated query passthrough (level/school/name/index).
- Do not merge PRs yourself — maintainer reviews and merges (per AGENTS.md).

## Decisions locked in

- D&D 5e-first MVP; generic `Character` core so future games plug in.
- Discord = voice companion (OAuth + deep link to installed client), not built-in VoIP.
- Rules data: open 5e-bits SRD API (`dnd5eapi.co`) consumed via **egress gateway** (§16); Spring backend never opens raw outbound connections to the internet — only `GatewayClient`/`SrdClient` → gateway → upstream. `X-Gateway-Token` required; deny-by-default route table; SSRF guard; SRD TTL cache (gateway is the single cache owner for SRD lists). `502 {status,message}` on upstream failure.
- Character generation: guided wizard + random quick-build. Ability scores = house-rule 6xd20 rolled server-side, returned unassigned; player assigns. Starting level configurable 1-3. Client-draft + pure server compile (validate vs SRD allow-lists, derive modifiers/HP/AC/saves/spell slots), finalize persists `Dnd5eCharacter`.
- DB: H2 dev / PostgreSQL prod via Spring profiles. Boot 4 (no `spring-boot-starter-web`), JAR packaging.
- Auth (implemented): login by username **or** email, age ≥ 13, strict password policy, 24h JWT (no refresh), single-use 24h email-verification tokens with 60s resend cooldown, roles `USER`/`MODERATOR`/`ADMIN`.
- Frontend (implemented): Tailwind CSS v4 via `@tailwindcss/vite`, react-router, JWT in `localStorage` restored via `GET /api/users/me`, Node 24 pinned, backend CORS via `tabletopserv.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`); no Vite `/api` proxy — SPA calls backend cross-origin with `VITE_API_URL`.
- Backend structure (implemented, §18): `tabletopserv/` multi-module Maven reactor — `tabletopapi` (interfaces + DTOs, package `com.gamer.fowever.tabletopapi`) + `tabletopservice` (impls + domain + runtime, `com.gamer.fowever.tabletopservice`); `tabletopfunctionaltest` commented out until first E2E suite; jacoco ≥ 90% on service only.
- Sessions & live chat (implemented): STOMP over `/ws` with JWT as a `?token=` handshake param; `/ws/**` is `permitAll()` — the token IS the auth. `StompAuthChannelInterceptor` (client inbound channel on `SyncTaskExecutor`) denies unauthenticated/non-member/unknown subscriptions by throwing `MessageDeliveryException`, so clients get real STOMP ERROR frames; membership re-validated on every SUBSCRIBE and SEND. Snapshot replay: `@SubscribeMapping` on `/app/sessions/{id}` returns `SessionSummary` (roster + `recentEvents`); live events (CHAT, PRESENCE; future DICE/TABLE) broadcast on `/topic/sessions/{id}` and are persisted as `SessionEvent`. Invite join assigns `PLAYER`; creator = `GM`; status `OPEN` (with auto-`ACTIVE` on first join).

## Next steps

- Character generation shipped (§8): draft→compile→finalize backend + frontend wizard/quick-build/sheet (R13 in `feature/chargen`, awaiting merge + deploy). Railway auto-deploy fix (R14) ships in the same PR: add `source: github(...)` + `rootDirectory` per service in `.railway/railway.ts` so deploys build the right app dir.
- Then table state + dice (§3/§6): DICE events on the existing event stream, server-authoritative rolls.
- Homebrew monster generation (§9b) via the gateway `llm-monsters` route (Stage 3b).
- Optional 3D battle-map viewport (§17) via React Three Fiber + drei.
- Functional test module (§18) scaffolded when the first E2E suite is needed.
- Deferrals recorded in `draft-design.md` §14 (standard array/point-buy/4d6, house-rule d20 config, leveling >3, SRD mirror/multilingual/2024, JWT refresh).

## Gotchas

- **gh CLI**: installed/authenticated at `~/.local/bin/gh` (v2.98.0, user `wolfman456`, ADMIN) — use `export PATH="$HOME/.local/bin:$PATH"`; PRs still reviewed/merged by maintainer.
- **Node 24**: every frontend/gateway bash command needs `source ~/.nvm/nvm.sh && nvm use 24` (fresh sessions default to Node 20). No `rg` — use `grep`.
- Editor freezes in non-interactive shells: set `GIT_EDITOR=true` for rebase/commit steps.
- Backend = Spring Boot 4.1.1, Java 21, **multi-module Maven reactor**; frontend = Vite 8 + React 19 (plain JSX), oxlint + Vitest; gateway = Express 5, Node 24, ESM, plain JS, Vitest+supertest. See `AGENTS.md`.
- Boot 4 auto-starts Docker Compose if `compose.yaml` exists — `spring.docker.compose.enabled=false` already set; don't remove it.
- Verification links appear in the backend console log in dev (ConsoleEmailSender).
- STOMP ERROR frames only reach clients if the client inbound channel runs inline (`SyncTaskExecutor`) AND the interceptor throws a `MessagingException` (`MessageDeliveryException`) — Spring's Java STOMP client delivers them to `StompSessionHandler.handleFrame` (native header `message`), not `handleException`.
- `tabletopapi` has **no** jacoco gate (interfaces/records would fail a strict gate) — gateway is on `tabletopservice` only.
- The gateway `package.json` is ESM (`"type": "module"`) — plain JS, no TypeScript. Tests: Vitest + supertest. `.nvmrc` pinned to Node 24.
