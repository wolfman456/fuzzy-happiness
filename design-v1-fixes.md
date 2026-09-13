# Design v1 — Live Bug-Fix Ledger

Sub-document to the **locked** [`design-v1.md`](design-v1.md). Tracks defects and fixes
against the shipped v1 design (the MVP released 2026-09-12). Each entry references the
relevant `design-v1.md` section, the GitHub issue/PR, and the shipped behavior change.

**Status key:** 🐛 open · 🔧 fixing · ✅ fixed

## 2026-09-12 triage (live-site pass 1)

Live verification pass on the deployed app produced the triage below. Priorities follow the
agreed convention (Severity `critical`/`high`/`medium`/`low` × Workaround × Audience
`customer-facing`/`informational`; P1 > P2 > …; a production customer-facing bug always
outranks an informational one).

| # | Issue | Severity | Priority | Status | Resolution |
|---|-------|----------|----------|--------|------------|
| #45 | New accounts couldn't log in: `SMTP_*` unset in prod → no verification email was ever deliverable (design-v1 §19). Registration is now auto-verified; the login verified-gate is removed; verification stays dormant until mail is configured | high | P1 | ✅ fixed | `fix/disable-email-verification` (PR B) |
| #46 | Age-gate mismatch: frontend used a ms-years approximation (`13×365.25`) while the backend uses `Period.between` — a user exactly 13 by calendar could be rejected client-side | medium | P2 | ✅ fixed | calendar-year gate (PR E) |
| #48 | Chargen subclass list only offers the single SRD example archetype per class (design-v1 §8) | high | P2 | ✅ fixed | curated catalog (PR D) |
| #49 | Intermittent chargen "compile/save" failure on first attempt (serial blocking SRD calls, no timeouts, compile inside `@Transactional`, Hikari pool 5) — zero observability | high | P2 | 🔧 fixing (PR C) | logging-only observability (PR C) |
| #50 | Background list only offers Acolyte (SRD) instead of the PHB backgrounds (design-v1 §8) | medium | P3 | ✅ fixed | curated catalog (PR D) |
| #51 | Class list beyond the SRD core (e.g. Artificer) — design-v1 defers this as R24 | low | P4 | ⏭ deferred | needs non-SRD data source |
| #52 | Chargen caster could skip the Spells step and still compile a "legal" sheet with **zero** spells (design-v1 §8) | high | P2 | ✅ fixed | `fix/chargen-bugs` (Bug A) |
| #53 | Compiled sheet carried **no features**: class level-ups, subclass features and race traits were absent from the payload and zero UI rendered them (design-v1 §8) | medium | P2 | ✅ fixed | `fix/chargen-bugs` (Bug B) |

## 2026-09-13 alpha pass (chargen UI readability)

Alpha run on chargen after the Bug A/B round produced four UI reports. All share one root
cause: the wizard's page chrome (heading, step flow, Back/Next) and the "Create Random
Character" quick-build button floated directly on the dark dragon backdrop (`ThemedBackdrop`),
so ghost borders + default/zinc-400/700 ink + `disabled:opacity-*` rendered near-invisible.

| # | Issue | Severity | Priority | Status | Resolution |
|---|-------|----------|----------|--------|------------|
| #59 | "Surprise me" quick-build button ghost-styled on the dragon backdrop → unreadable; label unclear about what it does | low | P3 | ✅ fixed | `fix/chargen-ui-readability` |
| #60 | Wizard Back/Next buttons (and their disabled states) on the dark backdrop → dark-on-dark; both look dead on entry | medium | P3 | ✅ fixed | `fix/chargen-ui-readability` |
| #61 | Wizard step flow is plain text — no jump navigation; only Back/Next can move | low | P3 | ✅ fixed | `fix/chargen-ui-readability` |
| #62 | "Character wizard" heading uses default ink on the dark backdrop → only readable when highlighted | low | P3 | ✅ fixed | `fix/chargen-ui-readability` |

## 2026-09-13 alpha pass (chargen scale-up)

