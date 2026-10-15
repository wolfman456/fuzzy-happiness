# avatar-gen — scripted modular-avatar asset generation

Headless, reproducible generation of the shared **CC0** avatar assets used by the v2 modular
avatar (design-draft-v2 §5, R51): the base bodies and the race-feature parts. No GUI step, no
committed toolchain.

## What it produces

`tabletopweb/public/models/avatars/base-{M,F}.glb` — one rigged body per sex:

- a single `Skin` material (re-coloured at runtime by `applyAvatarTints`),
- the `game_engine` skeleton (so the in-session 3D token can be animated later),
- named, **world-axis-aligned** socket empties that parts attach to: `socket_head`,
  `socket_hair`, `socket_ears`, `socket_horns`, `socket_tusks`, `socket_tail` — placed at
  anatomical anchors (top of head, mouth height, pelvis),
- Y-up, ~1 unit ≈ 1 metre, feet at the origin (matches `AvatarCanvas`'s camera).

`tabletopweb/public/models/avatars/parts/{kind}/{id}.glb` — additive features (each authored
around its own origin, so it lands exactly on its socket). Race features are fixed by the race
preset; `hair` is player-chosen from `appearance.hairStyle`:

| kind | ids | tint |
|---|---|---|
| `horns` | `dragonborn`, `tiefling` | fixed |
| `tusks` | `half-orc` | skin |
| `tail` | `dragonborn`, `tiefling` | fixed |
| `hair` | `short`, `ponytail`, `long` | `Hair` (tinted from `hairTone`) |

## Licence (all CC0)

MakeHuman's bundled assets and MPFB2's outputs are **CC0 1.0** (commercial use, no attribution).
The MakeHuman *application source* is AGPL, but nothing of it is shipped — only the generated
GLBs. The hand-authored parts are CC0. See `research/avatar resources.md`.

## Usage

```sh
tools/avatar-gen/fetch-tools.sh     # Blender 4.5 LTS + MPFB2 + CC0 assets → .cache/ (gitignored)
tools/avatar-gen/generate.sh        # → public/models/avatars/base-{M,F}.glb
tools/avatar-gen/generate_parts.sh  # → public/models/avatars/parts/{kind}/{id}.glb
```

`fetch-tools.sh` downloads into the gitignored `.cache/` (Blender tarball, `mpfb2-latest.zip`,
`makehuman_system_assets_cc0.zip`) and extracts Blender there — **no root, nothing committed**.
`generate.sh` installs MPFB2 into an isolated Blender config under `.cache/` on first run and runs
`generate_base.py` headless; `generate_parts.sh` needs no MPFB. Both are idempotent.

Regenerating is a deliberate, reviewable act: run the generator, inspect the diff of the `.glb`
files, and commit them together with any script change.

## Files

| File | Purpose |
|---|---|
| `fetch-tools.sh` | Download Blender LTS + MPFB2 + CC0 assets into `.cache/` |
| `generate.sh` | Isolated Blender config + `generate_base.py` |
| `generate_base.py` | Headless Blender script (build → rig → bake → material → sockets → export) |
| `generate_parts.sh` | Isolated Blender config + `generate_parts.py` |
| `generate_parts.py` | Headless Blender script for the race-feature and hair parts |
| `optimize.sh` | Optional glTF-Transform pass (texture resize/quantize) via `npx` |

## Conventions for parts

Parts live in `tabletopweb/public/models/avatars/parts/{kind}/{id}.glb`, are authored around the
matching `socket_*` origin, and use `Skin`/`Hair` (tintable) or a fixed material such as
`Horn`/`Scale`. A part must be registered in `AVATAR_PARTS` before a `RACE_PRESETS` entry may
reference it (race features) or `resolveAvatar` will attach it from `appearance.hairStyle` (hair);
see `tabletopweb/src/lib/avatars.js`. Pointed ears are deferred: the MPFB base bakes rounded ears
into the skin mesh, so they need a base variant, not an additive part.

## Options

`generate.sh [output-dir]` (default `tabletopweb/public/models/avatars`).
`generate_base.py` also accepts `--scale` (MPFB base scale, default `0.1`) and `--rig`.
