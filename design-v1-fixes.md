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