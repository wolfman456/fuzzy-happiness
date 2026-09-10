import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CharactersPage from './CharactersPage'

vi.mock('../lib/characters', () => ({
  listMyCharacters: vi.fn(),
  generateCharacter: vi.fn(),
  createCharacter: vi.fn(),
  sheetToDraft: vi.fn(),
}))

import { createCharacter, generateCharacter, listMyCharacters, sheetToDraft } from '../lib/characters'

beforeEach(() => {
  vi.clearAllMocks()
})

function sheet(overrides = {}) {
  return {
    name: 'Tordek',
    level: 1,
    scoreSource: 'STANDARD_ARRAY',
    raceIndex: 'dwarf',
    classIndex: 'cleric',
    subclassIndex: 'life',
    backgroundIndex: 'acolyte',
    strength: 15,
    dexterity: 14,
    constitution: 13,
    intelligence: 12,
    wisdom: 10,
    charisma: 8,
    hitPoints: 9,
    armorClass: 16,
    skillPicks: ['medicine', 'religion'],
    spellIndexes: ['sacred-flame'],
    equipmentIndexes: ['leather-armor', 'shield'],
    ...overrides,
  }
}

function renderPage() {
  return render(
    <MemoryRouter>
      <CharactersPage />
    </MemoryRouter>,
  )
}

describe('CharactersPage', () => {
  it('shows an empty state when there are no characters', async () => {
    listMyCharacters.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText(/No characters yet/)).toBeInTheDocument()
  })

  it('lists the user’s characters', async () => {
    listMyCharacters.mockResolvedValue([
      { id: 7, name: 'Tordek', level: 2, raceIndex: 'dwarf', classIndex: 'cleric', subclassIndex: 'life', backgroundIndex: 'acolyte', hitPoints: 14, armorClass: 16 },
    ])
    renderPage()

    expect(await screen.findByText('Tordek')).toBeInTheDocument()
    expect(screen.getByText(/dwarf cleric \(life\)/)).toHaveTextContent('life')
    expect(screen.getByText(/HP 14 · AC 16/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Tordek/ })).toHaveAttribute('href', '/characters/7')
  })

  it('shows a load error and still offers the wizard', async () => {
    listMyCharacters.mockRejectedValue(new Error('boom'))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load your characters')
    expect(screen.getByRole('link', { name: 'Guided wizard' })).toBeInTheDocument()
  })

  it('quick-builds a legal sheet and saves it', async () => {
    listMyCharacters.mockResolvedValue([])
    generateCharacter.mockResolvedValue({ valid: true, violations: [], sheet: sheet() })
    const rebuilt = { name: 'Tordek', raceIndex: 'dwarf', spellIndexes: ['sacred-flame'] }
    sheetToDraft.mockReturnValue(rebuilt)
    createCharacter.mockResolvedValue({ id: 1 })
    renderPage()

    await screen.findByText(/No characters yet/)
    fireEvent.click(screen.getByRole('button', { name: 'Surprise me' }))

    expect(await screen.findByText(/Quick build: Tordek/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save this character' }))

    await waitFor(() => expect(sheetToDraft).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(createCharacter).toHaveBeenCalledWith(rebuilt))
    await waitFor(() => expect(listMyCharacters).toHaveBeenCalledTimes(2))
  })

  it('surfaces violations when the quick-build is not legal', async () => {
    listMyCharacters.mockResolvedValue([])
    generateCharacter.mockResolvedValue({ valid: false, violations: ['unknown race: orc'] })
    renderPage()

    await screen.findByText(/No characters yet/)
    fireEvent.click(screen.getByRole('button', { name: 'Surprise me' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/unknown race: orc/)
    expect(screen.queryByText(/Quick build/)).not.toBeInTheDocument()
  })
})