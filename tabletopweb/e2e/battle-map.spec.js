import { expect, test } from '@playwright/test'
import {
  E2E_CHARACTER,
  E2E_MAP,
  mockBattleMap,
  mockCharacter,
  mockLoginSuccess,
  mockMapCatalog,
  mockMe,
  mockSession,
} from './support/api.js'

test.describe('battle map token avatar', () => {
  test('renders the cached character portrait on a linked player token', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockCharacter(page)
    await mockSession(page)
    await mockBattleMap(page)
    await mockMapCatalog(page)

    await page.goto('/login')
    await page.locator('#identifier').fill('aria')
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

    // Open the character sheet so its portrait snapshot is captured into the
    // in-memory cache (design-draft-v2 §5, item 4).
    await page.goto(`/characters/${E2E_CHARACTER.id}`)
    await expect(page.locator('img[data-testid="character-avatar"]')).toBeVisible({ timeout: 20000 })

    // Client-side navigate to the session so the snapshot cache survives (a full
    // reload would clear the module-scoped Map).
    await page.evaluate(() => {
      window.history.pushState({}, '', '/sessions/1')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    // The player token's linked character (dwarf/FEMALE/120/300) matches the
    // captured snapshot key, so the token renders the cached PNG instead of the
    // colour disc with initials (design-draft-v2 §5, item 5).
    const tokenAvatar = page.locator(`img[data-testid="token-avatar-${E2E_MAP.tokens[0].id}"]`)
    await expect(tokenAvatar).toBeVisible()
    await expect(tokenAvatar).toHaveAttribute('src', /^data:image\/png/)
  })
})

test.describe('battle map terrain tiles', () => {
  test('paints catalog-coloured tiles on the grid', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockSession(page)
    await mockBattleMap(page, {
      ...E2E_MAP,
      tiles: [
        { x: 2, y: 3, tileIndex: 0 },
        { x: 4, y: 1, tileIndex: 7 },
      ],
    })
    await mockMapCatalog(page)

    await page.goto('/login')
    await page.locator('#identifier').fill('aria')
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

    await page.goto('/sessions/1')
    await expect(page.locator('[data-testid="tile-2-3"]')).toBeVisible()
    await expect(page.locator('[data-testid="tile-4-1"]')).toBeVisible()
  })
})
