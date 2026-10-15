#!/usr/bin/env python3
"""Generate the race-feature avatar parts (design-draft-v2 §5, R51).

Runs inside Blender (>= 4.2); driven by ``generate.sh``. Each part is authored
*around its own origin, upright in Blender space* and exported to
``parts/{kind}/{id}.glb``; at runtime ``AvatarCanvas`` parents it to the matching
``socket_*`` empty (which sits, world-axis-aligned, at the anatomical anchor), so
a part authored here appears in the right place without any per-part offsets.

Scope: additive features only — horns, tusks and tails. Pointed ears (elf /
half-elf / tiefling) are deliberately out of scope: MPFB's base body already
bakes rounded ears into the skin mesh, so ears need a base variant rather than an
additive part.

Usage (via generate.sh):
    blender --background --python generate_parts.py -- --out-dir <dir>
"""
from __future__ import annotations

import argparse
import math
import os
import sys

import bpy

HORN = ("Horn", (0.22, 0.18, 0.15), 0.55)
SCALE = ("Scale", (0.26, 0.22, 0.19), 0.5)
SKIN = ("Skin", (0.82, 0.62, 0.50), 0.85)


def parse_args() -> argparse.Namespace:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    parser = argparse.ArgumentParser(description="Generate avatar part GLBs")
    parser.add_argument("--out-dir", required=True, help="models/avatars directory")
    return parser.parse_args(argv)


def reset() -> None:
    bpy.ops.wm.read_factory_settings(use_empty=True)


def material(name: str, color: tuple[float, float, float], roughness: float) -> bpy.types.Material:
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Roughness"].default_value = roughness
    return mat


def cone(location, rotation, radius1, radius2, depth, vertices=12) -> bpy.types.Object:
    bpy.ops.mesh.primitive_cone_add(
        vertices=vertices,
        radius1=radius1,
        radius2=radius2,
        depth=depth,
        location=location,
        rotation=rotation,
    )
    return bpy.context.active_object


def merge(objs, name: str, mat: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    bpy.ops.object.join()
    obj = bpy.context.active_object
    obj.name = name
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    bpy.ops.object.shade_smooth()
    return obj


def export(path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format="GLB",
        use_selection=False,
        export_apply=False,
        export_yup=True,
        export_morph=False,
        export_skins=False,
        export_animations=False,
        export_cameras=False,
        export_lights=False,
    )


# Blender space: +Z up, -Y is the character's front. A cone points +Z by default;
# a positive X rotation tilts its tip toward -Y (front), negative toward +Y (back).


def build_horns(dragonborn: bool) -> None:
    reset()
    objs = []
    for side in (-1, 1):
        back = math.radians(-55 if dragonborn else -5)
        out = math.radians(side * (22 if dragonborn else 32))
        objs.append(
            cone((side * 0.045, 0.02, 0.05), (back * 0.4, out, 0.0), 0.024, 0.013, 0.11)
        )
        objs.append(
            cone((side * 0.085, 0.075, 0.13), (back, out, 0.0), 0.013, 0.002, 0.12)
        )
    name = "HornsDragonborn" if dragonborn else "HornsTiefling"
    merge(objs, name, material(*HORN))


def build_tusks() -> None:
    reset()
    objs = []
    for side in (-1, 1):
        objs.append(
            cone(
                (side * 0.028, -0.052, 0.0),
                (math.radians(18), 0.0, math.radians(side * 8)),
                0.013,
                0.003,
                0.055,
                vertices=8,
            )
        )
    merge(objs, "TusksHalfOrc", material(*SKIN))


def build_tail(dragonborn: bool) -> None:
    reset()
    objs = []
    if dragonborn:
        radius, length = 0.05, 0.16
        for i in range(3):
            step = i / 3
            objs.append(
                cone(
                    (0.0, 0.06 + step * 0.16, -0.03 - step * 0.16),
                    (math.radians(-125), 0.0, 0.0),
                    radius * (1 - 0.28 * i),
                    radius * (0.72 - 0.28 * i),
                    length,
                )
            )
        mat = material(*SCALE)
    else:
        radius = 0.03
        objs.append(cone((0.0, 0.10, -0.05), (math.radians(-120), 0.0, 0.0), radius, radius * 0.4, 0.34))
        objs.append(cone((0.0, 0.24, -0.20), (math.radians(-120), 0.0, 0.0), radius * 0.5, 0.001, 0.10))
        spade = cone((0.0, 0.30, -0.26), (math.radians(-120), 0.0, 0.0), 0.05, 0.0, 0.06, vertices=4)
        objs.append(spade)
        mat = material(*HORN)
    name = "TailDragonborn" if dragonborn else "TailTiefling"
    merge(objs, name, mat)


PARTS = {
    ("horns", "dragonborn"): lambda: build_horns(True),
    ("horns", "tiefling"): lambda: build_horns(False),
    ("tusks", "half-orc"): build_tusks,
    ("tail", "dragonborn"): lambda: build_tail(True),
    ("tail", "tiefling"): lambda: build_tail(False),
}


def main() -> None:
    args = parse_args()
    for (kind, part_id), builder in PARTS.items():
        builder()
        path = os.path.join(args.out_dir, "parts", kind, f"{part_id}.glb")
        export(path)
        print(f"[generate_parts] wrote {path} ({os.path.getsize(path)} bytes)")


if __name__ == "__main__":
    main()
