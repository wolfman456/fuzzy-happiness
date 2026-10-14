import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ReleaseNotesPage from './ReleaseNotesPage'
import { ISSUES_URL, NEW_ISSUE_URL } from '../lib/releases'

describe('ReleaseNotesPage', () => {
  it('renders the release history from the bundled notes', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByRole('heading', { name: 'Release notes' })).toBeInTheDocument()
    expect(screen.getByTestId('release-1.0.0')).toBeInTheDocument()
    expect(screen.getByTestId('release-1.2.0')).toBeInTheDocument()
  })

  it('links to the GitHub issue tracker', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByTestId('issues-link')).toHaveAttribute('href', ISSUES_URL)
    expect(screen.getByTestId('new-issue-link')).toHaveAttribute('href', NEW_ISSUE_URL)
  })

  it('marks the newest released version as latest and the unreleased one as in development', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByText('Latest')).toBeInTheDocument()
    expect(screen.getByText('In development')).toBeInTheDocument()
  })

  it('links each released version to its GitHub tag', () => {
    render(<ReleaseNotesPage />)

    expect(screen.getByTestId('release-link-1.2.0')).toHaveAttribute(
      'href',
      'https://github.com/wolfman456/fuzzy-happiness/releases/tag/v1.2.0',
    )
    expect(screen.queryByTestId('release-link-1.3.0')).not.toBeInTheDocument()
  })
})
