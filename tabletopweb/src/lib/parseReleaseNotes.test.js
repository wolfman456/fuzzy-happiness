import { describe, expect, it } from 'vitest'
import { parseReleaseNotes, sortReleasesDesc } from './parseReleaseNotes'

const MARKDOWN = `# Releases

## Scheme

- **\`develop\`** carries the next release.

## History

- **v1.0.0 — 2026-01-01 (baseline).** Retroactive tag on the MVP code
  spanning two lines.
- **v1.1.0 — 2026-02-02.** Round-2 fixes: sheets,
  sessions and unified versioning.
- **v1.2.0-SNAPSHOT — (next release).** Current \`develop\`.
`

describe('parseReleaseNotes', () => {
  it('parses the history section into versioned entries', () => {
    const releases = parseReleaseNotes(MARKDOWN)

    expect(releases).toEqual([
      { version: '1.0.0', date: '2026-01-01', notes: 'Retroactive tag on the MVP code spanning two lines.' },
      { version: '1.1.0', date: '2026-02-02', notes: 'Round-2 fixes: sheets, sessions and unified versioning.' },
      { version: '1.2.0-SNAPSHOT', date: null, notes: 'Current `develop`.' },
    ])
  })

  it('returns an empty list when there is no history section', () => {
    expect(parseReleaseNotes('# Releases\n\n## Scheme\n')).toEqual([])
  })

  it('tolerates missing input', () => {
    expect(parseReleaseNotes(undefined)).toEqual([])
  })
})

describe('sortReleasesDesc', () => {
  it('orders releases newest-first by semantic version', () => {
    const releases = parseReleaseNotes(MARKDOWN)
    expect(sortReleasesDesc(releases).map((release) => release.version)).toEqual([
      '1.2.0-SNAPSHOT',
      '1.1.0',
      '1.0.0',
    ])
  })
})