Alpha run against a real character (`/characters/:id`) surfaced three chargen gaps (design-v1 §8):
the curated subclass catalog only covered the PHB/SRD options, the starting level was capped at 3
despite the whole feature pipeline supporting 1–20, and a character whose subclasses unlock above
the chosen starting level was hard-blocked on the Subclass step.

| # | Issue | Severity | Priority | Status | Resolution |
|---|-------|----------|----------|--------|------------|
| #64 | Starting level capped at 1–3 (DTO `@Max(3)` + wizard dropdown) — should allow 1–20 | medium | P3 | ✅ fixed | `fix/chargen-subclasses-level20` |
| #65 | Subclass step hard-blocks when subclasses unlock above the starting level (level 1–2 melee/martial) — nothing to choose yet | high | P2 | ✅ fixed | `fix/chargen-subclasses-level20` |
| #66 | Curated subclasses cover PHB/SRD only (barbarian 2 of ~8 official) — expand to all official 2014 sources | medium | P3 | ✅ fixed | `fix/chargen-subclasses-level20` |

## 2026-09-13 alpha pass (session battle map + monster search)

Alpha run on the session page's GM tools surfaced two gaps: the battle-map empty state sent
`createMap(..., { name: '' })` (the input never existed) so every valid submit bounced with
`400 name: must not be blank`, and the monster generator could only roll the dice — there was no
way to drop an existing SRD monster (e.g. a session-relevant goblin) onto the map by name.

| # | Issue | Severity | Priority | Status | Resolution |
|---|-------|----------|----------|--------|------------|
| #67 | Battle-map setup: every submit sent an empty `name` → backend rejected with `400 name: must not be blank` (design-v1 §11) | high | P2 | ✅ fixed | `fix/session-battlemap-monster` |
| #68 | Monster generator has no search — can't add an existing SRD monster to the map by name, only roll random statblocks | medium | P3 | ✅ fixed | `fix/session-battlemap-monster` |

## Changelog

