# Railway (Infrastructure as Code)

This directory defines the whole Railway project — managed Postgres plus the three
services (backend, egress gateway, web). It replaces the deprecated `railway.toml`
config-as-code; see the file header of `railway.ts` for the authored shape.

## Commands

```sh
railway login
railway link                # link this repo to the Railway project
railway config plan         # preview changes
railway config apply        # apply after review
```

> Requires the Railway CLI and, in the repo, the IaC SDK: `npm install` at the
> repo root (devDependency `railway`).

## Manual deploys

Each app deploys from its own directory (manual CLI deploys — no GitHub integration):

```sh
railway up    # tabletopserv/    Dockerfile build -> backend
railway up    # tabletopgateway/ Railpack build   -> gateway
railway up    # tabletopweb/     Dockerfile build -> web
```

## Secrets to set in the dashboard (per service environment)

Backend boot requires (prod profile): `JWT_SECRET`, `ADMIN_PASSWORD`,
`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `CORS_ALLOWED_ORIGINS`,
`GATEWAY_TOKEN`. The gateway needs the **same** `GATEWAY_TOKEN`. The web service
needs `VITE_API_URL` (backend's public URL, e.g. `https://<backend>.up.railway.app`)
set **before building** — then redeploy so the bundle picks it up.

All are managed with `preserve()` in `railway.ts`, so dashboard values survive a
`railway config apply`.