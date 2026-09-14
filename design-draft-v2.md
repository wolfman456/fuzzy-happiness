# Fuzzy Happiness — Design v2 (Draft)

Draft spec for the second major release of the tabletop platform. Companion to the **locked**
[`design-v1.md`](design-v1.md) (MVP, shipped 2026-09-12). Nothing here is binding until the
maintainer approves the slices; the living bug ledger stays in
[`design-v1-fixes.md`](design-v1-fixes.md).

**Status:** 🏗 in planning — v2 vision + slice plan agreed (2026-09-13).

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

## 3. Slice 0 — Foundation: Flyway + migrations

Groundwork every other slice leans on.

- Add Flyway to `tabletopserv`/`tabletopservice` (prod profile). Baseline the current schema
  (`baselineOnMigrate` or a hand-authored `V1__baseline.sql` from the entities), then
  `V2__v2_appearance_maps.sql`:
  - `character` / `dnd5e_characters`: `sex` (`varchar(10)`, `MALE|FEMALE`) and `appearance`
    (`jsonb`: avatar GLB ref + skin/hair tints).
  - New tables `battle_map_tiles` (`map_id`, `x`, `y`, `tile_index`, PK `(map_id,x,y)`) and
    `battle_map_objects` (`map_id`, `id`, `object_index`, `x`, `y`, `w`, `h`, `rotation`,
    `blocks_movement`).
- **Risk (deliberate):** prod DB is live. The baseline must be validated against a copy of prod
  Postgres before the real deploy. Dev (H2) and functional-IT paths keep Hibernate-managed
  schema unless Flyway proves safe on both; the decision lands here once tested.
- Deliverables: `design-draft-v2.md` (this file, once approved), Wants.md v2 entries, board
  items.

## 4. Slice 1 — Wizard labeling + site appearance / ease of use

Pure-frontend polish; cheapest slice, unblocks the rest.

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
- No secrets, no direct outbound calls; model/tile assets are static files under
  `tabletopweb/public/` (curated + licensed), never server-fetched.
- Each slice: Wants.md entry → board item → `feature/v2-*` branch → PR → maintainer merge →
  `develop`→`master` propagation.

## 9. Open decisions for maintainer

All four resolved 2026-09-14 — see §2 "Decisions locked in".

---

Related: [`design-v1.md`](design-v1.md) · [`design-v1-fixes.md`](design-v1-fixes.md) ·
[`research/maps resources.md`](research/maps%20resources.md) · [`Wants.md`](Wants.md)