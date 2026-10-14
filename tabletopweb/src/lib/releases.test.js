import { describe, expect, it } from 'vitest'
import {
  GITHUB_URL,
  ISSUES_URL,
  NEW_ISSUE_URL,
  RELEASE_NOTES,
  plainNotes,
  releaseUrl,
} from './releases'

describe('release links', () => {
  it('points at the project’s GitHub repo', () => {
    expect(GITHUB_URL).toBe('https://github.com/wolfman456/fuzzy-happiness')
    expect(ISSUES_URL).toBe(`${GITHUB_URL}/issues`)
    expect(NEW_ISSUE_URL).toBe(`${GITHUB_URL}/issues/new`)
  })

  it('builds a tag URL per version', () => {
    expect(releaseUrl('1.2.0')).toBe(`${GITHUB_URL}/releases/tag/v1.2.0`)
  })
})

describe('plainNotes', () => {
  it('strips inline code and emphasis', () => {
    expect(plainNotes('Current `develop` (**1.3.0-SNAPSHOT**).')).toBe(
      'Current develop (1.3.0-SNAPSHOT).',
    )
  })

  it('collapses whitespace and tolerates null', () => {
    expect(plainNotes('a  b\n c')).toBe('a b c')
    expect(plainNotes(null)).toBe('')
  })
})

describe('RELEASE_NOTES (generated from RELEASES.md)', () => {
  it('carries the release history, newest first', () => {
    expect(RELEASE_NOTES.length).toBeGreaterThan(0)
    const versions = RELEASE_NOTES.map((release) => release.version)
    expect(versions).toContain('1.0.0')
    // newest first
    const rank = (version) => version.split('.').map(Number)
    const isNewerOrEqual = (a, b) =>
      a[0] !== b[0] ? a[0] > b[0] : a[1] !== b[1] ? a[1] > b[1] : a[2] >= b[2]
    for (let i = 0; i < versions.length - 1; i++) {
      expect(isNewerOrEqual(rank(versions[i]), rank(versions[i + 1]))).toBe(true)
    }
  })

  it('marks unreleased entries with a null date', () => {
    const unreleased = RELEASE_NOTES.filter((release) => release.date === null)
    expect(unreleased.length).toBeLessThanOrEqual(1)
    if (unreleased.length > 0) {
      expect(unreleased[0]).toBe(RELEASE_NOTES[0])
    }
  })
})
