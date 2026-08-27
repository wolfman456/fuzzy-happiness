# AGENTS.md

## Repo layout

Two independent apps at the repo root inside their own directories. There is **no root `package.json` or `pom.xml`** — all commands run from the subdirectory.

- `tabletopserv/` — Spring Boot 4.1.1 (Java 21) backend, Maven wrapper (`./mvnw`).
- `tabletopweb/` — Vite 8 + React 19 frontend, plain JSX (no TypeScript).

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
- `compose.yaml` is an empty placeholder (`services: { }`) and `application.properties` only sets the app name. Do not assume a database is running; JPA/JDBC are included but unconfigured.

## CI

- `.github/workflows/node.js.yml` — builds/tests the frontend on push/PR to `develop` (Node 22/24, `setup-node` caches via `cache-dependency-path: tabletopweb/package-lock.json`, runs in `tabletopweb/`).
- `.github/workflows/maven.yml` — builds/tests the backend with `./mvnw test` on push/PR to `develop` (JDK 21, runs in `tabletopserv/`).
- `.github/workflows/maven-publish.yml` — builds and deploys the backend to GitHub Packages on releases (JDK 21); needs a valid GH token and relies on the `distributionManagement` block in the pom.

## Contribution workflow (per Wants.md)

- Default branch is `develop`; never merge to `develop`/`master` yourself — the maintainer reviews and merges PRs.
- File feature requests in Wants.md first, framed as: "As a user I want ... so I can ...."
- Work on topic branches. When the user asks for a new branch, name it `feature/<short-meaningful-slug>` based on the planned work (e.g. `feature/srd-integration`), e.g. `git checkout -b feature/<slug>`. This will later change to a ticket-number prefix (e.g. `feature/ABC-123-description`) once ticket tracking is set up.