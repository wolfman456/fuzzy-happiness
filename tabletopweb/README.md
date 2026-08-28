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
  `http://localhost:8080`), JWT bearer injection, HTTP error mapping (`ApiError`).
- `src/auth/` — auth store (localStorage key `tt.auth`), `AuthProvider` context, `useAuth`,
  session restore via `GET /api/users/me`.
- `src/components/` — `ProtectedRoute`, `ShellLayout` (stub Sessions / Characters nav).
- `src/pages/` — `LoginPage`, `RegisterPage` (client-side password + age policies),
  `VerifyPage`, `Dashboard`.
- `src/App.jsx` — routes: `/login`, `/register`, `/verify`, `/` (protected).

The frontend calls the backend cross-origin (no Vite `/api` proxy). The backend allows the
dev origin (`http://localhost:5173`) via `tabletopserv.cors.allowed-origins`.

## Status

Auth UI complete: register / login / email verification, protected dashboard with stub
session & character views. Platform plan: see the root `README.md` and `draft-design.md`;
repo conventions in `AGENTS.md`.