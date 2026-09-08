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
  onPrivateRoll, onError})` subscribing to the snapshot destination (`/app/sessions/{id}`),
  the live topic (`/topic/sessions/{id}`) and the GM-user dice queue
  (`/user/queue/dice`); returns `{connect, disconnect, sendChat}`.
- `src/lib/mapGeometry.js` — battle-map math: `RACE_SPEEDS` table (25/30/35/40 ft),
  `squaresFor` (per-turn budget), Chebyshev `distanceInSquares` / `costInFeet`,
  `remainingFeet`/`remainingSquares`, `reachableSquares` (clipped to map edges).
- `src/lib/battleMap.js` — map API helpers over `api()`: `getMap`, `createMap`, `addToken`,
  `updateToken`, `removeToken`, `moveToken`, `turnCommand`, plus initiative helpers
  (`setInitiative`, `rerollInitiative`, `nextInitiative`, `removeInitiativeEntry`) and
  `rollDice` (`POST /api/sessions/{id}/roll`).
- `src/auth/` — auth store (localStorage key `tt.auth`), `AuthProvider` context, `useAuth`,
  session restore via `GET /api/users/me`.
- `src/components/` — `ProtectedRoute`, `ShellLayout` (Sessions / Characters nav),
  `BattleMapPanel` (grid + tokens, select-to-move with reachable-square overlay, GM token
  form + turn bar; controlled via `map`/`onMapChange`), `InitiativeRail` (ordered list with
  current-turn highlight, GM add/reroll/remove + advance), `DiceTray` (expression + label +
  GM-private roll form on the session screen).
- `src/pages/` — `LoginPage`, `RegisterPage` (client-side password + age policies),
  `VerifyPage`, `Dashboard`, `LobbyPage`, `SessionPage`.
- `src/App.jsx` — routes: `/login`, `/register`, `/verify`, `/` (protected), plus
  `/sessions` (lobby) and `/sessions/:id` (live session).

The frontend calls the backend cross-origin (no Vite `/api` proxy). The backend allows the
dev origin (`http://localhost:5173`) via `tabletopserv.cors.allowed-origins`.

## Status

Stage 1 complete: auth UI (register / login / email verification) plus the sessions UI —
lobby with create/join by invite code and a live session view with roster, invite code and
real-time chat/presence over STOMP. Stage 3 track 1 (battle map): `BattleMapPanel` on the
session screen — grid, participant + monster tokens, select-to-move with a reachable-square
overlay, per-turn movement budget, GM token editing and a turn bar; map state streams over
`TABLE` session events (kept out of the chat feed). Stage 3 track 2 (dice + initiative):
`DiceTray` posts server-authoritative rolls to `POST /api/sessions/{id}/roll`; `DICE` events
render in the feed — public rolls show dice + total, GM-private rolls show the result only to
the GM (hidden frame on the topic + full frame on `/user/queue/dice`, merged by `rollId`).
`InitiativeRail` shows the ordered list with the current turn highlighted; GMs set the order
from tokens/custom labels (blank score = auto d20), reroll/remove entries and advance turns.
Platform plan: see the root `README.md`
and `draft-design.md`; repo conventions in `AGENTS.md`.