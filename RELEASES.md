# Releases

The repo uses one unified version across every artifact (semver), so a release is a single
number everywhere.

## Scheme

- **`develop`** carries the **next** release as `X.Y.Z-SNAPSHOT`.
- **`master`** carries the **live** release as `X.Y.Z`, and each release is git-tagged `vX.Y.Z`.
- **Bump policy:** breaking change → major (`X`), new feature → minor (`Y`), bug fix → patch (`Z`).
- Version files must stay in lockstep: `tabletopserv/pom.xml` (parent, inherited by the three
  modules), `tabletopweb/package.json` + lockfile, `tabletopgateway/package.json` + lockfile,
  repo-root `package.json`.

## Cutting a release

1. Merge the release's PRs into `develop`, then propagate `develop` into `master`
   (regular merge commit; see AGENTS.md "Contribution workflow").
2. On `master`, strip `-SNAPSHOT` from every version file → `X.Y.Z`, commit "Release X.Y.Z".
3. Create an annotated tag `vX.Y.Z` on that commit and push it.
4. On `develop`, bump to `X.(Y+1).0-SNAPSHOT` and commit.
5. Add the release to the history below.

## History

- **v1.0.0 — 2026-09-13 (baseline).** Retroactive tag on the live-site code that shipped the
  MVP. Predates repo versioning, so the manifests on the tagged commit still read
  `0.0.1-SNAPSHOT` / `0.0.0` / `0.1.0`; from here on versions are unified (§74).
- **v1.1.0 — 2026-09-13.** Round-2 alpha fixes: chargen scale-up to 1–20 + all official 2014
  subclasses, sheet/session readability, session battle-map name form + SRD monster search,
  and unified repo versioning.