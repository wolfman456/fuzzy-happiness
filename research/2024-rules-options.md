# 2024 Rules / Sheet Migration — Options

**Context:** the maintainer's 2026-10-13 answer to the locked `design-v1.md` §14 question on
character-sheet fields was to **"use the most current D&D sheet available"** — i.e. the **2024
rules / character sheet** (SRD 5.2 / 5.5e). The shipped v1 app is pinned to the **2014** ruleset
(SRD 5.1): the gateway targets `https://www.dnd5eapi.co/api/2014` and the chargen catalogs,
subclasses and backgrounds are all 2014-curated. This doc lays out the migration options; the
version decision itself is still open (§14 answer: *"unsure, provide options"*).

**Scope:** options research only — no code. The companion decisions live in
[`design-draft-v2.md`](../design-draft-v2.md) §2 "resolved 2026-10-13".

---

## 1. What "2024" means

- **SRD 5.2 / 5.2.1** is the 2024 core-rules System Reference Document (marketed as the 2024
  rules; rules text is "5.5e"). Released **2025-03-20**, licensed **CC-BY-4.0** (irrevocable).
- SRD 5.1 (2014) is *also* CC-BY-4.0, so we may build with **either or both**, with attribution.
- Mechanical differences that matter to chargen and the sheet:
  - **Species** replaces **race** (no ability-score bonuses from species by default).
  - **Backgrounds** grant ability-score increases and an origin feat; the SRD 5.2 background
    list is **4** (Acolyte, Criminal, Sage, Soldier) vs 5.1's single SRD Acolyte.
  - Weapon mastery, revised spells, revised class/subclass progressions, new sheet layout.
- Implication: 2014 and 2024 are **not drop-in compatible** — the same character concept can
  produce different numbers. Any migration must decide what happens to already-shipped 2014
  characters.

## 2. Data sources (verified 2026-10-13)

| Source | 2024 coverage | Notes |
|---|---|---|
| **dnd5eapi.co `/api/2024/*`** | ✅ live | The **same upstream the gateway already talks to**. `classes`=12, `species`=9, `backgrounds`=4, `equipment`=182. The gateway currently allowlists only `/api/2014`. |
| **Open5e API** (`api.open5e.com`, doc slug `wotc-srd` / `srd-2024`) | ✅ | Third-party aggregate, CC-BY-4.0; alternate/failover mirror source. |
| **Direct SRD 5.2.1** (WotC PDF + community JSON packages) | ✅ | Most control, most work; we'd parse and host it ourselves. |

**Key finding:** we do **not** need a new upstream to reach 2024 — `dnd5eapi.co` already serves
`/api/2024/`. The gateway change is an extra allowlisted path prefix plus the SRD mirror work;
no new egress integration.

## 3. Migration options

### Option A — Full swap to 2024
Replace every 2014 collection/endpoint with its 2024 equivalent and move the sheet to the 2024
layout.
- ➕ Single ruleset: less code, no branching, simplest long-term.
- ➖ **Breaks shipped 2014 characters** (numbers change) and invalidates the curated 2014
  subclass/background catalogs, which would need re-curation from 2024 sources.
- ➖ Flag-day migration; hard to roll back.

### Option B — Dual-run with a `ruleset` field ⭐ (recommended)
Keep 2014, add 2024, and carry a `ruleset` (`2014|2024`) on characters (and optionally rooms).
Both rulesets are mirrored; chargen branches per ruleset.
- ➕ No existing character breaks; new characters/rooms can opt into 2024 ("most current sheet").
- ➕ Fits the SRD-mirror decision (mirror both) and the per-room toggles already agreed.
- ➕ Full swap can happen later once 2024 proves out — same code path.
- ➖ Two catalogs and a ruleset branch through chargen/sheet; roughly the largest code surface.
- ➖ Doubles mirror storage and the curated-catalog maintenance.

### Option C — 2024 sheet layout over 2014 data (interim)
Show a 2024-style sheet UI but keep 2014 data underneath.
- ➕ Cheapest UI change.
- ➖ **Semantically wrong**: missing species, background ASIs, weapon mastery, revised spells.
  Almost certainly a dead end once players notice the numbers don't match the layout.
- ➖ Not recommended except as a pure visual experiment.

## 4. Effort / risk

| | A — full swap | B — dual-run ⭐ | C — reskin |
|---|---|---|---|
| Gateway/SRD mirror work | medium | medium | none |
| Chargen rule branches | none | high | none |
| Curated catalog rework | high (re-curate 2024) | high (add 2024 alongside) | none |
| Breaks shipped characters | **yes** | no | no |
| Reversible | low | high | n/a |

All options are gated on the **Slice 0 Flyway** migration and the **SRD mirror** decision; the
`ruleset` column in Option B is a schema change that `ddl-auto=validate` will reject until
Flyway lands.

## 5. Recommendation

**Option B (dual-run with a `ruleset` field), with 2024 as the default for new characters.**

Rationale: it satisfies "use the most current sheet" without a flag-day migration, preserves
every shipped 2014 character, and reuses the SRD-mirror infrastructure we already committed to.
Mirror 2024 alongside 2014 from the existing `dnd5eapi.co/api/2024` upstream; add
`ruleset` to `CharacterDraft`/`Dnd5eCharacter`/sheet DTOs; branch chargen. Once 2024 is proven,
Option A (full swap) is a small follow-up rather than a rewrite.

## 6. Open follow-ups (to schedule, not decide)

- Whether `ruleset` lives on the **room**, the **character**, or both (rooms hosting mixed parties).
- 2024 subclass/background **curation** plan (2024 SRD ships few; the homebrew class editor covers the rest).
- Mirror sync cadence and attribution string for CC-BY-4.0 (SRD 5.2.1 / WotC).
- Sheet-field diff 2014→2024 to size the client work.
