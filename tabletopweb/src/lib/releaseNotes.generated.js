// AUTO-GENERATED from RELEASES.md by scripts/generate-release-notes.mjs — do not edit by hand.
// Regenerate with `npm run gen:release-notes` after editing RELEASES.md.

export const RELEASE_NOTES = [
  {
    "version": "1.3.0",
    "date": "2026-10-14",
    "notes": "v2 delivery: Flyway migrations hand the prod schema to `V1__baseline` (Slice 0) and CI runs on every pull request with a new Playwright E2E layer; the wizard now labels every choice (R48, Slice 1), heroes gain sex + cosmetic appearance (R49, Slice 2a) and a 3D avatar preview with a 2D-swatch fallback (R50, Slice 2b); plus an in-app release-notes page (R52, §10) and config-gated social OAuth login — Google/Facebook/GitHub (R19, §11); chargen hardening (gateway-call bounding #49, merged subclass features #85, ability-score reroll cap #87) and a character-wizard helper-text contrast fix."
  },
  {
    "version": "1.2.0",
    "date": "2026-09-13",
    "notes": "Round-3 alpha fixes: one-shot full reroll + class-skill cap on chargen, delete-your-own-character end to end, and setup-phase token placement on the battle map (free placement until combat starts)."
  },
  {
    "version": "1.1.0",
    "date": "2026-09-13",
    "notes": "Round-2 alpha fixes: chargen scale-up to 1–20 + all official 2014 subclasses, sheet/session readability, session battle-map name form + SRD monster search, and unified repo versioning."
  },
  {
    "version": "1.0.0",
    "date": "2026-09-13",
    "notes": "Retroactive tag on the live-site code that shipped the MVP. Predates repo versioning, so the manifests on the tagged commit still read `0.0.1-SNAPSHOT` / `0.0.0` / `0.1.0`; from here on versions are unified (§74)."
  }
]
