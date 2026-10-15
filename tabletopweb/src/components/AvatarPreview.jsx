import { Component, Suspense, lazy, useCallback, useState } from 'react'
import { DEFAULT_APPEARANCE } from '../lib/characters'
import { hueCss, resolveAvatar, supportsWebGL } from '../lib/avatars'
import {
  avatarSnapshotKey,
  getAvatarSnapshot,
  setAvatarSnapshot,
} from '../lib/avatarSnapshot'

// three.js + R3F are heavy, so the Canvas is code-split: it is only downloaded
// once a base asset exists and WebGL is available (design-draft-v2 §5).
const AvatarCanvas = lazy(() => import('./AvatarCanvas'))

/** Flat 2D stand-in for an avatar: a skin-tinted disc with a hair arc. */
export function AvatarSwatch({ appearance, size = 64, testId = 'appearance-swatch', className = '' }) {
  return (
    <div
      data-testid={testId}
      aria-hidden="true"
      className={`relative shrink-0 overflow-hidden rounded-full border border-zinc-300 ${className}`}
      style={{
        width: size,
        height: size,
        backgroundColor: hueCss(appearance?.skinTone, DEFAULT_APPEARANCE.skinTone),
      }}
    >
      <span
        className="absolute bottom-0 left-1/2 -translate-x-1/2 rounded-t-full"
        style={{
          width: Math.round(size * 0.6),
          height: Math.round(size * 0.28),
          backgroundColor: hueCss(appearance?.hairTone, DEFAULT_APPEARANCE.hairTone),
        }}
      />
    </div>
  )
}

/** Swaps to the 2D swatch when a model fails to load or WebGL is unavailable. */
export class AvatarErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { failed: false }
  }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  render() {
    return this.state.failed ? this.props.fallback : this.props.children
  }
}

/**
 * The cached PNG snapshot of an avatar scene, shown as a plain circle image.
 * Used where a portrait should render without a live WebGL context (the
 * character sheet, and later the map token) — see `lib/avatarSnapshot`.
 */
export function AvatarPortrait({ src, size = 64, testId = 'appearance-swatch', className = '' }) {
  return (
    <img
      data-testid={testId}
      aria-hidden="true"
      alt=""
      src={src}
      width={size}
      height={size}
      className={`shrink-0 rounded-full border border-zinc-300 bg-zinc-100 object-cover ${className}`}
      style={{ width: size, height: size }}
    />
  )
}

/**
 * Renders a character's 3D avatar when a base body exists for its sex and WebGL
 * is available, and the 2D tint swatch otherwise (or if the model fails).
 * Changing race/sex re-resolves the base, scale and parts.
 *
 * Every live canvas captures a PNG snapshot into the shared, config-keyed cache
 * (`lib/avatarSnapshot`). With `portrait`, a cached snapshot renders as a plain
 * `<img>` instead of the live canvas, and the canvas is only mounted to fill a
 * cache miss.
 */
export default function AvatarPreview({
  raceIndex,
  sex,
  appearance,
  size = 64,
  testId = 'appearance-swatch',
  className = '',
  portrait = false,
}) {
  const avatar = resolveAvatar(raceIndex, sex)
  const key = avatarSnapshotKey(raceIndex, sex, appearance)
  const [snapshot, setSnapshot] = useState(() => (portrait ? getAvatarSnapshot(key) : null))
  const [snapshotKey, setSnapshotKey] = useState(key)

  if (snapshotKey !== key) {
    setSnapshotKey(key)
    setSnapshot(portrait ? getAvatarSnapshot(key) : null)
  }

  const onCapture = useCallback(
    (url) => {
      setAvatarSnapshot(key, url)
      if (portrait) setSnapshot(url)
    },
    [key, portrait],
  )

  const fallback = (
    <AvatarSwatch appearance={appearance} size={size} testId={testId} className={className} />
  )

  if (portrait && snapshot) {
    return <AvatarPortrait src={snapshot} size={size} testId={testId} className={className} />
  }

  if (!avatar || !supportsWebGL()) return fallback

  return (
    <AvatarErrorBoundary fallback={fallback}>
      <Suspense fallback={fallback}>
        <AvatarCanvas
          avatar={avatar}
          appearance={appearance}
          size={size}
          testId={testId}
          className={className}
          capture
          onCapture={onCapture}
        />
      </Suspense>
    </AvatarErrorBoundary>
  )
}
