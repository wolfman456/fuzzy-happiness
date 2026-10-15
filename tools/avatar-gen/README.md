# avatar-gen — scripted modular-avatar base generation

Headless, reproducible generation of the shared **CC0** avatar base bodies used by the v2
modular avatar (design-draft-v2 §5, R51). No GUI step, no committed toolchain.

## What it produces

`tabletopweb/public/models/avatars/base-{M,F}.glb` — one rigged body per sex:

- a single `Skin` material (re-coloured at runtime by `applyAvatarTints`),
- the `game_engine` skeleton (so the in-session 3D token can be animated later),
- named socket empties that parts attach to: `socket_head`, `socket_hair`, `socket_ears`,
  `socket_horns`, `socket_tusks`, `socket_tail`,
- Y-up, ~1 unit ≈ 1 metre, feet at the origin (matches `AvatarCanvas`'s camera).

## Licence (all CC0)

MakeHuman's bundled assets and MPFB2's outputs are **CC0 1.0** (commercial use, no attribution).
The MakeHuman *application source* is AGPL, but nothing of it is shipped — only the generated
GLBs. See `research/avatar resources.md`.

## Usage

```sh
tools/avatar-gen/fetch-tools.sh   # Blender 4.5 LTS + MPFB2 + CC0 assets → .cache/ (gitignored)
tools/avatar-gen/generate.sh      # → tabletopweb/public/models/avatars/base-{M,F}.glb
```

`fetch-tools.sh` downloads into the gitignored `.cache/` (Blender tarball, `mpfb2-latest.zip`,
`makehuman_system_assets_cc0.zip`) and extracts Blender there — **no root, nothing committed**.
`generate.sh` installs MPFB2 into an isolated Blender config under `.cache/` on first run and
runs `generate_base.py` headless. Both are idempotent.

Regenerating is a deliberate, reviewable act: run `generate.sh`, inspect the diff of the `.glb`
files, and commit them together with any `generate_base.py` change.

## Files

| File | Purpose |
|---|---|
| `fetch-tools.sh` | Download Blender LTS + MPFB2 + CC0 assets into `.cache/` |
| `generate.sh` | Set up an isolated Blender config and run `generate_base.py` |
| `generate_base.py` | The headless Blender script (build → rig → bake → material → sockets → export) |
| `optimize.sh` | Optional glTF-Transform pass (texture resize/quantize) via `npx` |

## Conventions for parts (next increment)

Parts go in `tabletopweb/public/models/avatars/parts/{kind}/{id}.glb`, are authored around the
matching `socket_*` origin, and use `Skin`/`Hair` material names so the runtime tint applies.
Register them in `AVATAR_PARTS` / `RACE_PRESETS` (`tabletopweb/src/lib/avatars.js`).

## Options

`generate.sh [output-dir]` (default `tabletopweb/public/models/avatars`).
`generate_base.py` also accepts `--scale` (MPFB base scale, default `0.1`) and `--rig`.
