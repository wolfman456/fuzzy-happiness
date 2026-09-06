# Session

Pick up where we left off: opencode session resumes via `/sessions`; if starting fresh, read this file plus `draft-design.md` (§8 Character Generation, §9 SRD Integration, §13 Roadmap are the strategic sections) and `AGENTS.md`.

## Where we are

- Current branch: **`feature/sessions`** (self-contained live-session slice; rebased on `develop`).
- Stage 1 "Sessions & chat" is **complete** (Draft v0.6): accounts/auth (PR #10/#11 merged) **plus** the lobby + live session slice now in `feature/sessions`.
- Backend: sessions REST (`/api/sessions`, `/api/sessions/{id}/leave`, `/api/sessions/join`, `/api/games`) + STOMP live chat/presence over `/ws?token=<jwt>` with membership-enforced topics and `@SubscribeMapping` snapshot replay. **101 tests pass**, jacoco ≥ 90% gate met.
- Frontend: `src/pages/LobbyPage.jsx` (create/join by invite code) + `src/pages/SessionPage.jsx` (roster, invite code, live chat via `@stomp/stompjs` in `src/lib/stomp.js`), routed at `/sessions` and `/sessions/:id`, Sessions nav enabled in `ShellLayout`. **57 Vitest tests pass**, oxlint + build clean.
- Do not merge PRs yourself — maintainer reviews and merges (per AGENTS.md).

## Decisions locked in

- D&D 5e-first MVP; generic `Character` core so future games plug in.
- Discord = voice companion (OAuth + deep link to installed client), not built-in VoIP.
- Rules data: open 5e-bits SRD API (`dnd5eapi.co`) consumed via backend proxy (WebClient) + Caffeine cache; no raw URL forwarding; cached fallback on outage.
- Character generation: guided wizard + random quick-build. Ability scores = house-rule 6xd20 rolled server-side, returned unassigned; player assigns. Starting level configurable 1-3. Client-draft + pure server compile (validate vs SRD allow-lists, derive modifiers/HP/AC/saves/spell slots), finalize persists `Dnd5eCharacter`.
- DB: H2 dev / PostgreSQL prod via Spring profiles. Boot 4 (no `spring-boot-starter-web`), JAR packaging.
- Auth (implemented): login by username **or** email, age ≥ 13, strict password policy, 24h JWT (no refresh), single-use 24h email-verification tokens with 60s resend cooldown, roles `USER`/`MODERATOR`/`ADMIN`.
- Frontend (implemented): Tailwind CSS v4 via `@tailwindcss/vite`, react-router, JWT in `localStorage` restored via `GET /api/users/me`, Node 24 pinned, backend CORS via `tabletopserv.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`); no Vite `/api` proxy — SPA calls backend cross-origin with `VITE_API_URL`.
- Sessions & live chat (implemented): STOMP over `/ws` with JWT as a `?token=` handshake param; `/ws/**` is `permitAll()` — the token IS the auth. `StompAuthChannelInterceptor` (client inbound channel on `SyncTaskExecutor`) denies unauthenticated/non-member/unknown subscriptions by throwing `MessageDeliveryException`, so clients get real STOMP ERROR frames; membership re-validated on every SUBSCRIBE and SEND. Snapshot replay: `@SubscribeMapping` on `/app/sessions/{id}` returns `SessionSummary` (roster + `recentEvents`); live events (CHAT, PRESENCE; future DICE/TABLE) broadcast on `/topic/sessions/{id}` and are persisted as `SessionEvent`. Invite join assigns `PLAYER`; creator = `GM`; status `OPEN` (with auto-`ACTIVE` on first join).

## Next steps

- Character generation (§8) + SRD proxy (§9): `Character` model, compilation, wizard/quick-build — next slice.
- Then table state + dice (§3/§6): DICE events on the existing event stream, server-authoritative rolls.
- Deferrals recorded in `draft-design.md` §14 (standard array/point-buy/4d6, house-rule d20 config, leveling >3, SRD mirror/multilingual/2024, JWT refresh).

## Gotchas

- **gh CLI**: installed/authenticated at `~/.local/bin/gh` (v2.98.0, user `wolfman456`, ADMIN) — use `export PATH="$HOME/.local/bin:$PATH"`; PRs still reviewed/merged by maintainer.
- **Node 24**: every frontend bash command needs `source ~/.nvm/nvm.sh && nvm use 24` (fresh sessions default to Node 20). No `rg` — use `grep`.
- Editor freezes in non-interactive shells: set `GIT_EDITOR=true` for rebase/commit steps.
- Backend = Spring Boot 4.1.1, Java 21, JAR packaging; frontend = Vite 8 + React 19 (plain JSX), oxlint + Vitest. See AGENTS.md.
- Boot 4 auto-starts Docker Compose if `compose.yaml` exists — `spring.docker.compose.enabled=false` already set; don't remove it.
- Verification links appear in the backend console log in dev (ConsoleEmailSender).
- STOMP ERROR frames only reach clients if the client inbound channel runs inline (`SyncTaskExecutor`) AND the interceptor throws a `MessagingException` (`MessageDeliveryException`) — Spring's Java STOMP client delivers them to `StompSessionHandler.handleFrame` (native header `message`), not `handleException`.