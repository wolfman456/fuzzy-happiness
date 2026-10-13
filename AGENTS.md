# AGENTS.md

## Repo layout

Three independent apps at the repo root inside their own directories. There is **no root `pom.xml`** — all Java/Node commands run from the subdirectory. One root `package.json` exists **only** for the Railway infras-as-code CLI (`railway` devDependency, used by `.railway/railway.ts`); it is not an app.

- `tabletopserv/` — Spring Boot 4.1.1 (Java 21) backend, Maven wrapper (`./mvnw`). Now a **multi-module Maven build** (parent aggregator pom + modules):
  - `tabletopapi/` — the **inbound REST API as interfaces + DTOs** (package `com.gamer.fowever.tabletopapi`). No domain/repository/business logic — the contract the service implements.
  - `tabletopservice/` — the runnable module (package `com.gamer.fowever.tabletopservice`): controller implementations of the api interfaces, services, domain, repositories, security, config, STOMP glue, `application*.properties`. Produces the executable JAR.
  - `tabletopfunctionaltest/` — **functional/E2E module** (package `com.gamer.fowever.tabletopfunctionaltest`): `*IT` journey suites (auth, session/STOMP, dice, battle map/initiative, monster, SRD) that boot the packaged `tabletopservice` JAR as a subprocess against a Testcontainers Postgres + a recorded-fixture gateway stub. No jacoco gate. The JAR-subprocess harness (`support/BackendInstance`) supports a Docker-less escape hatch via `-Dtabletopserv.functional.db-url=jdbc:h2:mem:...` (see design-v1 §18).
- `tabletopweb/` — Vite 8 + React 19 frontend, plain JSX (no TypeScript), Tailwind CSS v4, react-router.
- `tabletopgateway/` — Express 5 **egress gateway** (Node 24, ESM, plain JS). The only path out of the backend to internet upstreams (SRD `dnd5eapi.co`, future monster-gen LLM). Spring calls it server-to-server with an `X-Gateway-Token`; it does **not** see client traffic.
- Node 24 is pinned everywhere (`tabletopweb/.nvmrc`, `tabletopgateway/.nvmrc`, `engines >= 24`, CI matrix). Ensure `nvm use 24` before any Node command.

## Commands

```sh
# Backend (run from tabletopserv/ — root aggregator)
./mvnw test                       # build + test ALL modules (api + service), jacoco gate; unit-only
./mvnw verify                     # above + functional/E2E ITs (needs Docker for Testcontainers Postgres)
./mvnw verify -Dtabletopserv.functional.db-url=jdbc:h2:mem:ft   # ITs without Docker (H2 escape hatch)
./mvnw -pl tabletopservice -am spring-boot:run   # dev server (builds api first)
./mvnw -pl tabletopapi -am package               # build just the api library
./mvnw package                    # package the whole reactor

# Frontend (runs from tabletopweb/)
npm run dev                 # Vite dev server
npm run build               # production build
npm run lint                # oxlint (NOT eslint)
npm run test                # Vitest unit tests (no watch; use `npm run test:watch` for watch mode)
npm run test:e2e            # Playwright functional/E2E (starts the Vite dev server)

# Gateway (runs from tabletopgateway/)
npm run dev                 # nodemon/node --watch dev server on GATEWAY_PORT (default 3001)
npm start                   # production server
npm run test                # Vitest (no watch)
npm run lint                # oxlint
```

## Backend gotchas

