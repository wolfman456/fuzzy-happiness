# Releases

The repo uses one unified version across every artifact (semver), so a release is a single
number everywhere.

## Scheme

- **`develop`** carries the **next** release as `X.Y.Z-SNAPSHOT`.
- **`master`** carries the **live** release as `X.Y.Z`, and each release is git-tagged `vX.Y.Z`.
- **Bump policy:** breaking change → major (`X`), new feature → minor (`Y`), bug fix → patch (`Z`).
- Version files must stay in lockstep: `tabletopserv/pom.xml` (parent aggregator) **and the three
  module poms** (`tabletopapi`, `tabletopservice`, `tabletopfunctionaltest`, each of which pins the
  parent `<version>`), `tabletopweb/package.json` + lockfile, `tabletopgateway/package.json` +
  lockfile, repo-root `package.json`.

## Cutting a release

1. Merge the release's PRs into `develop`, then propagate `develop` into `master`
   (regular merge commit; see AGENTS.md "Contribution workflow").
2. On `master`, strip `-SNAPSHOT` from every version file → `X.Y.Z`, commit "Release X.Y.Z".
3. Create an annotated tag `vX.Y.Z` on that commit and push it.
4. On `develop`, bump to `X.(Y+1).0-SNAPSHOT` and commit.
5. Add the release to the history below.
6. Regenerate the in-app release-notes data and commit it in the same change:
   `cd tabletopweb && npm run gen:release-notes`. The page at `/releases`
   (design-draft-v2.md §10) is built from the history above, so it must not drift.
7. Publish a GitHub Release for the tag (`gh release create vX.Y.Z --notes …`) so the page's
   per-release links resolve to a real release.

## History

- **v1.0.0 — 2026-09-13 (baseline).** Retroactive tag on the live-site code that shipped the
  MVP. Predates repo versioning, so the manifests on the tagged commit still read
  `0.0.1-SNAPSHOT` / `0.0.0` / `0.1.0`; from here on versions are unified (§74).
- **v1.1.0 — 2026-09-13.** Round-2 alpha fixes: chargen scale-up to 1–20 + all official 2014
  subclasses, sheet/session readability, session battle-map name form + SRD monster search,
  and unified repo versioning.
- **v1.2.0 — 2026-09-13.** Round-3 alpha fixes: one-shot full reroll + class-skill cap on
  chargen, delete-your-own-character end to end, and setup-phase token placement on the battle
  map (free placement until combat starts).
- **v1.3.0 — 2026-10-14.** v2 delivery: Flyway migrations hand the prod schema to `V1__baseline`
  (Slice 0) and CI runs on every pull request with a new Playwright E2E layer; the wizard now
  labels every choice (R48, Slice 1), heroes gain sex + cosmetic appearance (R49, Slice 2a) and a
  3D avatar preview with a 2D-swatch fallback (R50, Slice 2b); plus an in-app release-notes page
  (R52, §10) and config-gated social OAuth login — Google/Facebook/GitHub (R19, §11); chargen
  hardening (gateway-call bounding #49, merged subclass features #85, ability-score reroll cap
  #87) and a character-wizard helper-text contrast fix.
- **v1.4.0 — 2026-10-15.** The modular avatar (R51, v2 Slice 2c): one shared, scripted CC0 base
  body per sex with named sockets (data chain #112, base assets/compositor #113), race-feature
  parts — dragonborn/tiefling horns, half-orc tusks, dragonborn/tiefling tails (#115) — and
  player-chosen hair parts (short/ponytail/long) with a wizard hair-style picker (#118); the
  character's chosen appearance now renders as a client-side cached portrait snapshot (#116) and on
  the battle-map token of the linked player (#117). Plus readable dark-backdrop headings + button/
  card motion (#109, #110) and a lockfile-ignore fix so app lockfiles stay uploadable (#111).
- **v1.5.0 — (next release).** Current `develop` (`1.5.0-SNAPSHOT`).