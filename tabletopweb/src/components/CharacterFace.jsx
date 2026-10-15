import { resolveFace } from '../lib/face'

/**
 * A deterministic, modular 2D face for the map token and the character-sheet
 * portrait (design-draft-v2 §5). Drawn as inline SVG from `resolveFace`, it
 * reads at token size (32px) where a full-body 3D render does not, and reflects
 * the modular config: skin/hair/eye tints, hair style, race ear shape and the
 * dragonborn/tiefling horns and half-orc tusks.
 *
 * Unknown races and unset fields fall back to neutral defaults, so the face is
 * always renderable — no assets, no WebGL, works in jsdom.
 */
export default function CharacterFace({
  raceIndex,
  sex,
  appearance,
  size = 64,
  testId = 'character-face',
  bordered = true,
  className = '',
}) {
  const face = resolveFace(raceIndex, sex, appearance)

  const cx = 50
  const cy = 56
  const rx = sex === 'MALE' ? 35 : sex === 'FEMALE' ? 32 : 34
  const ry = 40
  const eyeDx = Math.round(rx * 0.36)
  const eyeRx = 8
  const eyeRy = 6
  const browWidth = sex === 'MALE' ? 3.4 : sex === 'FEMALE' ? 2.4 : 2.8

  const headTop = cy - ry
  const hairR = rx + 2
  const hairRy = ry + 2

  const ears =
    face.ears === 'pointed' ? (
      <g data-testid="face-ears">
        <path d={`M${cx - rx + 2},${cy - 8} L${cx - rx - 16},${cy - 24} L${cx - rx + 6},${cy + 10} Z`} fill={face.skin} />
        <path d={`M${cx + rx - 2},${cy - 8} L${cx + rx + 16},${cy - 24} L${cx + rx - 6},${cy + 10} Z`} fill={face.skin} />
      </g>
    ) : (
      <g data-testid="face-ears">
        <ellipse cx={cx - rx + 4} cy={cy + 2} rx="7" ry="10" fill={face.skin} />
        <ellipse cx={cx + rx - 4} cy={cy + 2} rx="7" ry="10" fill={face.skin} />
      </g>
    )

  const hasHair = face.hairStyle !== ''
  const hairFront = hasHair ? (
    <path
      data-testid="face-hair"
      d={`M${cx - hairR},${cy - 6} A${hairR},${hairRy} 0 0 1 ${cx + hairR},${cy - 6} Z`}
      fill={face.hair}
    />
  ) : null

  const longPanels = face.hairStyle === 'long' ? (
    <g data-testid="face-hair-sides">
      <rect x={cx - rx - 6} y={cy - 12} width="13" height="46" rx="6" fill={face.hair} />
      <rect x={cx + rx - 7} y={cy - 12} width="13" height="46" rx="6" fill={face.hair} />
    </g>
  ) : null

  const ponytail =
    face.hairStyle === 'ponytail' ? (
      <ellipse data-testid="face-ponytail" cx={cx + rx + 6} cy={headTop + 2} rx="10" ry="15" fill={face.hair} />
    ) : null

  const horns =
    face.horns === 'tiefling' ? (
      <g data-testid="face-horns">
        <path d={`M${cx - 16},${headTop + 8} C${cx - 26},${headTop - 4} ${cx - 28},${headTop - 14} ${cx - 34},${headTop - 20} C${cx - 18},${headTop - 12} ${cx - 6},${headTop} ${cx - 8},${headTop + 12} Z`} fill="#7a4b3a" />
        <path d={`M${cx + 16},${headTop + 8} C${cx + 26},${headTop - 4} ${cx + 28},${headTop - 14} ${cx + 34},${headTop - 20} C${cx + 18},${headTop - 12} ${cx + 6},${headTop} ${cx + 8},${headTop + 12} Z`} fill="#7a4b3a" />
      </g>
    ) : face.horns === 'dragonborn' ? (
      <g data-testid="face-horns">
        <path d={`M${cx - 18},${headTop + 14} L${cx - 36},${headTop - 12} L${cx - 4},${headTop + 4} Z`} fill="#c9b79a" />
        <path d={`M${cx + 18},${headTop + 14} L${cx + 36},${headTop - 12} L${cx + 4},${headTop + 4} Z`} fill="#c9b79a" />
      </g>
    ) : null

  const tusks = face.tusks ? (
    <g data-testid="face-tusks">
      <path d={`M${cx - 10},${cy + 24} L${cx - 7},${cy + 10} L${cx - 3},${cy + 24} Z`} fill="#f3ead6" />
      <path d={`M${cx + 10},${cy + 24} L${cx + 7},${cy + 10} L${cx + 3},${cy + 24} Z`} fill="#f3ead6" />
    </g>
  ) : null

  return (
    <span
      data-testid={testId}
      aria-hidden="true"
      className={`block shrink-0 overflow-hidden rounded-full bg-zinc-100 ${
        bordered ? 'border border-zinc-300' : ''
      } ${className}`}
      style={{ width: size, height: size }}
    >
      <svg viewBox="0 0 100 100" width="100%" height="100%" preserveAspectRatio="xMidYMid slice">
        <rect width="100" height="100" fill="#f4f4f5" />
        {ears}
        {longPanels}
        {ponytail}
        <ellipse data-testid="face-skin" cx={cx} cy={cy} rx={rx} ry={ry} fill={face.skin} />
        {hairFront}
        {horns}
        {tusks}
        <g data-testid="face-eyes">
          <ellipse cx={cx - eyeDx} cy={cy} rx={eyeRx} ry={eyeRy} fill="#ffffff" />
          <ellipse cx={cx + eyeDx} cy={cy} rx={eyeRx} ry={eyeRy} fill="#ffffff" />
          <circle cx={cx - eyeDx} cy={cy} r="3.2" fill={face.eye} />
          <circle cx={cx + eyeDx} cy={cy} r="3.2" fill={face.eye} />
        </g>
        <g stroke={face.brow} strokeWidth={browWidth} strokeLinecap="round" fill="none">
          <path d={`M${cx - eyeDx - 7},${cy - 12} Q${cx - eyeDx},${cy - 16} ${cx - eyeDx + 7},${cy - 12}`} />
          <path d={`M${cx + eyeDx - 7},${cy - 12} Q${cx + eyeDx},${cy - 16} ${cx + eyeDx + 7},${cy - 12}`} />
        </g>
        <path
          d={`M${cx},${cy + 4} L${cx},${cy + 12} Q${cx + 3},${cy + 14} ${cx + 6},${cy + 12}`}
          stroke="#c98f80"
          strokeWidth="2"
          strokeLinecap="round"
          fill="none"
        />
        <path
          data-testid="face-mouth"
          d={`M${cx - 8},${cy + 20} Q${cx},${cy + 28} ${cx + 8},${cy + 20}`}
          stroke="#a15c57"
          strokeWidth="2.5"
          strokeLinecap="round"
          fill="none"
        />
      </svg>
    </span>
  )
}
