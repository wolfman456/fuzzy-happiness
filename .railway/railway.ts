/**
 * Fuzzy Happiness — Railway Infrastructure as Code.
 *
 * One editable file for the whole production topology: a managed Postgres
 * database plus the three runnable services (backend, egress gateway, web).
 *
 * Auto-deploy: each service now has a `source: github(...)` pointing at the
 * repo and its `rootDirectory`, so pushes to `master` trigger a build in the
 * right app directory automatically.
 *
 * Apply with the Railway CLI (initial setup / local escape hatch):
 *
 *   railway login
 *   railway link                 # link this repo to the Railway project
 *   railway config plan
 *   railway config apply
 *
 * Manual deploys (escape hatch — auto-deploy handles normal pushes):
 *
 *   railway up                   # from tabletopserv/   (Dockerfile build)
 *   railway up                   # from tabletopgateway/ (Railpack)
 *   railway up                   # from tabletopweb/     (Dockerfile build)
 *
 * Secrets are NOT written here — they are set per service in the Railway
 * dashboard and preserved by `preserve()` so a later `config apply` never
 * clobbers them. Required before the backend boots cleanly:
 * JWT_SECRET, GATEWAY_TOKEN (same value on backend + gateway),
 * ADMIN_PASSWORD, SMTP_HOST/PORT/USER/PASSWORD, CORS_ALLOWED_ORIGINS.
 * The web service needs VITE_API_URL at build time (set it, then redeploy).
 */
import { defineRailway, group, github, postgres, preserve, project, service } from "railway/iac";

export default defineRailway(() => {
  const db = postgres("postgres");

  // Egress-only gateway. Spring is its only client; it has no public domain.
  const gateway = service("gateway", {
    source: github("wolfman456/fuzzy-happiness", { branch: "master", rootDirectory: "tabletopgateway" }),
    start: "node src/index.js",
    healthcheck: "/health",
    replicas: 1,
    env: {
      GATEWAY_TOKEN: preserve(),
    },
  });

  const backend = service("backend", {
    source: github("wolfman456/fuzzy-happiness", { branch: "master", rootDirectory: "tabletopserv" }),
    healthcheck: "/actuator/health",
    healthcheckTimeout: 300,
    replicas: 1,
    env: {
      SPRING_PROFILES_ACTIVE: "prod",
      CORS_ALLOWED_ORIGINS: preserve(),
      JWT_SECRET: preserve(),
      ADMIN_PASSWORD: preserve(),
      SMTP_HOST: preserve(),
      SMTP_PORT: preserve(),
      SMTP_USER: preserve(),
      SMTP_PASSWORD: preserve(),
      GATEWAY_URL: "http://gateway.railway.internal",
      GATEWAY_TOKEN: preserve(),
      PGHOST: db.env.PGHOST,
      PGPORT: db.env.PGPORT,
      PGDATABASE: db.env.PGDATABASE,
      PGUSER: db.env.PGUSER,
      PGPASSWORD: db.env.PGPASSWORD,
    },
  });

  // Static SPA (nginx). React-router needs the VITE_API_URL build arg to point
  // at the backend's public URL; set it in the dashboard then redeploy.
  const web = service("web", {
    source: github("wolfman456/fuzzy-happiness", { branch: "master", rootDirectory: "tabletopweb" }),
    healthcheck: "/",
    replicas: 1,
    env: {
      VITE_API_URL: preserve(),
    },
  });

  const game = group("Game services", [web, backend, gateway]);
  const data = group("Data", [db]);

  return project("fuzzy-happiness", {
    resources: [game, data],
  });
});