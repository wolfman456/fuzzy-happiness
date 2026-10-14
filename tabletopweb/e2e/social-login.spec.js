import { expect, test } from '@playwright/test'
import { E2E_USER, mockMe, mockProviders } from './support/api.js'

test.describe('social login', () => {
  test('offers a button per configured provider on the login screen', async ({ page }) => {
    await mockProviders(page)

    await page.goto('/login')

    const google = page.getByRole('link', { name: 'Continue with Google' })
    await expect(google).toBeVisible()
    await expect(google).toHaveAttribute('href', /\/oauth2\/authorization\/google$/)
    await expect(page.getByRole('link', { name: 'Continue with GitHub' })).toBeVisible()
  })

  test('hides the provider section when none are configured', async ({ page }) => {
    await mockProviders(page, [])

    await page.goto('/login')

    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
    await expect(page.getByText('or continue with')).toHaveCount(0)
  })

  test('adopts the token handed back on the OAuth callback route', async ({ page }) => {
    await mockMe(page, E2E_USER)
    await mockProviders(page, [])

    await page.goto('/oauth/callback#token=e2e-token&tokenType=Bearer&expiresInSeconds=86400')

    await expect(page.getByRole('heading', { name: `Welcome, ${E2E_USER.displayName}` })).toBeVisible()
    const stored = await page.evaluate(() => JSON.parse(localStorage.getItem('tt.auth')))
    expect(stored.token).toBe('e2e-token')
    expect(stored.user.username).toBe(E2E_USER.username)
  })

  test('surfaces a provider failure on the login screen', async ({ page }) => {
    await mockProviders(page, [])

    await page.goto('/login?oauthError=social_login_failed&oauthMessage=Email%20already%20linked')

    await expect(page.getByRole('alert')).toHaveText('Email already linked')
  })
})
