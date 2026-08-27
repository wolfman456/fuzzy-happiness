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
  classes, spells, equipment and more are fetched and cached through the backend, so no
  rules data is hard-coded into the app.
- **Discord for voice** — connect your Discord account and jump into a voice channel;
  the web app runs beside it as the shared game table.
- **Database** — H2 while developing; PostgreSQL once deployed to production.

## Tech stack

- Frontend: tabletopweb/ — React 19, Vite 8, plain JSX (oxlint, Vitest)
- Backend: tabletopserv/ — Spring Boot 4.1.1, Java 21, Maven wrapper
- Auth: Spring Security — 24h JWT bearer (jjwt), bcrypt, `USER`/`MODERATOR`/`ADMIN` roles, email verification
- Rules data: D&D 5e SRD API (5e-bits/dnd5eapi.co), proxied + cached by the backend
- Persistence: JPA (H2 dev / PostgreSQL prod via Spring profiles)

See `AGENTS.md` for repo layout, commands, and conventions.

## Status

Iterative build; design draft in [`draft-design.md`](draft-design.md) (Draft v0.4).

Delivered:

- Core domain model + JPA persistence (users, games, characters, sessions) — `feature/java-start` (PR #9).
- Accounts & auth — `feature/spring-security`: register / login (username or email), 24h JWT,
  roles, bcrypt, email verification with resend; bootstrap admin in dev. 58 backend tests,
  jacoco line-coverage gate ≥ 90%.

Next: real-time sessions (WebSocket/STOMP lobby, invite codes, chat + presence), then
character generation backed by the 5e SRD proxy, then the game table (dice, initiative).