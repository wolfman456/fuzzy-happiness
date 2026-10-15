import { afterEach, describe, expect, it } from 'vitest'
import {
  avatarSnapshotKey,
  clearAvatarSnapshots,
  getAvatarSnapshot,
  setAvatarSnapshot,
} from './avatarSnapshot'

afterEach(() => {
  clearAvatarSnapshots()
})

describe('avatarSnapshotKey', () => {
  it('is deterministic for the same configuration', () => {
    const a = avatarSnapshotKey('dwarf', 'FEMALE', { skinTone: 120, hairTone: 300 })
    const b = avatarSnapshotKey('dwarf', 'FEMALE', { skinTone: 120, hairTone: 300 })
    expect(a).toBe(b)
  })

  it('differs when race, sex or a tint changes', () => {
    const base = avatarSnapshotKey('dwarf', 'FEMALE', { skinTone: 120, hairTone: 300 })
    expect(avatarSnapshotKey('elf', 'FEMALE', { skinTone: 120, hairTone: 300 })).not.toBe(base)
    expect(avatarSnapshotKey('dwarf', 'MALE', { skinTone: 120, hairTone: 300 })).not.toBe(base)
    expect(avatarSnapshotKey('dwarf', 'FEMALE', { skinTone: 121, hairTone: 300 })).not.toBe(base)
  })

  it('normalises missing values and future hair styles', () => {
    const empty = avatarSnapshotKey(undefined, undefined, undefined)
    expect(empty).toBe(avatarSnapshotKey(null, null, null))
    expect(avatarSnapshotKey('elf', 'MALE', { hairStyle: 'braids' })).toContain('braids')
  })
})

describe('avatar snapshot cache', () => {
  it('returns null until a snapshot is stored', () => {
    expect(getAvatarSnapshot('dwarf|FEMALE|s120|h300|')).toBeNull()
  })

  it('stores and retrieves a snapshot by key', () => {
    setAvatarSnapshot('k', 'data:image/png;base64,AAA')
    expect(getAvatarSnapshot('k')).toBe('data:image/png;base64,AAA')
  })

  it('ignores empty keys or data URLs', () => {
    setAvatarSnapshot('', 'data:image/png;base64,AAA')
    setAvatarSnapshot('k', '')
    expect(getAvatarSnapshot('')).toBeNull()
    expect(getAvatarSnapshot('k')).toBeNull()
  })

  it('clears every entry', () => {
    setAvatarSnapshot('k', 'data:image/png;base64,AAA')
    clearAvatarSnapshots()
    expect(getAvatarSnapshot('k')).toBeNull()
  })
})
