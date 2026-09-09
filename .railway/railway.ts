/**
 * Fuzzy Happiness — Railway Infrastructure as Code.
 *
 * One editable file for the whole production topology: a managed Postgres
 * database plus the three runnable services (backend, egress gateway, web).
 *
 * Apply with the Railway CLI (manual deploys — no GitHub integration):
 *
 *   railway login
 *   railway link                 # link this repo to the Railway project
 *   railway config plan
 *   railway config apply
 *
 * Then deploy each app manually from its directory:
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
import { defineRailway, group, postgres, preserve, project, service } from "railway/iac";

export default defineRailway(() => {
  const db = postgres("postgres");

  // Egress-only gateway. Spring is its only client; it has no public domain.
  const gateway = service("gateway", {
    start: "node src/index.js",
    healthcheck: "/health",
    replicas: 1,
    env: {
      GATEWAY_TOKEN: preserve(),
    },
  });

  const backend = service("backend", {
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
      GATEWAY_URL: `http://${gateway.env.RAILWAY_PRIVATE_DOMAIN}`,
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