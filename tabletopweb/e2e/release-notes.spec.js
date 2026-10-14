import { expect, test } from '@playwright/test'
import { mockLoginSuccess, mockMe } from './support/api.js'

async function openReleaseNotes(page) {
  await mockLoginSuccess(page)
  await mockMe(page)

  await page.goto('/login')
  await page.locator('#identifier').fill('aria')
  await page.locator('#password').fill('Password1!')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome, Aria/ })).toBeVisible()

  await page.goto('/releases')
}

test.describe('release notes', () => {
  test('lists the shipped releases and links to the GitHub issue tracker', async ({ page }) => {
    await openReleaseNotes(page)

    await expect(page.getByRole('heading', { name: 'Release notes' })).toBeVisible()
    await expect(page.getByTestId('release-list')).toBeVisible()
    await expect(page.getByTestId('release-1.2.0')).toBeVisible()

    const issues = page.getByTestId('issues-link')
    await expect(issues).toHaveAttribute(
      'href',
      'https://github.com/wolfman456/fuzzy-happiness/issues',
    )

    const releaseLink = page.getByTestId('release-link-1.2.0')
    await expect(releaseLink).toHaveAttribute(
      'href',
      'https://github.com/wolfman456/fuzzy-happiness/releases/tag/v1.2.0',
    )
  })

  test('is reachable from the navigation drawer', async ({ page }) => {
    await openReleaseNotes(page)

    await page.getByRole('button', { name: 'Open menu' }).click()
    await page.getByRole('link', { name: 'Release notes' }).click()

    await expect(page.getByRole('heading', { name: 'Release notes' })).toBeVisible()
  })
})
