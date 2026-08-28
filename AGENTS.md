# AGENTS.md

## Repo layout

Two independent apps at the repo root inside their own directories. There is **no root `package.json` or `pom.xml`** — all commands run from the subdirectory.

- `tabletopserv/` — Spring Boot 4.1.1 (Java 21) backend, Maven wrapper (`./mvnw`).
- `tabletopweb/` — Vite 8 + React 19 frontend, plain JSX (no TypeScript), Tailwind CSS v4, react-router.
- Node 24 is pinned everywhere (`tabletopweb/.nvmrc`, `engines >= 24`, CI matrix). Ensure `nvm use 24` before frontend commands.

## Commands

```sh
# Backend (runs from tabletopserv/)
./mvnw spring-boot:run      # dev server
./mvnw test                 # backend tests

# Frontend (runs from tabletopweb/)
npm run dev                 # Vite dev server
npm run build               # production build
npm run lint                # oxlint (NOT eslint)
npm run test                # Vitest (no watch; use `npm run test:watch` for watch mode)

## Backend gotchas

- Boot 4, not Boot 2/3: the pom uses `spring-boot-starter-webmvc` / `webflux` (no `spring-boot-starter-web`).
- Builds as an executable **JAR** with embedded Tomcat — run outside a servlet container. (It used to package as a WAR with `ServletInitializer`; do not reintroduce it.)
- Java sources live under package `com.gamer.fowever.tabletopserv` even though the pom `groupId` is `com.gamer.table` — put new code in the `com.gamer.fowever.tabletopserv` package to match existing sources (`tabletopserv/src/main/java/com/gamer/fowever/tabletopserv/`).
- `compose.yaml` is an empty placeholder (`services: { }`) and JPA/JDBC are included but unconfigured. Do not assume a database is running; the `dev` profile uses in-memory H2. `application.properties` carries the full config surface (`tabletopserv.*` keys) with env overrides.
- CORS is configured server-side for the Vite dev origin: `tabletopserv.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`). The frontend calls the backend cross-origin via `VITE_API_URL` (default `http://localhost:8080`) — there is **no** Vite `/api` proxy.
- Frontend style: Tailwind v4 via the `@tailwindcss/vite` plugin (import `tailwindcss` in `index.css`); routing with `react-router-dom`; auth state lives in `src/auth/` (localStorage + context); API calls go through `src/lib/api.js`.

## Frontend gotchas

- Tests run under Vitest + jsdom with `globals: true` (required for `@testing-library/react` auto-cleanup) and `setupFiles: ./src/setupTests.js` (jest-dom). Tests only work on Node ≥ 24: `source ~/.nvm/nvm.sh && nvm use 24`.

## CI

- `.github/workflows/node.js.yml` — builds/tests the frontend on push/PR to `develop` (Node 24, `setup-node` caches via `cache-dependency-path: tabletopweb/package-lock.json`, runs in `tabletopweb/`). The frontend pins Node 24 (`tabletopweb/.nvmrc`, `engines >= 24`).
- `.github/workflows/maven.yml` — builds/tests the backend with `./mvnw test` on push/PR to `develop` (JDK 21, runs in `tabletopserv/`).
- `.github/workflows/maven-publish.yml` — builds and deploys the backend to GitHub Packages on releases (JDK 21); needs a valid GH token and relies on the `distributionManagement` block in the pom.

## Contribution workflow (per Wants.md)

- Default branch is `develop`; never merge to `develop`/`master` yourself — the maintainer reviews and merges PRs.
- File feature requests in Wants.md first, framed as: "As a user I want ... so I can ...."
- Work on topic branches. When the user asks for a new branch, name it `feature/<short-meaningful-slug>` based on the planned work (e.g. `feature/srd-integration`), e.g. `git checkout -b feature/<slug>`. This will later change to a ticket-number prefix (e.g. `feature/ABC-123-description`) once ticket tracking is set up.
- Write clear, descriptive commit messages. A commit message should read like a changelog entry: a concise summary line of what was done, then a body listing the key changes and reasoning — so anyone looking back at history can identify exactly what took place and why. Reference the relevant `draft-design.md` section or requirement when a change implements one.

## Testing policy

- Every plan and every code change ships with unit tests and keeps **line coverage ≥ 90%** (jacoco `check` gate bound to the `test` phase in `tabletopserv/pom.xml` fails the build below that threshold). Backend tests run with `./mvnw test` from `tabletopserv/`.

## Documentation

- Keep `draft-design.md` and the README(s) in sync with the code. When a change ships a documented behavior — new endpoint, decided open question, config surface or dependency — update the sections that describe it (status banner, API surface, roadmap, tech notes, "decided" list) in the **same change**. Docs are part of the deliverable.