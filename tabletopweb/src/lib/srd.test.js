import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./api', () => ({ api: vi.fn() }))

import { api } from './api'
import { offeredClassSkills, srdDetail, srdList, srdRows, srdSubresource, subclassLevel } from './srd'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('srd catalog helpers', () => {
  it('lists a collection with curated query params', () => {
    srdList('spells', { level: '3', school: 'evocation' })
    expect(api).toHaveBeenCalledWith('/api/srd/spells?level=3&school=evocation')
  })

  it('lists a collection with no params when none are relevant', () => {
    srdList('races', {})
    expect(api).toHaveBeenCalledWith('/api/srd/races')
  })

  it('fetches a detail resource', () => {
    srdDetail('races', 'dwarf')
    expect(api).toHaveBeenCalledWith('/api/srd/races/dwarf')
  })

  it('fetches a class subresource', () => {
    srdSubresource('classes', 'cleric', 'spells')
    expect(api).toHaveBeenCalledWith('/api/srd/classes/cleric/spells')
  })

  it('normalizes list results into {index, name} rows', () => {
    expect(srdRows({ results: [{ index: 'dwarf', name: 'Dwarf' }] })).toEqual([
      { index: 'dwarf', name: 'Dwarf' },
    ])
    expect(srdRows({})).toEqual([])
    expect(srdRows({ results: null })).toEqual([])
  })

  it('extracts offered class skills from proficiency_choices', () => {
    const cleric = {
      proficiency_choices: [
        {
          choose: 2,
          from: {
            options: [
              { item: { index: 'skill-medicine' } },
              { item: { index: 'skill-religion' } },
              { item: { index: 'weapons' } },
            ],
          },
        },
      ],
    }
    expect([...offeredClassSkills(cleric)]).toEqual(['skill-medicine', 'skill-religion'])
    expect(offeredClassSkills(null)).toEqual(new Set())
  })

  it('defaults the subclass level to 1 when absent', () => {
    expect(subclassLevel({ subclass_level: 3 })).toBe(3)
    expect(subclassLevel({})).toBe(1)
    expect(subclassLevel(null)).toBe(1)
  })
})