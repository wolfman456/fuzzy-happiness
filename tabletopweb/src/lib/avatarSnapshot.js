/**
 * Deterministic, config-keyed PNG snapshots of the avatar scene
 * (design-draft-v2 §5, item 4). A snapshot is captured client-side from the
 * live three.js canvas and reused as the character-sheet portrait (and, in
 * time, the map token) so a portrait can render as a plain `<img>` — no WebGL,
 * no backend storage, no Flyway migration. The cache is in-memory only: it is
 * rebuilt from the scene on each page load, and the same configuration always
 * maps to the same key.
 */

const cache = new Map()

function tint(value) {
  return Number.isInteger(value) ? value : 'd'
}

/**
 * Stable cache key for an avatar configuration. Missing race/sex/tints are
 * normalised so `null` and `undefined` shapes share one entry.
 */
export function avatarSnapshotKey(raceIndex, sex, appearance) {
  const style = appearance?.hairStyle ?? ''
  return `${raceIndex ?? ''}|${sex ?? ''}|s${tint(appearance?.skinTone)}|h${tint(appearance?.hairTone)}|${style}`
}

/** The cached PNG data URL for `key`, or null when nothing has been captured. */
export function getAvatarSnapshot(key) {
  return cache.get(key) ?? null
}

/** Stores a captured PNG data URL under `key` (no-op for empty input). */
export function setAvatarSnapshot(key, dataUrl) {
  if (!key || !dataUrl) return
  cache.set(key, dataUrl)
}

/** Drops every cached snapshot (used by tests; the cache is otherwise unbounded). */
export function clearAvatarSnapshots() {
  cache.clear()
}
