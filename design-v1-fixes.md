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
| #46 | Age-gate mismatch: frontend used a ms-years approximation (`13×365.25`) while the backend uses `Period.between` — a user exactly 13 by calendar could be rejected client-side | medium | P2 | 🔧 open | frontend gate to backend logic |
| #48 | Chargen subclass list only offers the single SRD example archetype per class (design-v1 §8) | high | P2 | 🔧 open (triage PR A) | curated catalog (planned PR D) |
| #49 | Intermittent chargen "compile/save" failure on first attempt (serial blocking SRD calls, no timeouts, compile inside `@Transactional`, Hikari pool 5) — zero observability | high | P2 | 🔧 open (triage PR A) | logging-first (planned PR C) |
| #50 | Background list only offers Acolyte (SRD) instead of the PHB backgrounds (design-v1 §8) | medium | P3 | 🔧 open (triage PR A) | curated catalog (planned PR D) |
| #51 | Class list beyond the SRD core (e.g. Artificer) — design-v1 defers this as R24 | low | P4 | ⏭ deferred | needs non-SRD data source |

## Changelog

- **2026-09-12 — verification disabled until prod SMTP is configured (#45).** `AuthService.register`
  sets `emailVerified(true)` and stops issuing/sending tokens; `AuthService.login` drops the
  verified gate. SMTP + token plumbing is preserved (endpoints, TTL, cooldown, senders), so
  re-enabling is a 2-line change plus `SMTP_*`/`FRONTEND_URL` dashboard vars (design-v1 §19);
  tracked as Wants.md R31.