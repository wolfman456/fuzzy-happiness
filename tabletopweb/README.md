# tabletopweb

Frontend for fuzzy-happiness — React 19 + Vite 8, plain JSX (no TypeScript). Tailwind CSS v4
(via the `@tailwindcss/vite` plugin), react-router for routing, oxlint for linting, Vitest for
tests. Requires Node ≥ 24.

## Commands

```sh
npm install              # install dependencies
npm run dev              # Vite dev server (http://localhost:5173)
npm run build            # production build
npm run lint             # oxlint
npm run test             # Vitest (no watch; use npm run test:watch for watch mode)
```

## App map

- `src/lib/api.js` — `fetch` wrapper: base URL from `VITE_API_URL` (default
  `http://localhost:8080`), JWT bearer injection, HTTP error mapping (`ApiError`), plus
  session/game helpers (`listGames`, `createSession`, `getSession`, `joinSession`, `leaveSession`).
- `src/lib/stomp.js` — `@stomp/stompjs` wrapper: `stompBrokerUrl()` (Vite base → `ws`, injects
  the JWT as a `?token=` param) and `createRealtimeClient({sessionId, onSnapshot, onEvent,
  onError})` subscribing to the snapshot destination (`/app/sessions/{id}`) and the live topic
  (`/topic/sessions/{id}`); returns `{connect, disconnect, sendChat}`.
- `src/auth/` — auth store (localStorage key `tt.auth`), `AuthProvider` context, `useAuth`,
  session restore via `GET /api/users/me`.
- `src/components/` — `ProtectedRoute`, `ShellLayout` (Sessions / Characters nav).
- `src/pages/` — `LoginPage`, `RegisterPage` (client-side password + age policies),
  `VerifyPage`, `Dashboard`, `LobbyPage`, `SessionPage`.
- `src/App.jsx` — routes: `/login`, `/register`, `/verify`, `/` (protected), plus
  `/sessions` (lobby) and `/sessions/:id` (live session).

The frontend calls the backend cross-origin (no Vite `/api` proxy). The backend allows the
dev origin (`http://localhost:5173`) via `tabletopserv.cors.allowed-origins`.

## Status

Stage 1 complete: auth UI (register / login / email verification) plus the sessions UI —
lobby with create/join by invite code and a live session view with roster, invite code and
real-time chat/presence over STOMP. Platform plan: see the root `README.md` and
`draft-design.md`; repo conventions in `AGENTS.md`.