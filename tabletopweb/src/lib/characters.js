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

/**
 * Server-authoritative pre-racial score roll. The server decides every die, so
 * the wizard can force honest rolls instead of free assignment.
 */
export function rollScores(body) {
  return api('/api/characters/roll-scores', { method: 'POST', body })
}

/** Client mirror of the backend legality check (the server still re-validates). */
export function validateBaseScores(source, base) {
  const values = ABILITIES.map((ability) => base?.[ability])
  if (!values.every(Number.isInteger)) return false
  switch (source) {
    case 'STANDARD_ARRAY': {
      const sorted = [...values].sort((a, b) => b - a)
      return sorted.join(',') === [...PHB_STANDARD_ARRAY].sort((a, b) => b - a).join(',')
    }
    case 'POINT_BUY':
      return values.every((value) => value >= 8 && value <= 15)
        && values.reduce((sum, value) => sum + pointBuyCost(value), 0) <= 27
    case 'FOUR_D6_DROP_LOWEST':
      return values.every((value) => value >= 3 && value <= 18)
    case 'HOUSE_RULE_D20':
      return values.every((value) => value >= 1 && value <= 30)
    default:
      return false
  }
}

/** PHB 5.1 wealth-by-class purse (display only — the backend is authoritative). */
export function startingGoldClassBudget(classIndex) {
  switch (classIndex) {
    case 'barbarian':
    case 'druid':
      return 50
    case 'monk':
      return 12
    case 'sorcerer':
      return 75
    case 'rogue':
    case 'warlock':
    case 'wizard':
      return 100
    case 'bard':
    case 'cleric':
    case 'fighter':
    case 'paladin':
    case 'ranger':
      return 125
    default:
      return 100
  }
}

/**
 * SRD list prices (in gp) for the starter kit a level-1 adventurer actually
 * buys, taken from dnd5eapi's equipment `cost`. This is a display bootstrap
 * only: the wizard prefers the live SRD price and the backend always validates
 * against the live cost on compile, so the running total matches.
 */
export const STARTER_EQUIPMENT_PRICES = {
  backpack: 2,
  battleaxe: 10,
  bedroll: 1,
  'burglars-pack': 16,
  candle: 0.01,
  'chain-mail': 75,
  'chain-shirt': 50,
  'clothes-common': 0.5,
  'clothes-fine': 15,
  'clothes-travelers': 2,
  club: 0.1,
  'component-pouch': 25,
  dart: 0.05,
  'diplomats-pack': 39,
  'dungeoneers-pack': 12,
  'entertainers-pack': 40,
  'explorers-pack': 10,
  flail: 10,
  glaive: 20,
  greataxe: 30,
  greatclub: 0.2,
  greatsword: 50,
  handaxe: 5,
  'healers-kit': 5,
  'hide-armor': 10,
  javelin: 0.5,
  'leather-armor': 10,
  'light-hammer': 2,
  longbow: 50,
  longsword: 15,
  mace: 5,
  maul: 10,
  morningstar: 15,
  'padded-armor': 5,
  pike: 5,
  piton: 0.05,
  'plate-armor': 1500,
  pouch: 0.5,
  'priests-pack': 19,
  quarterstaff: 0.2,
  quiver: 1,
  rapier: 25,
  reliquary: 5,
  'ring-mail': 30,
  'rope-silk-50-ft': 10,
  sack: 0.01,
  'scale-mail': 50,
  'scholars-pack': 40,
  scimitar: 25,
  shield: 10,
  shortbow: 25,
  shortsword: 10,
  sickle: 1,
  sling: 0.1,
  spear: 1,
  'splint-armor': 200,
  'studded-leather-armor': 45,
  trident: 5,
  'war-pick': 5,
  warhammer: 15,
  waterskin: 0.2,
  whip: 2,
}

export function equipmentPriceGp(index) {
  const price = STARTER_EQUIPMENT_PRICES[index]
  return typeof price === 'number' ? price : null
}

/**
 * Server-parity conversion of a live SRD equipment `cost` ({quantity, unit})
 * into whole gold pieces. Mirrors ChargenRules.equipmentGoldCostGp so the
 * wizard's running total equals what `compile` actually charges (sub-gp prices
 * truncate to 0 gp).
 */
export function equipmentCostGp(cost) {
  const quantity = cost?.quantity ?? 0
  const copper =
    cost?.unit === 'pp' ? quantity * 1000
      : cost?.unit === 'sp' ? quantity * 10
        : cost?.unit === 'cp' ? quantity
          : quantity * 100
  return Math.floor(copper / 100)
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