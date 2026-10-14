import { RELEASE_NOTES } from './releaseNotes.generated'

/** Public GitHub repository for the project (issue tracking + release tags). */
export const GITHUB_REPO = 'wolfman456/fuzzy-happiness'
export const GITHUB_URL = `https://github.com/${GITHUB_REPO}`
export const ISSUES_URL = `${GITHUB_URL}/issues`
export const NEW_ISSUE_URL = `${GITHUB_URL}/issues/new`

/** Canonical GitHub page (tag) for a released version. */
export function releaseUrl(version) {
  return `${GITHUB_URL}/releases/tag/v${version}`
}

/**
 * Strips the lightweight inline Markdown used in RELEASES.md (code spans,
 * emphasis) so the notes read cleanly as plain text in the UI.
 */
export function plainNotes(notes) {
  return String(notes ?? '')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()
}

export { RELEASE_NOTES }
