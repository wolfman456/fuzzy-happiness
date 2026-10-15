import { describe, expect, it } from 'vitest'
import { browCss, eyeCss, resolveFace } from './face'
import { DEFAULT_APPEARANCE } from './characters'
import { hueCss } from './avatars'

describe('eyeCss / browCss', () => {
  it('uses the provided hue', () => {
    expect(eyeCss(120)).toBe('hsl(120 45% 32%)')
    expect(browCss(60)).toBe('hsl(60 40% 28%)')
  })

  it('falls back to the default tint for missing/invalid hues', () => {
    expect(eyeCss(null)).toBe(`hsl(${DEFAULT_APPEARANCE.eyeColor} 45% 32%)`)
    expect(eyeCss(undefined)).toBe(`hsl(${DEFAULT_APPEARANCE.eyeColor} 45% 32%)`)
    expect(browCss(null)).toBe(`hsl(${DEFAULT_APPEARANCE.hairTone} 40% 28%)`)
  })
})

describe('resolveFace', () => {
  it('resolves the default tints and no race features for an unknown race', () => {
    const face = resolveFace('kobold', null, null)
    expect(face.skin).toBe(hueCss(DEFAULT_APPEARANCE.skinTone))
    expect(face.hair).toBe(hueCss(DEFAULT_APPEARANCE.hairTone))
    expect(face.eye).toBe(eyeCss(null))
    expect(face.brow).toBe(browCss(null))
    expect(face.hairStyle).toBe('')
    expect(face.ears).toBe('rounded')
    expect(face.horns).toBeNull()
    expect(face.tusks).toBeNull()
    expect(face.sex).toBeNull()
  })

  it('reflects the character tints', () => {
    const face = resolveFace('human', 'FEMALE', { skinTone: 10, hairTone: 200, eyeColor: 90 })
    expect(face.skin).toBe(hueCss(10))
    expect(face.hair).toBe(hueCss(200))
    expect(face.eye).toBe(eyeCss(90))
    expect(face.sex).toBe('FEMALE')
  })

  it('gives pointed ears to the elder/under races', () => {
    expect(resolveFace('elf', 'MALE', null).ears).toBe('pointed')
    expect(resolveFace('half-elf', null, null).ears).toBe('pointed')
    expect(resolveFace('gnome', null, null).ears).toBe('pointed')
    expect(resolveFace('tiefling', null, null).ears).toBe('pointed')
    expect(resolveFace('human', null, null).ears).toBe('rounded')
    expect(resolveFace('dwarf', null, null).ears).toBe('rounded')
  })

  it('derives horns and tusks from the race presets', () => {
    expect(resolveFace('dragonborn', null, null).horns).toBe('dragonborn')
    expect(resolveFace('tiefling', null, null).horns).toBe('tiefling')
    expect(resolveFace('half-orc', null, null).tusks).toBe('half-orc')
    expect(resolveFace('human', null, null).horns).toBeNull()
    expect(resolveFace('elf', null, null).tusks).toBeNull()
  })

  it('keeps a registered hair style and drops an unknown one', () => {
    expect(resolveFace('human', null, { hairStyle: 'ponytail' }).hairStyle).toBe('ponytail')
    expect(resolveFace('human', null, { hairStyle: 'mohawk' }).hairStyle).toBe('')
    expect(resolveFace('human', null, null).hairStyle).toBe('')
  })

  it('normalises an unset/unknown sex to null', () => {
    expect(resolveFace('human', '', null).sex).toBeNull()
    expect(resolveFace('human', 'OTHER', null).sex).toBeNull()
    expect(resolveFace('human', 'MALE', null).sex).toBe('MALE')
  })
})
