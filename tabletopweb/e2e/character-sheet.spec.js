import { expect, test } from '@playwright/test'
import { E2E_CHARACTER, mockCharacter, mockLoginSuccess, mockMe } from './support/api.js'

test.describe('character sheet avatar', () => {
  test('renders the 3D avatar for a character with a base body', async ({ page }) => {
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
    // The shared CC0 base body is committed, so a WebGL canvas mounts for the
    // character's sex (the 2D tint swatch is only the WebGL-unavailable fallback).
    await expect(avatar.locator('canvas')).toHaveCount(1)
  })
})
