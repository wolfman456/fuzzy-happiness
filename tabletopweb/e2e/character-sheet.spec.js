import { expect, test } from '@playwright/test'
import { E2E_CHARACTER, mockCharacter, mockLoginSuccess, mockMe } from './support/api.js'

test.describe('character sheet avatar', () => {
  test('renders the modular face portrait for a character', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockCharacter(page)

    await page.goto('/login')
    await page.locator('#identifier').fill('aria')
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

    await page.goto(`/characters/${E2E_CHARACTER.id}`)
    await expect(page.getByRole('heading', { name: 'Tordek' })).toBeVisible()
    await expect(page.getByText('Female')).toBeVisible()

    // The sheet portrait is a modular client-side SVG face, no WebGL needed
    // (design-draft-v2 §5).
    const avatar = page.getByTestId('character-avatar')
    await expect(avatar).toBeVisible()
    await expect(avatar.locator('svg')).toBeVisible()
    await expect(avatar.getByTestId('face-mouth')).toBeVisible()
  })
})