- Boot 4, not Boot 2/3: starter deps are `spring-boot-starter-webmvc` / `webflux` (never `spring-boot-starter-web`).
- Builds as an executable **JAR** with embedded Tomcat; the runnable module is `tabletopservice` (the `spring-boot-maven-plugin` repackaging lives there, not on the parent or `tabletopapi`). Run outside a servlet container. (It used to package as a WAR with `ServletInitializer`; do not reintroduce it.)
- **Module split rule:** the inbound REST API lives in `com.gamer.fowever.tabletopapi` (interface + DTOs + shared error types only). Implementations (`*ControllerImpl implements *Api`), business logic, entities, repositories, security and config all live in `com.gamer.fowever.tabletopservice`. Interfaces reference only DTOs/`Authentication` — never `domain.*` types; the impl casts the principal. APIs must not depend on the service module (inversion of dependency).
- STOMP glue (`SessionEventsController`, interceptors) stays in `tabletopservice` — it is transport plumbing, not part of the REST interface contract; its DTOs live in `tabletopapi`.
- jacoco **line coverage ≥ 90% gate applies to `tabletopservice` only** (the api module is interfaces + records, which would fail a strict gate). Do not reintroduce a strict gate on `tabletopapi`.
- `compose.yaml` is an empty placeholder (`services: { }`) and JPA/JDBC are included but unconfigured. Do not assume a database is running; the `dev` profile uses in-memory H2. `application.properties` carries the full config surface (`tabletopserv.*` keys) with env overrides.
- CORS is configured server-side for the Vite dev origin: `tabletopserv.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`). The frontend calls the backend cross-origin via `VITE_API_URL` (default `http://localhost:8080`) — there is **no** Vite `/api` proxy.
- **Outbound calls:** the backend never talks to the internet directly. All upstream traffic goes through the gateway: `GatewayClient`/`SrdClient` (WebClient) call `tabletopserv.gateway.url` (env `GATEWAY_URL`, default `http://localhost:3001`) with `tabletopserv.gateway.token` (env `GATEWAY_TOKEN`) as `X-Gateway-Token`. New outbound integrations must route through the gateway, never a raw WebClient.
- Frontend style: Tailwind v4 via the `@tailwindcss/vite` plugin (import `tailwindcss` in `index.css`); routing with `react-router-dom`; auth state lives in `src/auth/` (localStorage + context); API calls go through `src/lib/api.js`.

## Frontend gotchas

- Unit tests run under Vitest + jsdom with `globals: true` (required for `@testing-library/react` auto-cleanup) and `setupFiles: ./src/setupTests.js` (jest-dom); Vitest excludes `e2e/**`. Functional/E2E lives in Playwright under `tabletopweb/e2e/` (`npm run test:e2e`, Chromium installed with `npx playwright install chromium`; CI installs it with `--with-deps`). Tests only work on Node ≥ 24: `source ~/.nvm/nvm.sh && nvm use 24`.

## Gateway gotchas

- Node 24, ESM (`"type": "module"`), plain JS (no TypeScript), Express 5 + a native `fetch`-based forwarder (no `http-proxy-middleware` — full response buffering is needed for the TTL-cached stale fallback, response-size cap and single retry). Tests are Vitest + supertest.
- Deny-by-default: every upstream is a named route in `src/routes.js` with an allowlisted set of path prefixes and query params; anything unmatched returns `403`. The SRD route forwards to `https://www.dnd5eapi.co/api/2014`.
- The gateway is egress-only: it binds where Spring can reach it (loopback by default in dev), enforces `X-Gateway-Token`, performs SSRF defense-in-depth (no dynamic hosts, private-range blocking), TTL-caches SRD GETs, and returns `502 {status,message}` when an upstream is unreachable.

## Deploy (Railway)

