import { defineConfig, devices } from '@playwright/test'

// End-to-end (functional) tests for the built UI. Run with `npm run test:e2e`.
//
// These drive a real browser against the Vite dev server. The backend is not
// started: each spec intercepts the API with `page.route` (see e2e/support),
// and VITE_API_URL is pinned to the dev-server origin so those calls are
// same-origin (no CORS preflight to fake). Unit tests stay in Vitest.
//
// Coverage policy: keep the frontend at >= 90% unit (Vitest) and >= 80%
// functional (these E2E flows) — see AGENTS.md.
const PORT = Number(process.env.PLAYWRIGHT_PORT ?? 5173)
const BASE_URL = process.env.PLAYWRIGHT_BASE_URL ?? `http://localhost:${PORT}`

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: `npm run dev -- --port ${PORT} --strictPort`,
    url: BASE_URL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      VITE_API_URL: BASE_URL,
    },
  },
})
