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
    expect(versions[0]).toBe('1.3.0')
  })

  it('marks unreleased entries with a null date', () => {
    expect(RELEASE_NOTES.find((release) => release.version === '1.3.0').date).toBeNull()
  })
})