- **Target:** one Railway project on the **Hobby** plan — managed **Postgres** + three services (`backend`, `gateway`, `web`) wired via **Infrastructure-as-Code** in `.railway/railway.ts` (Railway's legacy `railway.toml` config-as-code is deprecated, cutoff 2026-12-01). Deploys are **manual CLI** (`railway up` from the app dir) — there is **no GitHub integration and no CI deploy step**; documented in `design-v1.md` §19.
- **Ports/binds:** the backend prod profile reads `server.port=${PORT:8080}` and a Postgres JDBC URL built from `PGHOST`/`PGPORT`/`PGDATABASE`/`PGUSER`/`PGPASSWORD` (`application-prod.properties`). The gateway's `resolveListenConfig()` binds `PORT` → `GATEWAY_PORT` → 3001 and uses `0.0.0.0` when a container `PORT` is present (loopback stays the dev default). The web nginx image renders `listen ${PORT}` via the official envsubst entrypoint.
- **Priv network:** services reference each other as `<service>.railway.internal`; the gateway gets **no public domain**. Backend needs `GATEWAY_URL=http://gateway.railway.internal` and `GATEWAY_TOKEN` echoed in the gateway's own env. CORS: set `CORS_ALLOWED_ORIGINS` on the backend to the web service's public URL (currently `https://gamenight.bond,https://www.gamenight.bond,https://web-production-74bb0.up.railway.app`).
- **Build-time var:** web `VITE_API_URL` is baked at build — change the dashboard var and **redeploy** the web service. Recent deploys: with `railway up` the local working tree builds; the dashboard var must be set before the build.
- **Secrets:** per-service dashboard vars; `.railway/railway.ts` marks them `preserve()` so `railway config apply` never overwrites. `ddl-auto` is `validate` in prod; **Flyway owns the prod schema** (Slice 0 landed: `V1__baseline.sql`, `baseline-on-migrate=true` at V1). Add forward changes as new `V<n>__*.sql` under `tabletopservice/src/main/resources/db/migration` — never edit an applied migration. Dev/H2 and the functional ITs stay Hibernate-managed (`spring.flyway.enabled=false`, set in `application.properties`).
- Run the IaC SDK commands from the repo root with Node 24: `npx railway@latest login/link/config plan/config apply` (the root `package.json` pins the `railway` devDependency). `railway config plan` is safe; `config apply` creates the scaffolded resources and needs explicit user go-ahead.

## Versioning & releases

- **One unified semver across the whole repo** (see `RELEASES.md` for the scheme, history and the full cut-a-release checklist): `tabletopserv/pom.xml` (parent aggregator, inherited by the 3 modules), `tabletopweb/package.json` (+ lockfile), `tabletopgateway/package.json` (+ lockfile), repo-root `package.json`. Bump them all together — never one in isolation.
- **`develop` = `X.Y.Z-SNAPSHOT`** (the next release); **`master` = `X.Y.Z`** (the live release) + annotated tag `vX.Y.Z`. Bump policy: breaking → major, feature → minor, bug fix → patch.
- **Release step (round-close):** after the last PR propagates `develop` → `master`, strip `-SNAPSHOT` on `master`, commit "Release X.Y.Z", tag `vX.Y.Z` and push, then bump `develop` to `X.(Y+1).0-SNAPSHOT`. Keep `RELEASES.md` history current in the same change.

## CI

- `.github/workflows/node.js.yml` — builds/tests the frontend on push to `develop` and on **every** `pull_request` (no base-branch filter, so stacked PRs are covered too) — two jobs: `build` (lint + prod build + Vitest) and `e2e` (installs Chromium `--with-deps`, runs Playwright, uploads the report). Node 24, `setup-node` caches via `cache-dependency-path: tabletopweb/package-lock.json`, runs in `tabletopweb/`. The frontend pins Node 24 (`tabletopweb/.nvmrc`, `engines >= 24`).
- `.github/workflows/gateway.yml` — builds/tests the gateway on push to `develop` and on **every** `pull_request` (no base-branch filter, so stacked PRs are covered) (Node 24, runs in `tabletopgateway/`).
- `.github/workflows/maven.yml` — builds/tests the backend reactor with `./mvnw test` on push to `develop` and on **every** `pull_request` (no base-branch filter, so stacked PRs are covered) (JDK 21, runs in `tabletopserv/`).
- `.github/workflows/maven-publish.yml` — builds and deploys the runnable backend to GitHub Packages on releases (JDK 21, `./mvnw -pl tabletopservice -am deploy`); needs a valid GH token and relies on the `distributionManagement` block in the pom.

## Contribution workflow (per Wants.md)

- Default branch is `develop`; never merge to `develop`/`master` yourself — the maintainer reviews and merges PRs.
- **`develop` is protected:** the base ruleset requires **2 approving reviews** (+ code-owner review), so a plain `gh pr merge` is refused. The maintainer grants a standing, *per-PR* authorization for the agent to use `gh pr merge <n> --admin --merge --delete-branch`. **Always ask for explicit approval before admin-merging**, then after the merge run the post-merge bookkeeping on `develop`: sync the local branch (`git checkout develop && git pull --ff-only`), flip the now-shipped Wants.md markers (`🔨 in progress` → `✅ landed`), and move the tracked board item to `Done` via the Projects v2 GraphQL status field (see session notes for the item/field IDs).
- **Every accepted PR propagates to `master`:** each time a PR is merged into `develop`, `develop` is then merged into `master` so the release branch stays current (`git checkout develop && git pull --ff-only`, `git checkout master && git pull --ff-only && git merge --ff-only develop && git push origin master`). Do this as part of the same post-merge step, before starting the next feature.
- File feature requests in Wants.md first, framed as: "As a user I want ... so I can ...."
- Work on topic branches. When the user asks for a new branch, name it `feature/<short-meaningful-slug>` based on the planned work (e.g. `feature/srd-integration`), e.g. `git checkout -b feature/<slug>`. This will later change to a ticket-number prefix (e.g. `feature/ABC-123-description`) once ticket tracking is set up.
- Write clear, descriptive commit messages. A commit message should read like a changelog entry: a concise summary line of what was done, then a body listing the key changes and reasoning — so anyone looking back at history can identify exactly what took place and why. Reference the relevant `design-v1.md` section or requirement when a change implements one.

## Testing policy

- Every plan and every code change ships with unit tests and keeps **line coverage ≥ 90%** on the `tabletopservice` module (jacoco `check` gate bound to the `test` phase in `tabletopserv/tabletopservice/pom.xml` fails the build below that threshold). Backend tests run with `./mvnw test` from `tabletopserv/`.
- **Coverage targets (policy, going forward):** all code is covered by **≥ 80% functional tests** *and* **≥ 90% unit tests**. The 90% unit target is the enforced `tabletopservice` jacoco gate; the 80% functional target is a **PR/review guardrail** over the `*IT` journey suites (the packaged-JAR functional run), i.e. a change that adds behaviour must add functional coverage for it until/unless the functional run grows its own enforced gate.
- **Functional/E2E** (`tabletopfunctionaltest/`, §18): `*IT` journey suites run via failsafe at **`./mvnw verify`** only; `./mvnw test` stays unit-only. `verify` needs Docker for the Testcontainers Postgres; use `-Dtabletopserv.functional.db-url=jdbc:h2:mem:...` when Docker is unavailable. CI runs `./mvnw -B verify` on every push/PR to `develop`.
- **Run the functional suite before every commit/push:** `./mvnw verify` must pass **before** committing or pushing any change (never commit/push on a unit-only green run). When Docker is unavailable, use the H2 escape hatch (`./mvnw verify -Dtabletopserv.functional.db-url=jdbc:h2:mem:ft`); Postgres-only suites do **not** skip — `FlywayMigrationIT` falls back to a rootless embedded Postgres binary (`io.zonky.test:embedded-postgres`, no Docker/sudo), so the baseline is still validated locally. CI remains the backstop, but the local run is required first.
- **Frontend (Vite app) functional/E2E — same rule, Playwright:** the `tabletopweb/` app follows the same guardrails as the backend, with the functional/E2E layer in **Playwright** (the unit layer stays Vitest): **≥ 90% unit / ≥ 80% functional** coverage targets, and the **Playwright suite must pass before every commit/push** that touches the web app (mirroring `./mvnw verify`). Playwright is wired up: specs live in `tabletopweb/e2e/` (`*.spec.js`, run with `npm run test:e2e`; the Vite dev server is started automatically) and each spec stubs the backend with `page.route` (no Spring/Postgres needed). Unit specs stay under `src/**` — Vitest excludes `e2e/**` and Playwright is confined to `e2e/`. Any change that adds web behaviour must add Playwright coverage for it.

## Documentation

- Keep `design-v1.md` and the README(s) in sync with the code. When a change ships a documented behavior — new endpoint, decided open question, config surface or dependency — update the sections that describe it (status banner, API surface, roadmap, tech notes, "decided" list) in the **same change**. Docs are part of the deliverable.