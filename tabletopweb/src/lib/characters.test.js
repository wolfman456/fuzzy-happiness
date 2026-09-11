import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./api', () => ({ api: vi.fn() }))

import { api } from './api'
import {
  SCORE_SOURCES,
  PHB_STANDARD_ARRAY,
  abilityModifier,
  compileCharacter,
  createCharacter,
  equipmentPriceGp,
  generateCharacter,
  getCharacter,
  listMyCharacters,
  pointBuyCost,
  rollScores,
  startingGoldClassBudget,
  validateBaseScores,
} from './characters'

const legalDraft = {
  name: 'Tordek',
  strength: 15,
  dexterity: 14,
  constitution: 15,
  intelligence: 12,
  wisdom: 10,
  charisma: 8,
  scoreSource: 'STANDARD_ARRAY',
  startingLevel: 1,
  raceIndex: 'dwarf',
  classIndex: 'cleric',
  subclassIndex: 'life',
  backgroundIndex: 'acolyte',
  skillPickIndexes: ['medicine', 'religion'],
  spellIndexes: ['sacred-flame', 'cure-wounds'],
  equipmentIndexes: ['leather-armor', 'shield'],
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('character API helpers', () => {
  it('compileCharacter POSTs a draft to the compile endpoint', () => {
    compileCharacter(legalDraft)
    expect(api).toHaveBeenCalledWith('/api/characters/compile', {
      method: 'POST',
      body: legalDraft,
    })
  })

  it('generateCharacter POSTs a quick-build request', () => {
    generateCharacter({ name: 'Surprise', seed: 42 })
    expect(api).toHaveBeenCalledWith('/api/characters/generate', {
      method: 'POST',
      body: { name: 'Surprise', seed: 42 },
    })
  })

  it('rollScores POSTs a roll request for server-authoritative dice', () => {
    rollScores({ scoreSource: 'FOUR_D6_DROP_LOWEST' })
    expect(api).toHaveBeenCalledWith('/api/characters/roll-scores', {
      method: 'POST',
      body: { scoreSource: 'FOUR_D6_DROP_LOWEST' },
    })
  })

  it('createCharacter POSTs to my characters', () => {
    createCharacter(legalDraft)
    expect(api).toHaveBeenCalledWith('/api/users/me/characters', {
      method: 'POST',
      body: legalDraft,
    })
  })

  it('listMyCharacters fetches the owned list', () => {
    listMyCharacters()
    expect(api).toHaveBeenCalledWith('/api/users/me/characters')
  })

  it('getCharacter fetches a single owned sheet', () => {
    getCharacter(7)
    expect(api).toHaveBeenCalledWith('/api/users/me/characters/7')
  })
})

describe('score source math', () => {
  it('exposes the same score sources as the backend enum', () => {
    expect(SCORE_SOURCES).toEqual([
      'STANDARD_ARRAY',
      'POINT_BUY',
      'FOUR_D6_DROP_LOWEST',
      'HOUSE_RULE_D20',
    ])
  })

  it('bakes the PHB standard array', () => {
    expect(PHB_STANDARD_ARRAY).toEqual([15, 14, 13, 12, 10, 8])
  })

  it('computes PHB point-buy costs', () => {
    expect(pointBuyCost(8)).toBe(0)
    expect(pointBuyCost(13)).toBe(5)
    expect(pointBuyCost(15)).toBe(9)
    expect(pointBuyCost(7)).toBe(Number.MAX_SAFE_INTEGER)
    expect(pointBuyCost(16)).toBe(Number.MAX_SAFE_INTEGER)
  })

  it('computes ability modifiers', () => {
    expect(abilityModifier(10)).toBe(0)
    expect(abilityModifier(8)).toBe(-1)
    expect(abilityModifier(15)).toBe(2)
    expect(abilityModifier(20)).toBe(5)
  })

  it('validates legal base score sets per source', () => {
    expect(validateBaseScores('STANDARD_ARRAY', { strength: 15, dexterity: 13, constitution: 14, intelligence: 12, wisdom: 10, charisma: 8 })).toBe(true)
    expect(validateBaseScores('STANDARD_ARRAY', { strength: 15, dexterity: 15, constitution: 14, intelligence: 12, wisdom: 10, charisma: 8 })).toBe(false)
    expect(validateBaseScores('POINT_BUY', { strength: 15, dexterity: 15, constitution: 15, intelligence: 8, wisdom: 8, charisma: 8 })).toBe(true)
    expect(validateBaseScores('POINT_BUY', { strength: 15, dexterity: 15, constitution: 15, intelligence: 15, wisdom: 8, charisma: 8 })).toBe(false)
    expect(validateBaseScores('FOUR_D6_DROP_LOWEST', { strength: 3, dexterity: 4, constitution: 5, intelligence: 6, wisdom: 7, charisma: 18 })).toBe(true)
    expect(validateBaseScores('HOUSE_RULE_D20', { strength: 1, dexterity: 30, constitution: 2, intelligence: 29, wisdom: 3, charisma: 28 })).toBe(true)
    expect(validateBaseScores('HOUSE_RULE_D20', null)).toBe(false)
  })

  it('mirrors the backend class starting-gold budgets', () => {
    expect(startingGoldClassBudget('monk')).toBe(12)
    expect(startingGoldClassBudget('druid')).toBe(50)
    expect(startingGoldClassBudget('sorcerer')).toBe(75)
    expect(startingGoldClassBudget('rogue')).toBe(100)
    expect(startingGoldClassBudget('fighter')).toBe(125)
    expect(startingGoldClassBudget('mystery')).toBe(100)
  })

  it('publishes SRD starter-gear prices in gp', () => {
    expect(equipmentPriceGp('leather-armor')).toBe(10)
    expect(equipmentPriceGp('shield')).toBe(10)
    expect(equipmentPriceGp('plate-armor')).toBe(1500)
    expect(equipmentPriceGp('not-an-item')).toBeNull()
  })
})