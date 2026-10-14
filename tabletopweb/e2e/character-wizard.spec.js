import { expect, test } from '@playwright/test'
import { mockChargenCatalog, mockLoginSuccess, mockMe, mockSrd } from './support/api.js'

async function openWizard(page) {
  await mockLoginSuccess(page)
  await mockMe(page)
  await mockSrd(page)
  await mockChargenCatalog(page)

  await page.goto('/login')
  await page.locator('#identifier').fill('aria')
  await page.locator('#password').fill('Password1!')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

  await page.goto('/characters/new')
  await expect(page.getByRole('heading', { name: 'Name & basics' })).toBeVisible()
}

test.describe('character wizard labeling & race facts', () => {
  test('explains each step and describes the chosen race', async ({ page }) => {
    await openWizard(page)

    await expect(page.getByText(/Give your hero a name/)).toBeVisible()

    await page.getByLabel('Character name').fill('Tordek')
    await page.getByLabel('Ability scores').selectOption('STANDARD_ARRAY')
    await page.getByRole('button', { name: 'Next' }).click()

    await expect(page.getByRole('heading', { name: 'Ability scores' })).toBeVisible()
    await page.getByRole('button', { name: 'Use the standard array (as written)' }).click()
    await page.getByRole('button', { name: 'Next' }).click()

    await expect(page.getByRole('heading', { name: 'Race' })).toBeVisible()
    await page.getByRole('button', { name: 'Dwarf' }).click()

    const facts = page.getByTestId('race-facts')
    await expect(facts).toContainText('Medium')
    await expect(facts).toContainText('Common, Dwarvish')
  })

  test('gates the Appearance step on sex and lets the player tune tints', async ({ page }) => {
    await openWizard(page)

    await page.getByLabel('Character name').fill('Tordek')
    await page.getByLabel('Ability scores').selectOption('STANDARD_ARRAY')
    await page.getByRole('button', { name: 'Next' }).click()
    await page.getByRole('button', { name: 'Use the standard array (as written)' }).click()
    await page.getByRole('button', { name: 'Next' }).click()
    await page.getByRole('button', { name: 'Dwarf' }).click()
    await page.getByRole('button', { name: 'Next' }).click()

    await expect(page.getByRole('heading', { name: 'Appearance' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Next' })).toBeDisabled()

    await page.getByRole('radio', { name: 'Female' }).check()
    await expect(page.getByRole('button', { name: 'Next' })).toBeEnabled()

    await expect(page.getByText(/Skin tint \(200°\)/)).toBeVisible()
    await page.getByLabel(/Skin tint/).fill('40')
    await expect(page.getByText(/Skin tint \(40°\)/)).toBeVisible()

    const swatch = page.getByTestId('appearance-swatch')
    await expect(swatch).toBeVisible()
    await expect(swatch).toHaveCSS('background-color', 'rgb(199, 168, 107)')
    await expect(page.getByTestId('appearance-hair-swatch')).toBeVisible()
    // No curated GLB is registered yet, so no WebGL canvas is mounted.
    await expect(page.locator('canvas')).toHaveCount(0)
  })
})
