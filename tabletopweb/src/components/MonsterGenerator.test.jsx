import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MonsterGenerator from './MonsterGenerator'

const monstersMock = vi.hoisted(() => ({
  generateMonster: vi.fn(),
  listMyMonsters: vi.fn(),
  MONSTER_CRS: ['1', '2', '3', '4'],
  MONSTER_ROLES: ['AUTO', 'BALANCED', 'BRUTE', 'ARTILLERY', 'LURKER'],
  MONSTER_EDITIONS: ['SRD_2014', 'SRD_2024'],
}))

const battleMapMock = vi.hoisted(() => ({
  addToken: vi.fn(),
}))

vi.mock('../lib/battleMap', () => battleMapMock)

vi.mock('../lib/monsters', () => monstersMock)

function makeMap() {
  return {
    id: 1,
    sessionId: 7,
    name: 'Test map',
    width: 24,
    height: 18,
    squareFeet: 10,
    currentTurnTokenId: null,
    tokens: [
      { id: 11, name: 'Aria Sol', category: 'PLAYER', color: '#3b82f6', speedFeet: 30, posX: 1, posY: 1, movedFeet: 0, linkedParticipantId: null, linkedUserId: 1 },
      { id: 12, name: 'Goblin', category: 'MONSTER_NPC', color: '#ef4444', speedFeet: 30, posX: 5, posY: 5, movedFeet: 0, linkedParticipantId: null, linkedUserId: null },
    ],
  }
}

function makeMonster(overrides = {}) {
  return {
    id: 1,
    name: 'Gravetusk',
    cr: '7',
    xp: 2900,
    edition: 'SRD_2014',
    role: 'BRUTE',
    proficiencyBonus: 3,
    armorClass: 14,
    hitPoints: 219,
    size: 'Large',
    type: 'giant',
    alignment: 'unaligned',
    speedFeet: 30,
    strength: 16,
    dexterity: 8,
    constitution: 16,
    intelligence: 5,
    wisdom: 8,
    charisma: 6,
    attackBonus: 7,
    saveDc: 15,
    damagePerRound: 30,
    description: 'A brute of challenge rating 7.',
    actions: [{ name: 'Slam', description: 'Smash the target.' }],
    traits: ['Hardy Frame.'],
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function renderGenerator(props = {}) {
  const onMapChange = vi.fn()
  render(
    <MonsterGenerator
      sessionId="7"
      map={makeMap()}
      disabled={false}
      onMapChange={onMapChange}
      {...props}
    />,
  )
  return { onMapChange }
}

beforeEach(() => {
  vi.clearAllMocks()
  monstersMock.listMyMonsters.mockResolvedValue([])
  monstersMock.generateMonster.mockResolvedValue(makeMonster())
  battleMapMock.addToken.mockResolvedValue(makeMap())
})

describe('MonsterGenerator', () => {
  it('renders the generator form and loads my monsters', async () => {
    const saved = makeMonster({ id: 2, name: 'Vinebomb', cr: '3', role: 'ARTILLERY', hitPoints: 103 })
    monstersMock.listMyMonsters.mockResolvedValue([saved])
    renderGenerator()

    expect(await screen.findByRole('heading', { name: 'Monster generator' })).toBeInTheDocument()
    expect(screen.getByLabelText('Challenge rating')).toHaveValue('1')
    expect(screen.getByLabelText('Combat role')).toHaveValue('AUTO')
    expect((await screen.findByTestId('monster-list')).textContent).toContain('Vinebomb')
  })

  it('generates a statblock and renders it', async () => {
    renderGenerator()

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Gravetusk' } })
    fireEvent.change(screen.getByLabelText('Concept'), { target: { value: 'a lurking horror' } })
    fireEvent.click(screen.getByRole('button', { name: 'Generate monster' }))

    expect(await screen.findByTestId('monster-statblock')).toHaveTextContent('Gravetusk')
    expect(monstersMock.generateMonster).toHaveBeenCalledWith({
      cr: '1',
      role: 'AUTO',
      edition: 'SRD_2014',
      name: 'Gravetusk',
      concept: 'a lurking horror',
    })
    await waitFor(() =>
      expect(monstersMock.listMyMonsters).toHaveBeenCalled(),
    )
  })

  it('omits empty name and concept from the request', async () => {
    renderGenerator()

    fireEvent.click(screen.getByRole('button', { name: 'Generate monster' }))

    expect(await screen.findByTestId('monster-statblock')).toBeInTheDocument()
    expect(monstersMock.generateMonster).toHaveBeenCalledWith({
      cr: '1',
      role: 'AUTO',
      edition: 'SRD_2014',
    })
  })

  it('surfaces generation errors', async () => {
    monstersMock.generateMonster.mockRejectedValue(new Error('unsupported challenge rating: 47'))
    renderGenerator()

    fireEvent.click(screen.getByRole('button', { name: 'Generate monster' }))

    expect(await screen.findByTestId('monster-error')).toHaveTextContent('unsupported challenge rating: 47')
  })

  it('adds a generated monster token at the first free square', async () => {
    renderGenerator()
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Gravetusk' } })
    fireEvent.click(screen.getByRole('button', { name: 'Generate monster' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Add to map' }))

    await waitFor(() =>
      expect(battleMapMock.addToken).toHaveBeenCalledWith('7', {
        name: 'Gravetusk',
        category: 'MONSTER_NPC',
        speedFeet: 30,
        color: '#f97316',
        posX: 0,
        posY: 0,
      }),
    )
    expect((await screen.findByRole('status')).textContent).toMatch(/Gravetusk added/)
  })

  it('adds a saved monster to the map from the list', async () => {
    const saved = makeMonster({ id: 2, name: 'Vinebomb' })
    monstersMock.listMyMonsters.mockResolvedValue([saved])
    renderGenerator()

    const addButton = await screen.findByRole('button', { name: 'Add to map' })
    fireEvent.click(addButton)

    await waitFor(() =>
      expect(battleMapMock.addToken).toHaveBeenCalledWith('7', {
        name: 'Vinebomb',
        category: 'MONSTER_NPC',
        speedFeet: 30,
        color: '#eab308',
        posX: 0,
        posY: 0,
      }),
    )
  })

  it('warns the GM when no battle map exists yet', async () => {
    renderGenerator({ map: null })
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Gravetusk' } })
    fireEvent.click(screen.getByRole('button', { name: 'Generate monster' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Add to map' }))

    expect(await screen.findByTestId('monster-error')).toHaveTextContent('Create a battle map first')
    expect(battleMapMock.addToken).not.toHaveBeenCalled()
  })
})