# Avatar Resources — Free 3D Character Model Research

**Context:** v2 Slice 2b (`design-draft-v2.md` §5) renders a **wizard 3D preview** of a
character's **race × sex** avatar, tinted by the skin/hair hues chosen on the Appearance step.
The renderer, resolver and fallback are implemented; the **models themselves are a manual
curation step**. This doc records where to source them and under what licences.

**Scope:** free/CC0 sources only, link-only research — **nothing is bundled into the repo**.
Mirrors `research/maps resources.md`: downloads are a manual curation step when we actually
need the assets.

> **Revised 2026-10-14 (Slice 2c):** the avatar is now **modular/parametric** — one rigged
> **CC0 base per sex** (MakeHuman-derived, authored in Blender) with named sockets
> (`socket_ears|horns|tusks|tail|hair`), plus swappable **race-feature parts** (ears/horns/
> tusks/tail) and a **hair-style** part. The single per-race×sex GLB plan described below is
> superseded; the base + parts need the same CC0 sourcing treatment. Hosted/AI platforms
> (Ready Player Me — **shut down 2026-01-31** — Avaturn, Avatar SDK) are ruled out by the
> **self-hosted + offline + CC0** constraint chosen for the project.

---

## What the app expects

- **Naming:** `tabletopweb/public/models/avatars/{race}-{M|F}.glb` (e.g. `dwarf-M.glb`,
  `tiefling-F.glb`), one GLB per **race × sex** (9 SRD races × 2 sexes = **18 models**).
- **Registration:** add an entry to `AVATAR_MODELS` in `tabletopweb/src/lib/avatars.js`
  (`"dwarf-M": "/models/avatars/dwarf-M.glb"`). Until then the resolver returns `null` and the
  UI falls back to the 2D tint swatch.
- **Format:** glTF-binary (`.glb`), low-poly, ideally < ~1 MB each so the wizard stays snappy.
- **Tinting (optional per model):** the renderer recolours materials whose **name contains
  `Skin` or `Hair`** (case-insensitive) to the chosen hues. Assets that name those materials
  just work; others render with baked colours until renamed/remapped.
- **Orientation:** Y-up, roughly 1 unit ≈ 1 metre, feet at the origin (so a shared camera works).

## The hard part

No single free pack ships **nine D&D-specific fantasy races**. The realistic paths are:

1. **One modular humanoid base per sex, differentiated by tint + scale/accessory** — cheapest
   and consistent; the v2 decision ("no parametric body builder") still allows picking a
   distinct *curated mesh* per race where one exists.
2. **Curate loose CC0 single models** (dwarf/elf/orc/…) from Sketchfab / Poly Pizza and
   normalise them to the naming + orientation above.

Either way the set is hand-assembled; the code does not care which, only that the files land
with the right names.

---

## KayKit — Character Pack: Adventurers (Kay Lousberg)

- **Type:** direct **source** of stylised low-poly **fantasy characters**.
- **What it offers:** 4–5 rigged + animated adventurers (knight, rogue, mage, barbarian, …),
  75 animations, 25+ accessories; single 1024×1024 gradient-atlas texture (downsampleable).
  Also **Skeletons / Goblins / Halloween** packs by the same author.
- **Format:** `.FBX` + `.glTF`; GitHub mirror
  (https://github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0).
- **License:** **CC0 1.0** ("free for personal and commercial use, no attribution required";
  please don't resell unmodified copies). Public-domain, safe to bundle.
- **Verdict:** the **best fantasy base**. Tint the atlas materials (rename to `Skin`/`Hair`) or
  use as-is; map the closest hero mesh per race where the pack allows.

## Quaternius — Universal Base Characters / RPG Character Pack

- **Type:** direct **source** of modular low-poly humans.
- **What it offers:** **Universal Base Characters** — separate **male/female** base meshes with
  modular clothing; **Ultimate Modular Characters** for a wider wardrobe; **RPG Character Pack**
  (6 fantasy characters, rigged + animated); **Universal Animation Library** for retargeting.
- **Format:** glTF / FBX / OBJ (some `.blend`).
- **License:** **CC0** ("free to use in personal, educational and commercial projects").
- **Verdict:** **primary base-mesh source** — the male/female bases give clean per-sex bodies to
  tint and later equip. Pairs well with KayKit accessories.

## Kenney — Character assets (with Kay Lousberg)

- **Type:** direct **source** of very low-poly characters (`Character A–R`, `Male/Female A–F`).
- **What it offers:** node-rigged / skinned stylised figures with baked animations; tiny
  (a few hundred–few thousand verts), great for weak devices.
- **Format:** GLB.
- **License:** **CC0 1.0**.
- **Verdict:** a good **fallback/lod** tier; less fantasy-specific than KayKit, simpler geometry.

## Poly Pizza — https://poly.pizza

- **Type:** **search index** aggregating CC0 / CC-BY models (incl. many Google Poly + Quaternius
  uploads), downloadable as GLB.
- **License:** per-model (filter to **CC0** only).
- **Verdict:** the **discovery layer** for filling specific race gaps (a dwarf, a dragonborn, …).

## Sketchfab — https://sketchfab.com (CC0 filter)

- **Type:** **broad marketplace** of single models; filter `Downloadable` + `CC0`.
- **License:** per-model; only CC0 is usable here.
- **Verdict:** best for **race-specific one-offs** (elf/dwarf/orc/half-orc/dragonborn) that are
  missing from the curated packs; normalise scale + orientation by hand.

---

## Cross-cutting takeaways

| Source | Kind | License | Use for us |
|---|---|---|---|
| KayKit Adventurers | Low-poly fantasy characters | **CC0** | **Primary** fantasy base mesh |
| Quaternius Universal Base | Modular male/female humans | **CC0** | Per-sex base to tint/equip |
| Kenney Characters | Very low-poly figues | **CC0** | Fallback / LOD tier |
| Poly Pizza | CC0/CC-BY search index | per-model (CC0) | Discovery for race gaps |
| Sketchfab | Model marketplace | per-model (CC0) | Race-specific one-offs |

**Recommendation:** build the 18 models from **Quaternius Universal Base** (per sex) **+ KayKit
Adventurers** accessories, filling race gaps from **Poly Pizza / Sketchfab (CC0)**; normalise to
`{race}-{M|F}.glb`, Y-up with feet at origin, and name the skin/hair materials so the tint
sliders apply. **Downloads stay a manual step — nothing is committed unless we choose to bundle
a vetted set.** The app renders the 2D swatch until a model is registered.
