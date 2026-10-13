import { expect, test } from '@playwright/test'
import { mockRegisterSuccess } from './support/api.js'

async function fillRegistration(page, { password = 'Password1!', confirmPassword = 'Password1!' } = {}) {
  await page.goto('/register')
  await page.locator('#displayName').fill('Aria')
  await page.locator('#realName').fill('Aria Rivers')
  await page.locator('#email').fill('aria@example.com')
  await page.locator('#dateOfBirth').fill('1990-01-15')
  await page.locator('#username').fill('aria')
  await page.locator('#password').fill(password)
  await page.locator('#confirmPassword').fill(confirmPassword)
}

test.describe('registration', () => {
  test('a valid registration shows the confirmation screen', async ({ page }) => {
    await mockRegisterSuccess(page)
    await fillRegistration(page)

    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByRole('heading', { name: 'Account created' })).toBeVisible()
    await expect(page.getByText(/Welcome aboard/)).toBeVisible()
  })

  test('the client-side password policy blocks submission before any request', async ({ page }) => {
    let registerCalled = false
    await page.route('**/api/auth/register', (route) => {
      registerCalled = true
      return route.fulfill({ status: 201, contentType: 'application/json', body: '{}' })
    })
    await fillRegistration(page, { password: 'weak', confirmPassword: 'weak' })

    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(
      page.getByText('Password needs 8+ characters with an upper, lower, digit and symbol.'),
    ).toBeVisible()
    await expect(page).toHaveURL(/\/register$/)
    expect(registerCalled).toBe(false)
  })
})
