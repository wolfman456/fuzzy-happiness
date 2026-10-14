import { Card, EmptyState, PageHeading } from '../components/ui'
import {
  ISSUES_URL,
  NEW_ISSUE_URL,
  RELEASE_NOTES,
  plainNotes,
  releaseUrl,
} from '../lib/releases'

export default function ReleaseNotesPage() {
  const latestReleased = RELEASE_NOTES.find((release) => release.date)

  return (
    <div className="space-y-6">
      <Card>
        <PageHeading
          title="Release notes"
          subtitle="Everything that shipped, newest first."
        />
        <p className="mt-3 text-sm text-zinc-600">
          Found a bug or have an idea?{' '}
          <a
            href={ISSUES_URL}
            target="_blank"
            rel="noreferrer"
            className="font-medium text-zinc-900 underline"
            data-testid="issues-link"
          >
            Browse the issue tracker
          </a>{' '}
          or{' '}
          <a
            href={NEW_ISSUE_URL}
            target="_blank"
            rel="noreferrer"
            className="font-medium text-zinc-900 underline"
            data-testid="new-issue-link"
          >
            open a new issue
          </a>
          .
        </p>
      </Card>

      {RELEASE_NOTES.length === 0 ? (
        <EmptyState title="No releases yet">
          Updates will show up here once the first release ships.
        </EmptyState>
      ) : (
        <ol className="space-y-4" data-testid="release-list">
          {RELEASE_NOTES.map((release) => {
            const isLatest = release.version === latestReleased?.version
            return (
              <li key={release.version}>
                <Card>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <h2
                        className="font-display text-lg font-semibold text-zinc-900"
                        data-testid={`release-${release.version}`}
                      >
                        v{release.version}
                      </h2>
                      {isLatest && (
                        <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
                          Latest
                        </span>
                      )}
                      {!release.date && (
                        <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                          In development
                        </span>
                      )}
                    </div>
                    <span className="text-sm text-zinc-500">
                      {release.date ?? 'Unreleased'}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-zinc-700">{plainNotes(release.notes)}</p>
                  {release.date && (
                    <a
                      href={releaseUrl(release.version)}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-3 inline-block text-sm font-medium text-zinc-900 underline"
                      data-testid={`release-link-${release.version}`}
                    >
                      View release on GitHub
                    </a>
                  )}
                </Card>
              </li>
            )
          })}
        </ol>
      )}
    </div>
  )
}
