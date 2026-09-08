import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./api', () => ({ api: vi.fn() }))

import { api } from './api'
import {
  addToken,
  createMap,
  getMap,
  moveToken,
  nextInitiative,
  removeInitiativeEntry,
  removeToken,
  rerollInitiative,
  rollDice,
  setInitiative,
  turnCommand,
  updateMap,
  updateToken,
} from './battleMap'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('battleMap API helpers', () => {
  it('getMap fetches the session map', () => {
    getMap(7)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map')
  })

  it('createMap POSTs a map', () => {
    createMap(7)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map', { method: 'POST', body: {} })
  })

  it('updateMap PATCHes a map', () => {
    updateMap(7, { width: 12, height: 10 })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map', {
      method: 'PATCH',
      body: { width: 12, height: 10 },
    })
  })

  it('addToken POSTs a token', () => {
    addToken(7, { name: 'Goblin', speedFeet: 30 })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/tokens', {
      method: 'POST',
      body: { name: 'Goblin', speedFeet: 30 },
    })
  })

  it('updateToken PATCHes a token', () => {
    updateToken(7, 11, { name: 'Orc' })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/tokens/11', {
      method: 'PATCH',
      body: { name: 'Orc' },
    })
  })

  it('removeToken DELETEs a token', () => {
    removeToken(7, 11)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/tokens/11', { method: 'DELETE' })
  })

  it('moveToken POSTs a move', () => {
    moveToken(7, 11, { x: 3, y: 4 })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/tokens/11/move', {
      method: 'POST',
      body: { x: 3, y: 4 },
    })
  })

  it('turnCommand POSTs a turn action', () => {
    turnCommand(7, { action: 'NEW_ROUND' })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/turn', {
      method: 'POST',
      body: { action: 'NEW_ROUND' },
    })
  })

  it('setInitiative POSTs an ordered list', () => {
    setInitiative(7, { entries: [{ label: 'Goblin', score: 14 }] })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/initiative', {
      method: 'POST',
      body: { entries: [{ label: 'Goblin', score: 14 }] },
    })
  })

  it('rerollInitiative POSTs to an entry', () => {
    rerollInitiative(7, 42)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/initiative/42/reroll', {
      method: 'POST',
    })
  })

  it('nextInitiative advances the order', () => {
    nextInitiative(7)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/initiative/next', {
      method: 'POST',
    })
  })

  it('removeInitiativeEntry DELETEs an entry', () => {
    removeInitiativeEntry(7, 42)
    expect(api).toHaveBeenCalledWith('/api/sessions/7/map/initiative/42', {
      method: 'DELETE',
    })
  })

  it('rollDice POSTs a roll', () => {
    rollDice(7, { expression: '2d6+3', label: 'Perception', privateRoll: true })
    expect(api).toHaveBeenCalledWith('/api/sessions/7/roll', {
      method: 'POST',
      body: { expression: '2d6+3', label: 'Perception', privateRoll: true },
    })
  })
})