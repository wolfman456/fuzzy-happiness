# Avatar Resources — Free 3D Character Model Research

**Context:** v2 Slice 2b (`design-draft-v2.md` §5) renders a wizard **3D preview** of a
character's avatar, tinted by the skin/hair hues chosen on the Appearance step. Slice 2c turns
that into a **modular/parametric** avatar (shared base body + race-feature parts + hair). The
renderer, resolver and fallback are implemented in `tabletopweb/`; the **assets are generated
in-repo** by the scripted pipeline in [`tools/avatar-gen/`](../tools/avatar-gen/).

> **Revised 2026-10-14 (Slice 2c, R51):** the avatar is **modular** — one shared rigged **CC0
> base body per sex** with named sockets (`socket_ears|horns|tusks|tail|hair`), plus swappable
> **race-feature parts** (ears/horns/tusks/tail) and **hair-style** parts. This supersedes both
> the original 18-file per-race×sex plan *and* the "no parametric body builder" decision (R12
> re-opened). Hosted/AI platforms are ruled out by the **self-hosted + offline + CC0**
> constraint: Ready Player Me (**shut down 2026-01-31**, Netflix-acquired), Avaturn (~$800/mo),
> Avatar SDK / MetaPerson (enterprise local-compute).

---

## What the app expects

- **Bases:** `tabletopweb/public/models/avatars/base-{M|F}.glb` — one shared body per sex,
  Y-up, ~1 unit ≈ 1 metre, feet at the origin, a rig, a single `Skin` material, and the socket
  empties listed above.
- **Parts:** `tabletopweb/public/models/avatars/parts/{kind}/{id}.glb` (e.g.
  `parts/horns/tiefling-curved.glb`). Each part is authored around its socket origin and uses
  `Skin`/`Hair` material names so the runtime tint applies.
- **Registration** in `tabletopweb/src/lib/avatars.js`: `AVATAR_BASES`, `AVATAR_PARTS` and
  `RACE_PRESETS` (per-race `scale` + `{ kind, id, socket }` parts). `resolveAvatar()` composes
  them; anything unregistered is simply absent. `RACE_PRESETS.parts` stays empty until the part
  GLBs exist, so a missing file can never trip the swatch fallback.
- **Format:** glTF-binary (`.glb`); the committed bases are ~0.9 MB each. Use
  `tools/avatar-gen/optimize.sh` (glTF-Transform) if a part grows large.
- **Tinting:** the renderer recolours materials whose **name contains `Skin` or `Hair`**
  (case-insensitive) to the chosen hues. Name materials accordingly.
- **WebGL fallback:** with no WebGL (or a load failure) the UI shows the 2D tint swatch.

## The chosen pipeline — MakeHuman (CC0) via MPFB2, scripted

MakeHuman's **bundled assets are CC0 1.0** (base mesh, proxies, targets, textures) since
September 2020, and **exports are CC0** — commercial use, no attribution. Only the MakeHuman
*application source* is AGPL, and we ship neither it nor a service based on it.

| Tool | Role | License |
|---|---|---|
| Blender ≥ 4.2 (LTS 4.5) | host + glTF export | GPL (app, not shipped) |
| **MPFB2** (MakeHuman Plugin For Blender) | builds the human, fits the rig | GPL-3 (addon, not shipped) |
| MakeHuman system assets (`makehuman_system_assets_cc0.zip`) | skins/eyes/teeth | **CC0 1.0** |
| Generated `base-*.glb` | our output | **CC0 1.0** |

The pipeline lives in [`tools/avatar-gen/`](../tools/avatar-gen/):

```sh
tools/avatar-gen/fetch-tools.sh     # Blender LTS + MPFB2 + CC0 assets into .cache/ (gitignored)
tools/avatar-gen/generate.sh        # headless: base-{M,F}.glb into public/models/avatars/
tools/avatar-gen/optimize.sh        # optional glTF-Transform pass
```

`generate_base.py` (run by `generate.sh`) creates the body from MPFB's bundled CC0 base mesh,
fits the `game_engine` skeleton, bakes the macro-detail shape keys, strips helper geometry,
applies the flat `Skin` material and adds the socket empties, then exports GLB. It is headless
and reproducible — no GUI step.

## Race-feature + hair parts (next increment)

Authored **ourselves** in Blender (clean CC0 ownership, consistent style) as simple meshes built
around each socket origin, exported to `parts/{kind}/{id}.glb`. Planned mapping (SRD races):

| Race | ear | horn | tusk | tail | notes |
|---|---|---|---|---|---|
| elf | long pointed | — | — | — | |
| half-elf | slight point | — | — | — | |
| half-orc | round | — | yes | — | |
| dragonborn | round | yes | — | yes | scaled up ~1.05 |
| tiefling | long pointed | yes | — | yes | |
| dwarf / gnome / halfling | round | — | — | — | scaled down (0.75–0.8) |
| human | round | — | — | — | baseline |

## Superseded sources (kept for reference / non-MakeHuman fallback)

Before the modular decision, per-race poses were sourced from loose low-poly packs. These remain
useful for **parts or a stylised base** if the MakeHuman look is ever swapped out:

| Source | Kind | License | Use for us |
|---|---|---|---|
| KayKit Adventurers | Low-poly fantasy characters | **CC0** | Stylised base / accessories |
| Quaternius Universal Base | Modular male/female humans | **CC0** | Per-sex base alternative |
| Kenney Characters | Very low-poly figures | **CC0** | Fallback / LOD tier |
| Poly Pizza (https://poly.pizza) | CC0/CC-BY search index | per-model (CC0) | Discovery for race gaps |
| Sketchfab (CC0 filter) | Model marketplace | per-model (CC0) | Race-specific one-offs |

**Recommendation:** keep the **MakeHuman + MPFB2 scripted** pipeline as the base-body source and
**self-author** the race/hair parts around the sockets, normalising to the naming + orientation
above. No hosted/AI avatar service is compatible with the self-hosted + offline + CC0
constraint. Generated bases are committed (≈1.8 MB total); downloads/toolchain stay out of git.
