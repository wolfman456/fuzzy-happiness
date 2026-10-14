import { expect, test } from '@playwright/test'
import { E2E_CHARACTER, mockCharacter, mockLoginSuccess, mockMe } from './support/api.js'

test.describe('character sheet avatar', () => {
  test('shows the tinted fallback avatar until a 3D model is curated', async ({ page }) => {
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

    const avatar = page.getByTestId('character-avatar')
    await expect(avatar).toBeVisible()
    await expect(avatar).toHaveCSS('background-color', 'rgb(107, 199, 107)')
    // No curated GLB is registered yet, so the lightweight 2D swatch renders and
    // no WebGL canvas is mounted.
    await expect(page.locator('canvas')).toHaveCount(0)
  })
})
