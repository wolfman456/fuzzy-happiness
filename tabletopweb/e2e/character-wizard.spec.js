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
})
