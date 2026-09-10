import { api } from './api'

export const SCORE_SOURCES = [
  'STANDARD_ARRAY',
  'POINT_BUY',
  'FOUR_D6_DROP_LOWEST',
  'HOUSE_RULE_D20',
]

export const ABILITIES = [
  'strength',
  'dexterity',
  'constitution',
  'intelligence',
  'wisdom',
  'charisma',
]

export const SCORE_SOURCE_LABELS = {
  STANDARD_ARRAY: 'Standard array (15,14,13,12,10,8)',
  POINT_BUY: '27-point buy',
  FOUR_D6_DROP_LOWEST: 'Roll 4d6, drop lowest',
  HOUSE_RULE_D20: 'House-rule 6×d20',
}

export const PHB_STANDARD_ARRAY = [15, 14, 13, 12, 10, 8]

/** PHB point-buy cost (p.13); out-of-range scores are unaffordable. */
export function pointBuyCost(score) {
  switch (score) {
    case 8:
      return 0
    case 9:
      return 1
    case 10:
      return 2
    case 11:
      return 3
    case 12:
      return 4
    case 13:
      return 5
    case 14:
      return 7
    case 15:
      return 9
    default:
      return Number.MAX_SAFE_INTEGER
  }
}

export function abilityModifier(score) {
  return Math.floor((score - 10) / 2)
}

export function compileCharacter(draft) {
  return api('/api/characters/compile', { method: 'POST', body: draft })
}

export function generateCharacter(body) {
  return api('/api/characters/generate', { method: 'POST', body })
}

export function listMyCharacters() {
  return api('/api/users/me/characters')
}

export function createCharacter(draft) {
  return api('/api/users/me/characters', { method: 'POST', body: draft })
}

export function getCharacter(id) {
  return api(`/api/users/me/characters/${id}`)
}

/** Rebuilds a create/compile draft from a compiled sheet (quick-build → save/revise). */
export function sheetToDraft(sheet) {
  return {
    name: sheet.name,
    strength: sheet.strength,
    dexterity: sheet.dexterity,
    constitution: sheet.constitution,
    intelligence: sheet.intelligence,
    wisdom: sheet.wisdom,
    charisma: sheet.charisma,
    scoreSource: sheet.scoreSource,
    startingLevel: sheet.level,
    raceIndex: sheet.raceIndex,
    classIndex: sheet.classIndex,
    subclassIndex: sheet.subclassIndex,
    backgroundIndex: sheet.backgroundIndex,
    skillPickIndexes: [...(sheet.skillPicks ?? [])],
    spellIndexes: [...(sheet.spellIndexes ?? [])],
    equipmentIndexes: [...(sheet.equipmentIndexes ?? [])],
  }
}