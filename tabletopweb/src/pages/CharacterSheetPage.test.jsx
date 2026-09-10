import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import CharacterSheetPage from './CharacterSheetPage'

vi.mock('../lib/characters', () => ({
  ABILITIES: ['strength', 'dexterity', 'constitution', 'intelligence', 'wisdom', 'charisma'],
  abilityModifier: (score) => Math.floor((score - 10) / 2),
  getCharacter: vi.fn(),
}))

import { getCharacter } from '../lib/characters'

const SHEET = {
  id: 7,
  name: 'Tordek',
  level: 3,
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
  proficiencyBonus: 2,
  hitPoints: 22,
  armorClass: 16,
  speedFeet: 25,
  savingThrows: ['wis', 'cha'],
  classSkills: ['medicine', 'religion'],
  backgroundSkills: [],
  skillPicks: ['medicine', 'religion'],
  spellIndexes: ['sacred-flame', 'bless'],
  equipmentIndexes: ['leather-armor', 'shield'],
}

function renderSheet() {
  return render(
    <MemoryRouter initialEntries={['/characters/7']}>
      <Routes>
        <Route path="/characters/:id" element={<CharacterSheetPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('CharacterSheetPage', () => {
  it('renders the sheet identity, stats, scores and lists', async () => {
    getCharacter.mockResolvedValue(SHEET)
    renderSheet()

    expect(await screen.findByRole('heading', { name: 'Tordek' })).toBeInTheDocument()
    expect(screen.getByText(/Level 3 · dwarf cleric \(life\) · acolyte/)).toBeInTheDocument()
    expect(screen.getByText('22')).toBeInTheDocument()
    expect(screen.getByText('16')).toBeInTheDocument()
    expect(screen.getByText('25 ft')).toBeInTheDocument()
    expect(screen.getByText('medicine')).toBeInTheDocument()
    expect(screen.getByText('religion')).toBeInTheDocument()
    expect(screen.getByText('sacred-flame')).toBeInTheDocument()
    expect(screen.getByText('leather-armor')).toBeInTheDocument()
    expect(screen.getByText('shield')).toBeInTheDocument()
  })

  it('renders empty-state lists for a bare sheet', async () => {
    getCharacter.mockResolvedValue({ ...SHEET, skillPicks: [], spellIndexes: [], equipmentIndexes: [] })
    renderSheet()

    expect(await screen.findByRole('heading', { name: 'Tordek' })).toBeInTheDocument()
    expect(screen.getAllByText('None')).toHaveLength(3)
  })

  it('shows an error when the character cannot be loaded', async () => {
    getCharacter.mockRejectedValue(new Error('nope'))
    renderSheet()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load this character sheet')
  })
})