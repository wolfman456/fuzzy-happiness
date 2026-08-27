# tabletopweb

Frontend for fuzzy-happiness — React 19 + Vite 8, plain JSX (no TypeScript). Oxlint for
linting, Vitest for tests.

## Commands

```sh
npm install              # install dependencies
npm run dev              # Vite dev server
npm run build            # production build
npm run lint             # oxlint
npm run test             # Vitest (no watch; use npm run test:watch for watch mode)
```

## Status

Scaffold + tooling only so far. Auth UI (register/login) and the session/character views
land as the corresponding backend endpoints come online. Platform plan: see the root
`README.md` and `draft-design.md`; repo conventions in `AGENTS.md`.