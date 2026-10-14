const DATE_PATTERN = /(\d{4}-\d{2}-\d{2})/

/**
 * Parses the `## History` section of `RELEASES.md` into structured releases.
 *
 * Each history entry looks like `- **v1.2.0 — 2026-09-13.** Round-3 fixes: …`
 * with any wrapped continuation lines indented beneath it. A missing date
 * (e.g. `(next release)`) yields `date: null`, marking the entry unreleased.
 *
 * Pure and dependency-free so it can be reused by the generator script
 * (`scripts/generate-release-notes.mjs`) and unit-tested under Vitest.
 */
export function parseReleaseNotes(markdown) {
  const lines = String(markdown ?? '').split(/\r?\n/)
  const start = lines.findIndex((line) => /^##\s+History\s*$/.test(line))
  if (start === -1) return []

  const section = []
  for (let i = start + 1; i < lines.length; i += 1) {
    if (/^##\s+/.test(lines[i])) break
    section.push(lines[i])
  }

  const entries = []
  let current = null
  for (const line of section) {
    const match = /^-\s+\*\*(v[\w.-]+)\s+—\s+(.*?)\.\*\*\s*(.*)$/.exec(line)
    if (match) {
      if (current) entries.push(current)
      const date = DATE_PATTERN.exec(match[2])?.[1] ?? null
      current = { version: match[1].replace(/^v/, ''), date, notes: match[3].trim() }
    } else if (current && line.trim()) {
      current.notes = `${current.notes} ${line.trim()}`.trim()
    }
  }
  if (current) entries.push(current)

  return entries.map((entry) => ({ ...entry, notes: entry.notes.replace(/\s+/g, ' ').trim() }))
}

/** Orders releases newest-first by semantic version. */
export function sortReleasesDesc(releases) {
  return [...releases].sort((a, b) => compareVersionsDesc(a.version, b.version))
}

function versionParts(version) {
  return String(version)
    .split('.')
    .map((part) => Number.parseInt(part, 10) || 0)
}

function compareVersionsDesc(a, b) {
  const left = versionParts(a)
  const right = versionParts(b)
  const length = Math.max(left.length, right.length)
  for (let i = 0; i < length; i += 1) {
    const diff = (right[i] ?? 0) - (left[i] ?? 0)
    if (diff !== 0) return diff
  }
  return 0
}
