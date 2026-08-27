# Session

Pick up where we left off: opencode session resumes via `/sessions`; if starting fresh, read this file plus `draft-design.md` (§8 Character Generation, §9 SRD Integration are the newest sections).

## Where we are

- Design phase, no app code yet. Active branch: **`feature/design`** (squashed, one commit: "Add platform design doc and D&D 5e character generation model"). PR open against `develop`.
- `develop` already contains the original design draft (PR #6 merged), so the PR now shows only the incremental SRD + character-generation design in the diff. **Do not merge the PR yourself** — maintainer reviews (per AGENTS.md).

## Decisions locked in

- D&D 5e-first MVP; generic `Character` core so future games plug in.
- Discord = voice companion (OAuth + deep link to installed client), not built-in VoIP.
- Rules data: open 5e-bits SRD API (`dnd5eapi.co`) consumed via backend proxy (WebClient) + Caffeine cache; no raw URL forwarding; cached fallback on outage.
- Character generation: guided wizard + random quick-build. Ability scores = house-rule 6xd20 rolled server-side, returned unassigned; player assigns. Starting level configurable 1-3. Client-draft + pure server compile (validate vs SRD allow-lists, derive modifiers/HP/AC/saves/spell slots), finalize persists `Dnd5eCharacter`.
- DB: H2 dev / PostgreSQL prod via Spring profiles. Root `.gitignore` ignores `.idea/`.

## Next steps

- Review design; when approved, proceed to Stage 1 implementation (auth, SRD proxy, REST).
- Deferrals recorded in `draft-design.md` §14 (standard array/point-buy/4d6, house-rule d20 config, leveling >3, SRD mirror/multilingual/2024).

## Gotchas

- No `gh` CLI / GITHUB_TOKEN on this machine — PRs must be opened/merged manually via the GitHub web UI; we can only print the compare URL.
- Editor freezes in non-interactive shells: set `GIT_EDITOR=true` for rebase/commit steps.
- Backend = Spring Boot 4.1.1, Java 21, JAR packaging; frontend = Vite 8 + React 19 (plain JSX), oxlint + Vitest. See AGENTS.md.