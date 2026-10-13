import { expect, test } from '@playwright/test'
import { E2E_USER, mockLoginFailure, mockLoginSuccess } from './support/api.js'

test.describe('authentication', () => {
  test('redirects an unauthenticated visitor from the dashboard to login', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
  })

  test('a valid login lands on the dashboard and persists the session', async ({ page }) => {
    await mockLoginSuccess(page)

    await page.goto('/login')
    await page.locator('#identifier').fill(E2E_USER.username)
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page.getByRole('heading', { name: `Welcome, ${E2E_USER.displayName}` })).toBeVisible()
    await expect(page.getByText(`(${E2E_USER.username})`)).toBeVisible()

    const stored = await page.evaluate(() => JSON.parse(localStorage.getItem('tt.auth')))
    expect(stored.token).toBe('e2e-token')
    expect(stored.user.username).toBe(E2E_USER.username)
  })

  test('an invalid login shows an inline error and stays on the login screen', async ({ page }) => {
    await mockLoginFailure(page)

    await page.goto('/login')
    await page.locator('#identifier').fill('aria')
    await page.locator('#password').fill('wrong-password')
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page.getByRole('alert')).toHaveText('Invalid username/email or password.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('logging out returns to the login screen and clears the session', async ({ page }) => {
    await mockLoginSuccess(page)

    await page.goto('/login')
    await page.locator('#identifier').fill(E2E_USER.username)
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page.getByRole('heading', { name: `Welcome, ${E2E_USER.displayName}` })).toBeVisible()

    await page.getByRole('button', { name: 'Log out' }).click()

    await expect(page).toHaveURL(/\/login$/)
    const stored = await page.evaluate(() => localStorage.getItem('tt.auth'))
    expect(stored).toBeNull()
  })
})
