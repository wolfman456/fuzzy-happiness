# Fuzzy Happiness — Design v2 (Draft)

Draft spec for the second major release of the tabletop platform. Companion to the **locked**
[`design-v1.md`](design-v1.md) (MVP, shipped 2026-09-12). Nothing here is binding until the
maintainer approves the slices; the living bug ledger stays in
[`design-v1-fixes.md`](design-v1-fixes.md).

**Status:** 🏗 in planning — v2 vision + slice plan agreed (2026-09-13); Slice 0 Flyway baseline
landed (2026-10-13).

---

## 1. v2 Vision

Move the app from "a legal-sheet + map session tool" to a **presentable, gameable table**:

1. **Appearance that follows the character.** Character creation produces a 3D avatar drawn
   from the chosen **race × sex** (binary `MALE`/`FEMALE` only), with cosmetic skin/hair tints.
   That avatar shows up in the wizard, on the character sheet, and as the player's token in the
   battle map.
2. **Map creation as a first-class tool.** GMs edit a real map — not just a token grid — by
   painting **tiles** and placing **environment items** (walls, props, terrain) to build the
   encounter space.
3. **A site that looks and feels finished.** Consistent theme, clearer navigation, and a
   character wizard that labels and explains each choice instead of relying on terse one-liners.

**Beyond v2 slices (alpha-pass ideas recorded 2026-09-14, deferred):**
- **R38 VOIP integration** (in-session voice), **R39 sound effects**, **R40 music** — added as
  deferred Wants items; no slice scheduled. Future research: VOIP options (SFU vs mesh
  WebRTC, libs, cost) when a slice starts.

## 2. Decisions locked in (2026-09-13 planning)

- **Schema:** enable **Flyway + migrations** on prod now (design-v1 §13 Stage 5 pulled forward).
  v2 data needs new tables/columns that `ddl-auto=validate` will reject; a baseline + `V2`
  migration is the forward path.
- **Avatar build:** **curated low-poly GLBs per race×sex** (9 races × 2 sexes = 18 models,
  free CC0 packs) plus **cosmetic tint sliders** (skin/hair). No parametric body builder in v2.
- **Avatar usage:** wizard preview **+** character-sheet portrait **+** in-session 3D token
  (re-opens deferred **R12**, design-v1 §17 blueprint — R3F + drei, WebGL2, OrbitControls,
  server-authoritative state).
- **Sequencing:** Slice 1 (labeling/UX) → Slice 2 (sex + 3D avatar) → Slice 3 (map builder).
- **Slice 3 movement (decided 2026-09-14):** a **mix of blocking and decorative** objects. Fully
  blocking (walls/rock faces/mounts) are hard legal gates — server rejects moves/pathing into
  them; partial (rivers, rubble by depth/speed) read as decorative/impeding — the GM picks per
  object. Applies to blackout squares too (impassable = hard block, path-blocking BFS).
- **Slice 3 editor surface (decided 2026-09-14):** a **dedicated full-screen map-editor route**,
  not edit-in-panel. Maps save to an **open community map library**; private maps are a later
  iteration.
- **Chargen bugs (decided 2026-09-14):** Bug A and the **residual Bug B** were filed as **v2.0
  hot-fixes before any slice** (Bug A shipped in v1.1 as #58 and is verified fixed; Bug B's
  curated-only subclass gap shipped 2026-09-14 as #90/#85 — the remaining unreleased piece is
  `feature_choices`).
- **Player 3D tokens scope (decided 2026-09-14):** **all tokens, incl. monster minis**, get
  meshes in v2, viewed third-person (design-v1 §17).

### design-v1 §14 open questions — resolved 2026-10-13

The 11 open questions carried in the **locked** [`design-v1.md`](design-v1.md) §14 were triaged
with the maintainer. All are now decided; each decision that needs code becomes its own
`feature/v2-*` slice (see the backlog note below).

