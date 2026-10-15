import { expect, test } from '@playwright/test'
import {
  E2E_MAP,
  E2E_SESSION,
  E2E_USER,
  jsonResponse,
  mockLoginSuccess,
  mockMapCatalog,
  mockMe,
  mockSession,
} from './support/api.js'

async function login(page) {
  await page.goto('/login')
  await page.locator('#identifier').fill('aria')
  await page.locator('#password').fill('Password1!')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()
}

test.describe('map editor', () => {
  test('the GM paints tiles and places objects on the full-screen editor', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockSession(page, {
      ...E2E_SESSION,
      participants: [{ user: E2E_USER, role: 'GM' }],
    })
    await mockMapCatalog(page)

    let map = { ...E2E_MAP }
    await page.route('**/api/sessions/1/map', (route) => route.fulfill(jsonResponse(200, map)))
    await page.route('**/api/sessions/1/map/tiles', (route) => {
      map = { ...map, tiles: route.request().postDataJSON().tiles }
      return route.fulfill(jsonResponse(200, map))
    })
    await page.route('**/api/sessions/1/map/objects', (route) => {
      const body = route.request().postDataJSON()
      map = {
        ...map,
        objects: [
          ...(map.objects ?? []),
          {
            id: 21,
            objectIndex: body.objectIndex,
            x: body.x,
            y: body.y,
            w: 1,
            h: 1,
            rotation: 0,
            blocksMovement: true,
          },
        ],
      }
      return route.fulfill(jsonResponse(200, map))
    })

    await login(page)
    await page.goto('/sessions/1/map/edit')
    await expect(page.getByRole('heading', { name: 'Map editor' })).toBeVisible()

    await page.getByTestId('tile-swatch-0').click()
    await page.getByTestId('cell-2-3').click()
    await expect(page.getByTestId('editor-tile-2-3')).toBeVisible()

    await page.getByTestId('object-swatch-0').click()
    await page.getByTestId('cell-5-5').click()
    await expect(page.getByTestId('editor-object-21')).toBeVisible()
  })

  test('players cannot open the editor', async ({ page }) => {
    await mockLoginSuccess(page)
    await mockMe(page)
    await mockSession(page)
    await mockMapCatalog(page)
    await page.route('**/api/sessions/1/map', (route) => route.fulfill(jsonResponse(200, E2E_MAP)))

    await login(page)
    await page.goto('/sessions/1/map/edit')
    await expect(page.getByText('Only the game master can edit this map.')).toBeVisible()
  })
})