- **2026-09-12 — verification disabled until prod SMTP is configured (#45).** `AuthService.register`
  sets `emailVerified(true)` and stops issuing/sending tokens; `AuthService.login` drops the
  verified gate. SMTP + token plumbing is preserved (endpoints, TTL, cooldown, senders), so
  re-enabling is a 2-line change plus `SMTP_*`/`FRONTEND_URL` dashboard vars (design-v1 §19);
  tracked as Wants.md R31.
- **2026-09-12 — logging-only observability for the compile/save flake (#49, PR C).** No behavior
  or response-body changes. A `CorrelationIdFilter` reuses or generates `X-Correlation-Id` per
  request — echoed back on the response header (also exposed via CORS) and in SLF4J/MDC log lines.
  `GatewayClient` per-call timing/logs and propagates the same id upstream; `SrdClient` logs each
  SRD fetch; `CharacterService.compile`/`create` log entry/exit timing inside the save transaction;
  `GlobalExceptionHandler` now logs full stacks for unhandled 500s and 5xx `ApiException`s; the
  gateway logs one structured line per request (method, path, status, correlation id, cache
  status, duration); the frontend `api()` surfaces `status` + `correlationId` on `ApiError`, so a
  failed chargen can be traced end-to-end.
- **2026-09-12 — curated PHB subclass/background catalog (#48/#50, PR D).** A `ChargenCatalog`
  component in `tabletopservice` carries the 2014 PHB 13 backgrounds and each class's PHB subclass
  options ({index, name, level}) — names/indices only, no rulebook text, no DB/DDL change. It is
  served at `GET /api/characters/catalog` (`ChargenCatalogDto` in `tabletopapi`). `CharacterService`
  merges the catalog with the SRD allow-lists: compiled drafts may use curated backgrounds and
  curated subclasses (level-gated per subclass); quick-build samples the curated pools deterministically
  (sorted indexes) and never fetches SRD detail for non-SRD backgrounds. The wizard loads the catalog
  and uses curated rows for the background step and subclass step, falling back to SRD when the
  endpoint is unavailable. The SRD example archetype keeps its canonical index so existing characters
  and quick-builds stay valid.
- **2026-09-13 — calendar-year age gate on registration (#46, PR E).** `RegisterPage` no longer
  blocks on a `13 × 365.25`-day ms approximation, which rejected a user exactly 13 by calendar
  (or slightly older, depending on time of day and leap years). The new `src/lib/age.js` helper
  mirrors the backend's `Period.between(dateOfBirth, today).getYears()` semantics (whole calendar
  years, birthday-not-yet-reached) and is used for the inline DOB check; the backend `AuthService`
  gate is unchanged and remains authoritative.
- **2026-09-13 — chargen caster spell floor (#52, Bug A).** A caster could dart from the Spells
  page to Review with zero spells and the backend compiled the draft "legal" (`validateSpells`
  early-returned on an empty pick set). `validateSpells` now requires a caster to pick at least
  `min(cantrips_known, available cantrips)` for the chosen level; the wizard gates the Next button
  the same way via `cantripsSelected < cantripsRequired`, shows a live "Pick at least N cantrips"
  alert on the Spells step, and the non-caster path is unchanged. Compile now rejects a
  cantrip-less caster (`sheet` null + violation) instead of silently certifying it.
- **2026-09-13 — features on compiled sheets (#53, Bug B).** The sheet's `featureIndexes` now
  merges class level-up features, subclass features up to the starting level and the race's
  `traits` (previously only class features — and even those were never rendered). The gateway and
  `SrdClient` allow the `subclasses → levels` subresource; `loadFacts` fetches it only for
  SRD-listed subclasses (curated PHB-only archetypes contribute nothing). The sheet page gained a
  Features section and the wizard Review gained a Features row (population requires a compile, so
  the row reads "after compiling" until then).
- **2026-09-13 — chargen UI readability on the themed backdrop (#59–#62).** The wizard now renders
  heading, step flow, step content and Back/Next inside one white card, so the `h1` ("Character
  wizard", explicit `text-zinc-900`), the flow (`text-zinc-600` inactive / solid chip active) and
  both nav buttons read on the light surface instead of fading into the dragon backdrop. The flow
  chips are now buttons: backward jumps always allowed, forward jumps gated by the same
  `canAdvance()` rule as Next (with an amber "Complete this step before jumping ahead" hint).
  The Characters-page quick build is relabeled **"Create Random Character"**, restyled solid
  (`bg-zinc-900 text-white`, matching "Guided wizard") so it no longer ghosts into the backdrop,
  and the empty-state copy matches. UI only — no behavior or API changes.
- **2026-09-13 — chargen scale-up (#64/#65/#66).** The curated `ChargenCatalog` subclass set grows
  from the PHB/SRD-only options to **all official 2014 subclasses** (PHB + XGtE + Tasha's + SCAG,
  101 across the 12 classes) with correct per-class unlock levels (cleric/sorcerer/warlock 1,
  druid/wizard 2, others 3); curated-only archetypes keep working exactly as before — SRD-listed
  ones merge their features, others contribute name only. Starting level is raised from 3 to 20:
  both request DTOs (`CharacterDraftDto`, `GenerateCharacterRequest`) relax to `@Max(20)` and the
  wizard dropdown offers 1–20. The Subclass step now **defers** instead of blocking below the unlock
  level ("Subclasses unlock at level N — nothing to choose yet", Next enabled, Review shows
  "none (unlocks at level N)"); compile still rejects a chosen subclass below its required level.
  Backend derivation (prof bonus, spell slots, features, HP) already scaled to 20 with the SRD
  level rows.
- **2026-09-13 — session battle-map name + SRD monster search (#67/#68).** The battle-map empty
  state now carries an inline **Name** field and submits `createMap(id, { name })`, defaulting to
  "Battle map" when left blank (fixing the `400 name: must not be blank` on every valid setup).
  The monster generator gains a **find-an-SRD-monster** search: the GM types a name, `srdList
  ('monsters', { name })` returns matching statblocks (already allow-listed in the gateway and
  `SrdClient`), and "Add to map" resolves the detail and drops a token with the SRD speed
  (`walk`, parsed to feet) and a stable per-index color, reusing the existing `addToken` flow.