import { DEFAULT_APPEARANCE } from './characters'
import { RACE_PRESETS, hasAvatarPart, hueCss } from './avatars'

/**
 * Deterministic 2D face specification for the token/portrait face
 * (design-draft-v2 §5). A 3D render of the shared base mesh is a blank,
 * featureless mannequin that reads poorly at token size, so the map token and
 * the character-sheet portrait are drawn instead as a crisp, modular SVG face.
 *
 * `resolveFace` is a pure function of the same character config the 3D avatar
 * uses (`raceIndex`, `sex`, `appearance`), so the face reflects the modular
 * skin/hair/eye tints, the chosen hair style, and the race's ear shape, horns
 * and tusks. No randomness, no assets, no WebGL.
 */

/**
 * Race → ear shape. Pointed ears are the classic mark of the elder/under races;
 * everything else keeps the rounded human ear. (The 3D base bakes rounded ears
 * into the skin mesh, which is why the 3D side defers this — the 2D face can
 * express it directly.)
 */
export const RACE_EARS = {
  elf: 'pointed',
  'half-elf': 'pointed',
  gnome: 'pointed',
  tiefling: 'pointed',
}

/** The default ear shape when a race is unknown or unset. */
export const DEFAULT_EARS = 'rounded'

/** Eye hue rendered darker than the skin/hair tints so eyes read at token size. */
export function eyeCss(hue, fallback = DEFAULT_APPEARANCE.eyeColor) {
  const value = Number.isInteger(hue) ? hue : fallback
  return `hsl(${value} 45% 32%)`
}

/** Brow hue: the hair tint darkened so brows read against the skin. */
export function browCss(hue, fallback = DEFAULT_APPEARANCE.hairTone) {
  const value = Number.isInteger(hue) ? hue : fallback
  return `hsl(${value} 40% 28%)`
}

/**
 * The modular face spec for a character: resolved colours (CSS strings), the
 * validated hair style slug, the ear shape and the race-derived horn/tusk
 * overlays. Everything the SVG renderer needs, with no randomness.
 */
export function resolveFace(raceIndex, sex, appearance) {
  const preset = RACE_PRESETS[raceIndex]
  const parts = preset?.parts ?? []
  const partId = (kind) => parts.find((part) => part.kind === kind)?.id ?? null

  const hairStyle = hasAvatarPart('hair', appearance?.hairStyle) ? appearance.hairStyle : ''

  return {
    skin: hueCss(appearance?.skinTone, DEFAULT_APPEARANCE.skinTone),
    hair: hueCss(appearance?.hairTone, DEFAULT_APPEARANCE.hairTone),
    eye: eyeCss(appearance?.eyeColor),
    brow: browCss(appearance?.hairTone),
    hairStyle,
    ears: RACE_EARS[raceIndex] ?? DEFAULT_EARS,
    horns: partId('horns'),
    tusks: partId('tusks'),
    sex: sex === 'MALE' || sex === 'FEMALE' ? sex : null,
  }
}
