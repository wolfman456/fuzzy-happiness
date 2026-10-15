#!/usr/bin/env python3
"""Generate the shared modular-avatar base meshes (design-draft-v2 §5, R51).

Runs inside Blender (>= 4.2) with the MPFB2 extension installed; driven by
``generate.sh``. For each sex it creates a MakeHuman body from MPFB's bundled
CC0 base mesh, fits the game-engine skeleton, strips helper geometry, applies a
single flat ``Skin`` material (re-coloured at runtime by ``applyAvatarTints``)
and writes ``base-M.glb`` / ``base-F.glb`` with head/tail attachment sockets.

Everything is CC0: MPFB's base mesh, targets and skin assets are CC0 1.0, and
the generated output carries no third-party material.

Usage (via generate.sh, which sets up the Blender/MPFB environment):
    blender --background --python generate_base.py -- --out-dir <dir>
"""
from __future__ import annotations

import argparse
import os
import sys

import bpy

from bl_ext.user_default.mpfb.services.humanservice import HumanService
from bl_ext.user_default.mpfb.services.targetservice import TargetService

# Socket empties, parented to a rig bone, that parts attach to at runtime
# (three.js looks them up by name via scene.getObjectByName).
SOCKETS: list[tuple[str, str]] = [
    ("socket_head", "head"),
    ("socket_hair", "head"),
    ("socket_ears", "head"),
    ("socket_horns", "head"),
    ("socket_tusks", "head"),
    ("socket_tail", "pelvis"),
]

# Race-neutral skin base colour; the runtime tint recolours the ``Skin`` material.
SKIN_COLOR = (0.82, 0.62, 0.50)


def parse_args() -> argparse.Namespace:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    parser = argparse.ArgumentParser(description="Generate avatar base GLBs")
    parser.add_argument("--out-dir", required=True, help="output directory for base-*.glb")
    parser.add_argument("--scale", type=float, default=0.1, help="MPFB base-mesh scale")
    parser.add_argument("--rig", default="game_engine", help="MPFB built-in rig name")
    return parser.parse_args(argv)


def make_active(obj: bpy.types.Object) -> None:
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj


def bake_and_strip_helpers(basemesh: bpy.types.Object) -> None:
    """Bake the macro-detail shape keys into the mesh, then apply the helper mask.

    Shape keys must be baked *after* the rig was fitted (it reads the helper
    vertex groups) but *before* the mask modifier is applied (Blender refuses to
    apply modifiers to meshes that still have shape keys).
    """
    make_active(basemesh)
    if basemesh.data.shape_keys:
        bpy.ops.object.shape_key_remove(all=True, apply_mix=True)
    for modifier in list(basemesh.modifiers):
        if modifier.type == "MASK":
            bpy.ops.object.modifier_apply(modifier=modifier.name)


def make_skin_material() -> bpy.types.Material:
    material = bpy.data.materials.new("Skin")
    material.use_nodes = True
    bsdf = material.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*SKIN_COLOR, 1.0)
    bsdf.inputs["Roughness"].default_value = 0.85
    return material


def add_socket(name: str, armature: bpy.types.Object, bone: str) -> None:
    empty = bpy.data.objects.new(name, None)
    bpy.context.scene.collection.objects.link(empty)
    empty.empty_display_size = 0.05
    empty.parent = armature
    empty.parent_type = "BONE"
    empty.parent_bone = bone


def build_base(sex: str, scale: float, rig: str) -> None:
    bpy.ops.wm.read_factory_settings(use_empty=True)

    macro = TargetService.get_default_macro_info_dict()
    macro["gender"] = 1.0 if sex == "M" else 0.0

    basemesh = HumanService.create_human(scale=scale, macro_detail_dict=macro)
    basemesh.name = "Avatar"

    armature = HumanService.add_builtin_rig(basemesh, rig)
    if armature is None:
        raise RuntimeError(f"Could not fit rig '{rig}'")
    armature.name = "Armature"

    bake_and_strip_helpers(basemesh)

    basemesh.data.materials.clear()
    basemesh.data.materials.append(make_skin_material())

    for socket_name, bone in SOCKETS:
        add_socket(socket_name, armature, bone)


def export_glb(path: str) -> None:
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format="GLB",
        use_selection=False,
        export_apply=False,
        export_yup=True,
        export_morph=False,
        export_skins=True,
        export_animations=False,
        export_cameras=False,
        export_lights=False,
    )


def main() -> None:
    args = parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    for sex in ("M", "F"):
        build_base(sex, args.scale, args.rig)
        path = os.path.join(args.out_dir, f"base-{sex}.glb")
        export_glb(path)
        size = os.path.getsize(path)
        print(f"[generate_base] wrote {path} ({size} bytes)")


if __name__ == "__main__":
    main()
