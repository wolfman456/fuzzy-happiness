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
    // The sheet portrait is a client-side PNG snapshot captured from the avatar
    // scene (design-draft-v2 §5): the live WebGL canvas mounts on the first
    // render, then is swapped for the cached <img>.
    await expect(page.locator('img[data-testid="character-avatar"]')).toBeVisible({ timeout: 20000 })
  })
})
