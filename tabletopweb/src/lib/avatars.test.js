import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  AVATAR_MODEL_BASE,
  AVATAR_MODELS,
  applyAvatarTints,
  avatarModelKey,
  avatarModelUrl,
  avatarSexCode,
  hueCss,
  resolveAvatarModel,
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

describe('avatarModelKey', () => {
  it('joins race index and sex code', () => {
    expect(avatarModelKey('dwarf', 'MALE')).toBe('dwarf-M')
    expect(avatarModelKey('tiefling', 'FEMALE')).toBe('tiefling-F')
  })

  it('returns null when race or sex is missing', () => {
    expect(avatarModelKey(null, 'MALE')).toBeNull()
    expect(avatarModelKey('dwarf', null)).toBeNull()
  })
})

describe('avatarModelUrl', () => {
  it('builds the conventional asset path', () => {
    expect(avatarModelUrl('dwarf', 'MALE')).toBe(`${AVATAR_MODEL_BASE}/dwarf-M.glb`)
  })

  it('returns null without a key', () => {
    expect(avatarModelUrl(null, null)).toBeNull()
  })
})

describe('resolveAvatarModel', () => {
  afterEach(() => {
    delete AVATAR_MODELS['dwarf-M']
  })

  it('returns null while no asset is curated', () => {
    expect(resolveAvatarModel('dwarf', 'MALE')).toBeNull()
  })

  it('returns the registered URL', () => {
    AVATAR_MODELS['dwarf-M'] = `${AVATAR_MODEL_BASE}/dwarf-M.glb`
    expect(resolveAvatarModel('dwarf', 'MALE')).toBe(`${AVATAR_MODEL_BASE}/dwarf-M.glb`)
  })

  it('returns null when the race/sex half is missing', () => {
    expect(resolveAvatarModel('', 'MALE')).toBeNull()
    expect(resolveAvatarModel('dwarf', '')).toBeNull()
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