| `design-v1.md` §14 question | Decision |
|---|---|
| Friends list / permanent groups vs invite codes | **Both** — permanent friends/groups **and** per-session invite codes |
| Exact D&D 5e sheet fields for v1 | **Use the most current D&D sheet — the 2024 ruleset/sheet** (migration options in [`research/2024-rules-options.md`](research/2024-rules-options.md)) |
| Room persistence (archived vs ephemeral) | **Creator decides per room** — persistent/archived vs ephemeral |
| SRD: live proxy vs mirror into our DB | **Mirror** the SRD into our own database (both rulesets if dual-run) |
| SRD version pinning / when to adopt 2024 | **Undecided → options to be presented**; target 2024 (see the options doc) |
| SRD multilingual (`?lang=`) | **English only for now** |
| Expanded classes (R24) data source | **User-facing homebrew class editor** — a friendly form producing the same JSON, with server-side validation before persistence |
| House-rule d20 (always-on vs per-room) | **Per-room on/off toggle, set by the room creator** |
| Leveling existing characters | **Yes** — milestone **or** XP, chosen by the room creator |
| Monster-generation LLM provider | **No LLM** — monsters come from a **random table or the GM's discretion**, *supplementing* (not replacing) the deterministic CR engine |
| 3D glTF asset source/style | **Deferred to v2** (matches existing R12 deferral) |

**Backlog slices spawned by these decisions** — each its own `feature/v2-*` PR, gated on
Slice 0 (Flyway) wherever schema is touched:

- 2024 ruleset + sheet migration (see options doc for the recommended shape).
- Friends list / permanent groups (on top of the existing invite-code sessions).
- SRD mirror into our own database.
- Homebrew class editor (JSON form + server-side validation) — satisfies R24.
- Room toggles: house-rule d20, leveling mode, persistence — all set by the creator.
- Milestone/XP leveling for existing characters.
- Monster random-table + GM-discretion entry (supplementing the CR engine).

## 3. Slice 0 — Foundation: Flyway + migrations

Groundwork every other slice leans on.

- ✅ **Flyway baseline landed** (`feature/v2-flyway`, PR B1, 2026-10-13): Flyway 12 on the
  **prod profile only** (`application-prod.properties`), with `V1__baseline.sql` captured
  verbatim from the Hibernate 7.4.5.Final PostgreSQL DDL and `baseline-on-migrate` so the live
  Railway Postgres adopts Flyway at V1 without re-running the baseline. Dev/H2 keeps the
  Hibernate-managed schema (the baseline is PostgreSQL-specific). Validated by
  `FlywayMigrationIT`, which boots the packaged service against an empty Postgres with
  `ddl-auto=validate` — a baseline/entity mismatch fails the boot, so CI proves it on every run.
  CI uses a Testcontainers Postgres; without Docker the IT falls back to a rootless embedded
  Postgres binary (`io.zonky.test:embedded-postgres`), so the baseline is validated locally too.
  Wiring uses `spring-boot-starter-flyway` (Boot 4's modular Flyway auto-configuration — the raw
  `flyway-core` dependency alone does not activate it). No Java config classes were added
  (properties only), so the jacoco gate is untouched.
- ⏳ **Still to come** — v2 schema changes ship as forward migrations (never edits to V1), e.g.
  `V2__v2_appearance_maps.sql`:
  - `character` / `dnd5e_characters`: `sex` (`varchar(10)`, `MALE|FEMALE`) and `appearance`
    (`jsonb`: avatar GLB ref + skin/hair tints).
  - New tables `battle_map_tiles` (`map_id`, `x`, `y`, `tile_index`, PK `(map_id,x,y)`) and
    `battle_map_objects` (`map_id`, `id`, `object_index`, `x`, `y`, `w`, `h`, `rotation`,
    `blocks_movement`).
- **Risk (deliberate):** prod DB is live. The baseline is proven safe against a fresh Postgres
  in CI; the first `railway` deploy that enables Flyway will create only the `flyway_schema_history`
  table and record the baseline (no app-table DDL), so a drifted prod schema cannot be clobbered.
- Deliverables: `design-draft-v2.md` (this file, once approved), Wants.md v2 entries, board
  items.

## 4. Slice 1 — Wizard labeling + site appearance / ease of use

Pure-frontend polish; cheapest slice, unblocks the rest.

