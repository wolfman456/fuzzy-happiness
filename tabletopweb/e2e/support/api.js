// Helpers for stubbing the backend from Playwright. The E2E suite runs the UI in a
// real browser without the Spring backend, so every API call a spec exercises is
// intercepted with `page.route`. VITE_API_URL is pinned to the dev-server origin
// (playwright.config.js) so these calls are same-origin and need no CORS headers.

export const E2E_USER = {
  id: 1,
  username: 'aria',
  displayName: 'Aria',
  email: 'aria@example.com',
  role: 'USER',
  emailVerified: true,
}

export function jsonResponse(status, body) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  }
}

export function mockLoginSuccess(page, user = E2E_USER) {
  return page.route('**/api/auth/login', (route) =>
    route.fulfill(
      jsonResponse(200, {
        token: 'e2e-token',
        tokenType: 'Bearer',
        expiresInSeconds: 86_400,
        user,
      }),
    ),
  )
}

export function mockLoginFailure(page, message = 'Authentication required') {
  return page.route('**/api/auth/login', (route) =>
    route.fulfill(jsonResponse(401, { status: 401, message })),
  )
}

export function mockRegisterSuccess(page, user = E2E_USER) {
  return page.route('**/api/auth/register', (route) => route.fulfill(jsonResponse(201, user)))
}
