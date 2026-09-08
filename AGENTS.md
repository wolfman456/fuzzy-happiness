# AGENTS.md

## Repo layout

Three independent apps at the repo root inside their own directories. There is **no root `package.json` or `pom.xml`** — all commands run from the subdirectory.

- `tabletopserv/` — Spring Boot 4.1.1 (Java 21) backend, Maven wrapper (`./mvnw`). Now a **multi-module Maven build** (parent aggregator pom + modules):
  - `tabletopapi/` — the **inbound REST API as interfaces + DTOs** (package `com.gamer.fowever.tabletopapi`). No domain/repository/business logic — the contract the service implements.
  - `tabletopservice/` — the runnable module (package `com.gamer.fowever.tabletopservice`): controller implementations of the api interfaces, services, domain, repositories, security, config, STOMP glue, `application*.properties`. Produces the executable JAR.
  - `tabletopfunctionaltest/` — **blank for now** (parent `<modules>` entry commented out); reserved for functional/E2E testing on top of unit tests (see draft-design §18).
- `tabletopweb/` — Vite 8 + React 19 frontend, plain JSX (no TypeScript), Tailwind CSS v4, react-router.
- `tabletopgateway/` — Express 5 **egress gateway** (Node 24, ESM, plain JS). The only path out of the backend to internet upstreams (SRD `dnd5eapi.co`, future monster-gen LLM). Spring calls it server-to-server with an `X-Gateway-Token`; it does **not** see client traffic.
- Node 24 is pinned everywhere (`tabletopweb/.nvmrc`, `tabletopgateway/.nvmrc`, `engines >= 24`, CI matrix). Ensure `nvm use 24` before any Node command.

## Commands

```sh
# Backend (run from tabletopserv/ — root aggregator)
./mvnw test                       # build + test ALL modules (api + service), jacoco gate
./mvnw -pl tabletopservice -am spring-boot:run   # dev server (builds api first)
./mvnw -pl tabletopapi -am package               # build just the api library
./mvnw package                    # package the whole reactor

# Frontend (runs from tabletopweb/)
npm run dev                 # Vite dev server
npm run build               # production build
npm run lint                # oxlint (NOT eslint)
npm run test                # Vitest (no watch; use `npm run test:watch` for watch mode)

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

- Tests run under Vitest + jsdom with `globals: true` (required for `@testing-library/react` auto-cleanup) and `setupFiles: ./src/setupTests.js` (jest-dom). Tests only work on Node ≥ 24: `source ~/.nvm/nvm.sh && nvm use 24`.

## Gateway gotchas

- Node 24, ESM (`"type": "module"`), plain JS (no TypeScript), Express 5 + a native `fetch`-based forwarder (no `http-proxy-middleware` — full response buffering is needed for the TTL-cached stale fallback, response-size cap and single retry). Tests are Vitest + supertest.
- Deny-by-default: every upstream is a named route in `src/routes.js` with an allowlisted set of path prefixes and query params; anything unmatched returns `403`. The SRD route forwards to `https://www.dnd5eapi.co/api/2014`.
- The gateway is egress-only: it binds where Spring can reach it (loopback by default in dev), enforces `X-Gateway-Token`, performs SSRF defense-in-depth (no dynamic hosts, private-range blocking), TTL-caches SRD GETs, and returns `502 {status,message}` when an upstream is unreachable.

## CI

- `.github/workflows/node.js.yml` — builds/tests the frontend on push/PR to `develop` (Node 24, `setup-node` caches via `cache-dependency-path: tabletopweb/package-lock.json`, runs in `tabletopweb/`). The frontend pins Node 24 (`tabletopweb/.nvmrc`, `engines >= 24`).
- `.github/workflows/gateway.yml` — builds/tests the gateway on push/PR to `develop` (Node 24, runs in `tabletopgateway/`).
- `.github/workflows/maven.yml` — builds/tests the backend reactor with `./mvnw test` on push/PR to `develop` (JDK 21, runs in `tabletopserv/`).
- `.github/workflows/maven-publish.yml` — builds and deploys the runnable backend to GitHub Packages on releases (JDK 21, `./mvnw -pl tabletopservice -am deploy`); needs a valid GH token and relies on the `distributionManagement` block in the pom.

## Contribution workflow (per Wants.md)

- Default branch is `develop`; never merge to `develop`/`master` yourself — the maintainer reviews and merges PRs.
- File feature requests in Wants.md first, framed as: "As a user I want ... so I can ...."
- Work on topic branches. When the user asks for a new branch, name it `feature/<short-meaningful-slug>` based on the planned work (e.g. `feature/srd-integration`), e.g. `git checkout -b feature/<slug>`. This will later change to a ticket-number prefix (e.g. `feature/ABC-123-description`) once ticket tracking is set up.
- Write clear, descriptive commit messages. A commit message should read like a changelog entry: a concise summary line of what was done, then a body listing the key changes and reasoning — so anyone looking back at history can identify exactly what took place and why. Reference the relevant `draft-design.md` section or requirement when a change implements one.

## Testing policy

- Every plan and every code change ships with unit tests and keeps **line coverage ≥ 90%** on the `tabletopservice` module (jacoco `check` gate bound to the `test` phase in `tabletopserv/tabletopservice/pom.xml` fails the build below that threshold). Backend tests run with `./mvnw test` from `tabletopserv/`.
- Functional/E2E testing strategy (future, `tabletopfunctionaltest/`) is documented in `draft-design.md` §18; the module is blank and commented out of the parent until implemented.

## Documentation

- Keep `draft-design.md` and the README(s) in sync with the code. When a change ships a documented behavior — new endpoint, decided open question, config surface or dependency — update the sections that describe it (status banner, API surface, roadmap, tech notes, "decided" list) in the **same change**. Docs are part of the deliverable.