import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseReleaseNotes, sortReleasesDesc } from '../src/lib/parseReleaseNotes.js'

const HERE = dirname(fileURLToPath(import.meta.url))
const SOURCE = resolve(HERE, '../../RELEASES.md')
const TARGET = resolve(HERE, '../src/lib/releaseNotes.generated.js')

const BANNER =
  '// AUTO-GENERATED from RELEASES.md by scripts/generate-release-notes.mjs — do not edit by hand.\n' +
  '// Regenerate with `npm run gen:release-notes` after editing RELEASES.md.\n'

function render(releases) {
  return `${BANNER}\nexport const RELEASE_NOTES = ${JSON.stringify(releases, null, 2)}\n`
}

function main() {
  const check = process.argv.includes('--check')

  let markdown
  try {
    markdown = readFileSync(SOURCE, 'utf8')
  } catch {
    // The web image builds from its own directory (Railway rootDirectory), so
    // RELEASES.md is absent there; keep the committed generated file as-is.
    console.log(`Skipping: ${SOURCE} not found (using committed releaseNotes.generated.js).`)
    return
  }

  const output = render(sortReleasesDesc(parseReleaseNotes(markdown)))

  if (check) {
    const current = readFileSync(TARGET, 'utf8')
    if (current !== output) {
      console.error('releaseNotes.generated.js is out of date — run `npm run gen:release-notes`.')
      process.exit(1)
    }
    console.log('releaseNotes.generated.js is up to date.')
    return
  }

  writeFileSync(TARGET, output)
  console.log(`Wrote ${TARGET}`)
}

main()
