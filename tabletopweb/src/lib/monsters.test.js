import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./api', () => ({ api: vi.fn() }))

import { api } from './api'
import {
  MONSTER_CRS,
  MONSTER_EDITIONS,
  MONSTER_ROLES,
  generateMonster,
  listMyMonsters,
} from './monsters'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('monster API helpers', () => {
  it('generateMonster POSTs a generation request', () => {
    generateMonster({ cr: '7', role: 'BRUTE', edition: 'SRD_2014', name: 'Gravetusk' })
    expect(api).toHaveBeenCalledWith('/api/monsters/generate', {
      method: 'POST',
      body: { cr: '7', role: 'BRUTE', edition: 'SRD_2014', name: 'Gravetusk' },
    })
  })

  it('listMyMonsters fetches the owned list', () => {
    listMyMonsters()
    expect(api).toHaveBeenCalledWith('/api/monsters/mine')
  })

  it('exposes the same selectable surface as the backend engine', () => {
    expect(MONSTER_CRS).toContain('0')
    expect(MONSTER_CRS).toContain('1/8')
    expect(MONSTER_CRS).toContain('30')
    expect(MONSTER_ROLES).toContain('AUTO')
    expect(MONSTER_ROLES).toContain('SWARM')
    expect(MONSTER_EDITIONS).toEqual(['SRD_2014', 'SRD_2024'])
  })
})