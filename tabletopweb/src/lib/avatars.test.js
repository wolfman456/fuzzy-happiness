import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  AVATAR_BASES,
  AVATAR_MODEL_BASE,
  attachToSocket,
  applyAvatarTints,
  avatarPartUrl,
  avatarSexCode,
  hueCss,
  RACE_PRESETS,
  resolveAvatar,
  supportsWebGL,
} from './avatars'

describe('avatarSexCode', () => {
  it('maps the Sex enum to asset codes', () => {
    expect(avatarSexCode('MALE')).toBe('M')
    expect(avatarSexCode('FEMALE')).toBe('F')
  })

  it('returns null for unset/unknown sex', () => {
    expect(avatarSexCode('')).toBeNull()
    expect(avatarSexCode(null)).toBeNull()
    expect(avatarSexCode(undefined)).toBeNull()
  })
})

describe('avatarPartUrl', () => {
  it('builds the conventional part path', () => {
    expect(avatarPartUrl('horns', 'curved')).toBe(`${AVATAR_MODEL_BASE}/parts/horns/curved.glb`)
  })

  it('returns null when a half is missing', () => {
    expect(avatarPartUrl('', 'curved')).toBeNull()
    expect(avatarPartUrl('horns', null)).toBeNull()
  })
})

describe('resolveAvatar', () => {
  afterEach(() => {
    delete RACE_PRESETS.elf.parts
  })

  it('returns the sex base and race scale', () => {
    expect(resolveAvatar('dwarf', 'MALE')).toEqual({
      base: AVATAR_BASES.M,
      parts: [],
      scale: 0.8,
    })
    expect(resolveAvatar('human', 'FEMALE').base).toBe(AVATAR_BASES.F)
  })

  it('falls back to the default preset for an unknown race', () => {
    expect(resolveAvatar('goblin', 'MALE')).toEqual({
      base: AVATAR_BASES.M,
      parts: [],
      scale: 1,
    })
  })

  it('resolves preset parts to socket+url entries', () => {
    RACE_PRESETS.elf.parts = [{ kind: 'ears', id: 'long', socket: 'socket_ears' }]
    expect(resolveAvatar('elf', 'MALE').parts).toEqual([
      { socket: 'socket_ears', url: `${AVATAR_MODEL_BASE}/parts/ears/long.glb` },
    ])
  })

  it('drops parts missing a socket or asset id', () => {
    RACE_PRESETS.elf.parts = [
      { kind: 'ears', id: 'long', socket: '' },
      { kind: '', id: 'long', socket: 'socket_ears' },
    ]
    expect(resolveAvatar('elf', 'MALE').parts).toEqual([])
  })

  it('returns null without a usable sex', () => {
    expect(resolveAvatar('dwarf', '')).toBeNull()
    expect(resolveAvatar('dwarf', null)).toBeNull()
  })
})

describe('supportsWebGL', () => {
  it('reports a boolean capability flag', () => {
    expect(typeof supportsWebGL()).toBe('boolean')
  })
})

describe('attachToSocket', () => {
  it('parents the part at the named socket', () => {
    const part = { name: 'part' }
    const socket = { add: vi.fn() }
    const root = { getObjectByName: vi.fn((name) => (name === 'socket_ears' ? socket : null)) }

    expect(attachToSocket(root, 'socket_ears', part)).toBe(true)
    expect(socket.add).toHaveBeenCalledWith(part)
  })

  it('returns false when the socket is absent or inputs are invalid', () => {
    expect(attachToSocket({ getObjectByName: () => null }, 'socket_ears', {})).toBe(false)
    expect(attachToSocket(null, 'socket_ears', {})).toBe(false)
    expect(attachToSocket({ getObjectByName: () => ({ add: vi.fn() }) }, 's', null)).toBe(false)
  })
})

describe('hueCss', () => {
  it('renders a hue as an HSL colour', () => {
    expect(hueCss(200)).toBe('hsl(200 45% 60%)')
    expect(hueCss(0)).toBe('hsl(0 45% 60%)')
  })

  it('falls back for null/invalid hues', () => {
    expect(hueCss(null, 30)).toBe('hsl(30 45% 60%)')
    expect(hueCss(undefined, 30)).toBe('hsl(30 45% 60%)')
    expect(hueCss(1.5, 30)).toBe('hsl(30 45% 60%)')
    expect(hueCss('120', 30)).toBe('hsl(30 45% 60%)')
  })

  it('defaults the fallback to 0', () => {
    expect(hueCss(null)).toBe('hsl(0 45% 60%)')
  })
})

function colorStub() {
  return { set: vi.fn() }
}

function mesh(material) {
  return { isMesh: true, material }
}

describe('applyAvatarTints', () => {
  it('recolours Skin/Hair materials from the appearance', () => {
    const skin = colorStub()
    const hair = colorStub()
    const root = {
      traverse: (fn) => [
        mesh({ name: 'Skin.001', color: skin }),
        mesh({ name: 'hair', color: hair }),
      ].forEach(fn),
    }

    expect(applyAvatarTints(root, { skinTone: 10, hairTone: 200 })).toBe(root)
    expect(skin.set).toHaveBeenCalledWith('hsl(10 45% 60%)')
    expect(hair.set).toHaveBeenCalledWith('hsl(200 45% 60%)')
  })

  it('uses default tints when appearance is absent', () => {
    const color = colorStub()
    applyAvatarTints(
      { traverse: (fn) => fn(mesh({ name: 'Skin', color })) },
      undefined,
    )
    expect(color.set).toHaveBeenCalledTimes(1)
    expect(color.set.mock.calls[0][0]).toMatch(/^hsl\(\d+ 45% 60%\)$/)
  })

  it('handles material arrays and skips non-colourable nodes', () => {
    const a = colorStub()
    const b = colorStub()
    const armor = colorStub()
    const root = {
      traverse: (fn) => [
        mesh([{ name: 'Skin', color: a }, { name: 'Hair', color: b }]),
        mesh({ name: 'Armor', color: armor }),
        mesh(null),
        { isMesh: false },
        null,
      ].forEach(fn),
    }

    applyAvatarTints(root, { skinTone: 5, hairTone: 6 })
    expect(a.set).toHaveBeenCalledWith('hsl(5 45% 60%)')
    expect(b.set).toHaveBeenCalledWith('hsl(6 45% 60%)')
    expect(armor.set).not.toHaveBeenCalled()
  })

  it('returns non-scene inputs untouched', () => {
    expect(applyAvatarTints(null, {})).toBeNull()
    expect(applyAvatarTints({}, {})).toEqual({})
  })
})
