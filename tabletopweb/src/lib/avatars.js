import { DEFAULT_APPEARANCE } from './characters'

/** Where curated avatar GLBs live (design-draft-v2 §5). */
export const AVATAR_MODEL_BASE = '/models/avatars'

/**
 * Curated race×sex GLB registry, keyed by `${raceIndex}-${M|F}`. **Empty until the
 * CC0 assets are curated** (see `research/avatar resources.md`); while a key is
 * absent `resolveAvatarModel` returns null and the UI shows the 2D tint swatch.
 * To light up a model: drop `{race}-{M|F}.glb` in `public/models/avatars/` and add
 * `'dwarf-M': `${AVATAR_MODEL_BASE}/dwarf-M.glb`` here.
 */
export const AVATAR_MODELS = {}

/** Maps the `Sex` enum to the single-letter asset code used in filenames. */
export function avatarSexCode(sex) {
  if (sex === 'MALE') return 'M'
  if (sex === 'FEMALE') return 'F'
  return null
}

/** Registry key for a race×sex pair, or null when either half is missing. */
export function avatarModelKey(raceIndex, sex) {
  const code = avatarSexCode(sex)
  return raceIndex && code ? `${raceIndex}-${code}` : null
}

/** The conventional asset path for a race×sex pair (used when curating/registering). */
export function avatarModelUrl(raceIndex, sex) {
  const key = avatarModelKey(raceIndex, sex)
  return key ? `${AVATAR_MODEL_BASE}/${key}.glb` : null
}

/** Resolves a registered GLB URL, or null when the avatar has no curated asset yet. */
export function resolveAvatarModel(raceIndex, sex) {
  const key = avatarModelKey(raceIndex, sex)
  return key ? AVATAR_MODELS[key] ?? null : null
}

/** CSS colour for a hue slider value; a null/invalid hue falls back to the default tint. */
export function hueCss(hue, fallback = 0) {
  const value = Number.isInteger(hue) ? hue : fallback
  return `hsl(${value} 45% 60%)`
}

/**
 * Recolours a loaded glTF scene from the appearance tints: materials whose name
 * contains `Skin`/`Hair` (case-insensitive) get the chosen hue. Meshes without
 * such materials keep their baked colours. Returns the same root for chaining.
 */
export function applyAvatarTints(root, appearance) {
  if (!root || typeof root.traverse !== 'function') return root
  const skin = hueCss(appearance?.skinTone, DEFAULT_APPEARANCE.skinTone)
  const hair = hueCss(appearance?.hairTone, DEFAULT_APPEARANCE.hairTone)
  root.traverse((node) => {
    if (!node || node.isMesh !== true) return
    const materials = Array.isArray(node.material) ? node.material : [node.material]
    materials.forEach((material) => {
      if (!material || !material.color) return
      const name = String(material.name || '').toLowerCase()
      if (name.includes('skin')) material.color.set(skin)
      else if (name.includes('hair')) material.color.set(hair)
    })
  })
  return root
}
