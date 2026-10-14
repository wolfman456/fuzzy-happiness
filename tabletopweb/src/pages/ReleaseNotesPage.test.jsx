import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ReleaseNotesPage from './ReleaseNotesPage'
import { ISSUES_URL, NEW_ISSUE_URL, RELEASE_NOTES, releaseUrl } from '../lib/releases'

describe('ReleaseNotesPage', () => {
  it('renders the release history from the bundled notes', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByRole('heading', { name: 'Release notes' })).toBeInTheDocument()
    expect(screen.getByTestId('release-1.0.0')).toBeInTheDocument()
    expect(screen.getByTestId('release-1.2.0')).toBeInTheDocument()
    expect(screen.getByTestId(`release-${RELEASE_NOTES[0].version}`)).toBeInTheDocument()
  })

  it('links to the GitHub issue tracker', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByTestId('issues-link')).toHaveAttribute('href', ISSUES_URL)
    expect(screen.getByTestId('new-issue-link')).toHaveAttribute('href', NEW_ISSUE_URL)
  })

  it('marks the newest released version as latest and any unreleased one as in development', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getAllByText('Latest')).toHaveLength(1)
    const unreleased = RELEASE_NOTES.filter((release) => release.date === null)
    if (unreleased.length > 0) {
      expect(screen.getByText('In development')).toBeInTheDocument()
    } else {
      expect(screen.queryByText('In development')).not.toBeInTheDocument()
    }
  })

  it('links each released version to its GitHub tag', () => {
    render(<ReleaseNotesPage />)

    for (const release of RELEASE_NOTES) {
      if (release.date) {
        expect(screen.getByTestId(`release-link-${release.version}`)).toHaveAttribute(
          'href',
          releaseUrl(release.version),
        )
      } else {
        expect(screen.queryByTestId(`release-link-${release.version}`)).not.toBeInTheDocument()
      }
    }
  })
})
