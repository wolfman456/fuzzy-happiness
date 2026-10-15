import { expect, test } from '@playwright/test'
import {
  E2E_MAP,
  mockBattleMap,
  mockCharacter,
  mockLoginSuccess,
  mockMapCatalog,
  mockMe,
  mockSession,
} from './support/api.js'

test.describe('battle map token avatar', () => {
  test('renders a modular character face on a linked player token', async ({ page }) => {
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

    await page.goto('/sessions/1')

    // The player token's linked character (dwarf/FEMALE) renders the modular SVG
    // face inside the token disc, no WebGL or cached snapshot needed
    // (design-draft-v2 §5).
    const tokenFace = page.getByTestId(`token-face-${E2E_MAP.tokens[0].id}`)
    await expect(tokenFace).toBeVisible()
    await expect(tokenFace.locator('svg')).toBeVisible()
    await expect(tokenFace.getByTestId('face-mouth')).toBeVisible()
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

test.describe('battle map blocking objects', () => {
  test('renders object sprites and refuses movement onto a blocked square', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockSession(page)
    await mockBattleMap(page, {
      ...E2E_MAP,
      initiativeIndex: 0,
      objects: [
        { id: 21, objectIndex: 0, x: 2, y: 1, w: 1, h: 1, rotation: 0, blocksMovement: true },
      ],
    })
    await mockMapCatalog(page)

    await page.goto('/login')
    await page.locator('#identifier').fill('aria')
    await page.locator('#password').fill('Password1!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

    await page.goto('/sessions/1')
    await expect(page.locator('[data-testid="object-21"]')).toBeVisible()

    await page.getByRole('button', { name: /Token Aria/ }).click()
    await expect(page.locator('button[aria-label="Move to square 2, 0"]')).toBeVisible()
    await expect(page.locator('button[aria-label="Move to square 2, 1"]')).toHaveCount(0)
  })
})