> **Status:** ✅ landed (`feature/v2-slice-1-wizard-ux`, R48, PR #103, 2026-10-13). Ships a Tailwind v4 `@theme`
> design language (parchment/ink/brass palette + display serif, no runtime font fetch), shared UI
> primitives (`PageHeading`, `Card`, `Alert`, `LoadingState`, `EmptyState`), a per-step `<h2>` +
> guidance paragraph on every wizard step, and a **Race step facts block** rendering the already-fetched
> SRD `size` / `size_description` / `age` / `alignment` / `languages`. Covered by Vitest + a new
> Playwright `character-wizard.spec.js`. The v1 doc's step-order drift is reconciled in favour of the
> shipped code order (Skills → Spells → Equipment → Review), recorded in `design-v1-fixes.md`.

- **Wizard** (`tabletopweb/src/pages/CharacterWizardPage.jsx`):
  - Per-step `<h2>` heading + a guidance paragraph on every step; existing copy is expanded,
    kept verbatim where good.
  - Richer **Race** step using SRD fields already fetched but unused: `size`, `size_description`,
    `age`, `alignment`, `languages`.
  - Reconcile the v1 doc step-order drift (design-v1 §8 lists equipment before spells; the code
    is Skills → Spells → Equipment → Review).
  - Consistent empty/loading/error states and clearer gating copy per step.
- **Shell/theme:**
  - `tabletopweb/src/index.css` (currently one `@import` line) gains a `@theme` design language
    (fantasy palette + font) matching the dragon backdrop.
  - Polish `ThemedBackdrop` / `AuthBackground` / `ShellLayout` drawer.
  - Pass of consistent focus/empty/loading/error states across all pages (Session, Characters,
    Settings, Dashboard).
- **Risk:** low. Styling is centralized in 3 touchpoints (per v2 research); all components use
  Tailwind utilities.

## 5. Slice 2 — Sex field + 3D avatar step

"An avatar that matches race and sex." Sex is **required, binary `MALE`/`FEMALE` only, cosmetic —
no mechanical effect** (5e does not gate chargen choices by sex).

- **Data chain** (rides Slice 0 columns): wizard draft → `CharacterDraftDto` → `CharacterDraft` /
  `Dnd5eCharacter` → `CharacterSheetDto` → `sheetToDraft`. Server validates `sex ∈ {MALE,FEMALE}`
  and compiles it into every sheet.
- **Wizard:** new **Appearance** step after Race (avatar is race×sex). Sex radio, live 3D preview
  (`Canvas`, R3F) of the race-base mesh, skin/hair tint sliders. Changing race after this step
  re-validates the model.
- **3D stack:** add `three`, `@react-three/fiber`, `@react-three/drei`. Models:
  `tabletopweb/public/models/avatars/{race}-{M|F}.glb`, curated CC0 (asset-licensing research
  first, mirroring `research/maps resources.md`). WebGL2 now; WebGPU later (§17).
  jsdom tests **mock the Canvas** and test selection/tint logic, not pixels.
- **Consumption:**
  - Portrait render on `CharacterSheetPage`.
  - In-session: a linked player's token renders their avatar GLB instead of the color disc
    (`MapToken.appearanceRef`), inside a 3D viewport sharing the same server-authoritative
    `BattleMapDto` state. Re-opens deferred R12 (design-v1 §17).

## 6. Slice 3 — Map builder: tiles + environment items

The **major v2 focus** ("map creation"). A GM builds the encounter space, not just tokens.

- **Server:**
  - `MapCatalog` component (tile palette + object defs: `{index, name}` + dims +
    `blocksMovement` flag; no rulebook text — `ChargenCatalog` precedent).
  - New endpoints: `PUT /api/sessions/{id}/map/tiles` (bulk grid paint),
    `POST|PATCH|DELETE …/map/objects[/{id}]`. GM-only via the existing `requireGm` pattern.
  - Mutations keep the full-map STOMP `TABLE` replace-all; fine at 24×18 (≈432 cells).
    Payload deltas are a noted follow-up, not v2 blocker.
- **Client** (`BattleMapPanel.jsx`):
  - Bottom tile layer (per-cell divs / sprite images) + object sprites above tokens, below
    reachable-move overlay.
  - **Map editor mode** (GM): palette sidebar, tile paint / erase / eyedropper, object
    place / move / resize / rotate. Play view unchanged.
- **Behavior default (decided 2026-09-14):** tiles are visual; `blocksMovement` objects are
  hard legal gates excluded by server move-validation **and** path-blocking frontend
  `reachableSquares` (BFS); decorative/impeding objects (rivers, rubble) shoot-over but slow
  crossing — GM marks each object's mode (see §2).
- **Impassable-square blackout (from alpha-pass, 2026-09-14):** a GM can mark individual
  squares black before/at setup — an MVP slice of Slice 3. Rides the same Slice-0 tables
  (`battle_map_squares` with an `impassable` flag, or `battle_map_tiles` carrying the marker);
  server rejects `addToken`/`placeToken`/`moveToken` into them; `reachableSquares` becomes
  path-aware (BFS) so walls block pathing, matching the decided §2 semantics. Filed as a
  feature issue; shipped after Flyway lands.
- **Persistence** through Slice 0 tables.

## 7. Chargen bugs surfaced during v2 planning

Found by the v2 planning deep-dive (bug-report agent, 2026-09-13). Track as fixes; do **not**
wait for slices if severity warrants.

- **Bug A — caster can skip Spells to Review with zero spells.** ✅ **Fixed in v1.1 (#58).**
   Verified 2026-09-14: no early return in `validateSpells`; backend emits the cantrip violation;
   wizard Next is gated (`canAdvance` step 7); covered by `CharacterJourneyIT` + wizard tests.
- **Bug B — class/subclass/race features missing.** ✅ **Fix shipped in v1.1 (#58)** — subclass +
   race features merge at compile and the sheet + Review render them — **and the residual for
   curated-only subclasses shipped 2026-09-14 (#90, design-v1-fixes #85)**: `collectedFeatures`
   keeps SRD `subclassLevels` authoritative where present and otherwise falls back to the curated
   `SUBCLASS_FEATURES` map in `ChargenCatalog` (all ~90 official 2014 archetypes, keyed by
   `class/subclass`). Still unmodeled: `feature_choices`.
- Shared: changing class leaves stale `spellIndexes` (no wizard invalidation); the Spells step
  caps cantrips but not leveled spells; fixtures under-model class features (only
  `spellcasting`/`channel-divinity`).

## 8. Guardrails (carried from v1)

- Backend jacoco ≥90% line gate; frontend Vitest + oxlint + prod build on every PR;
  functional `*IT` journeys extended per slice.
- **Coverage targets (policy, going forward):** **≥ 90% unit** (the enforced `tabletopservice`
  jacoco gate) **and ≥ 80% functional** (a PR/review guardrail over the packaged-JAR `*IT`
  suites) — any change adding behaviour adds functional coverage for it.
- **Run the functional suite before every commit/push:** `./mvnw verify` must pass before any
  commit or push (never a unit-only green run); use the H2 escape hatch
  (`./mvnw verify -Dtabletopserv.functional.db-url=jdbc:h2:mem:ft`) when Docker is unavailable,
  then let CI validate the Postgres-only suites.
- **Frontend (Vite app) — same rule, Playwright:** `tabletopweb/` mirrors the backend guardrails,
  with the functional/E2E layer in **Playwright** (unit stays Vitest): **≥ 90% unit / ≥ 80%
  functional** coverage targets, and the **Playwright suite must pass before every commit/push**
  touching the web app. Playwright is wired up: specs live in `tabletopweb/e2e/` (`npm run
  test:e2e` starts the Vite dev server and stubs the backend with `page.route`); Vitest excludes
  `e2e/**`. Any web behaviour change adds E2E coverage.
- No secrets, no direct outbound calls; model/tile assets are static files under
  `tabletopweb/public/` (curated + licensed), never server-fetched.
- Each slice: Wants.md entry → board item → `feature/v2-*` branch → PR → maintainer merge →
  `develop`→`master` propagation.

## 9. Open decisions for maintainer

All four resolved 2026-09-14 — see §2 "Decisions locked in".

---

Related: [`design-v1.md`](design-v1.md) · [`design-v1-fixes.md`](design-v1-fixes.md) ·
[`research/maps resources.md`](research/maps%20resources.md) · [`Wants.md`](Wants.md)