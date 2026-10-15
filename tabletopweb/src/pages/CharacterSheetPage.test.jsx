import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CharacterSheetPage from './CharacterSheetPage'
import {
  avatarSnapshotKey,
  clearAvatarSnapshots,
  setAvatarSnapshot,
} from '../lib/avatarSnapshot'

vi.mock('../lib/characters', () => ({
  ABILITIES: ['strength', 'dexterity', 'constitution', 'intelligence', 'wisdom', 'charisma'],
  DEFAULT_APPEARANCE: { skinTone: 200, hairTone: 30 },
  abilityModifier: (score) => Math.floor((score - 10) / 2),
  getCharacter: vi.fn(),
  deleteCharacter: vi.fn(),
}))

import { deleteCharacter, getCharacter } from '../lib/characters'

afterEach(() => {
  clearAvatarSnapshots()
})

const SHEET = {
  id: 7,
  name: 'Tordek',
  level: 3,
  scoreSource: 'STANDARD_ARRAY',
  sex: 'FEMALE',
  appearance: { skinTone: 120, hairTone: 300 },
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
  featureIndexes: ['spellcasting', 'disciple-of-life', 'darkvision'],
  equipmentIndexes: ['leather-armor', 'shield'],
}

function renderSheet() {
  return render(
    <MemoryRouter initialEntries={['/characters/7']}>
      <Routes>
        <Route path="/characters/:id" element={<CharacterSheetPage />} />
        <Route path="/characters" element={<div>characters list</div>} />
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
    expect(screen.getByText('Female')).toBeInTheDocument()
    expect(screen.getByTestId('character-avatar')).toBeInTheDocument()
    expect(screen.getByText('22')).toBeInTheDocument()
    expect(screen.getByText('16')).toBeInTheDocument()
    expect(screen.getByText('25 ft')).toBeInTheDocument()
    expect(screen.getByText('medicine')).toBeInTheDocument()
    expect(screen.getByText('religion')).toBeInTheDocument()
    expect(screen.getByText('sacred-flame')).toBeInTheDocument()
    expect(screen.getByText('disciple-of-life')).toBeInTheDocument()
    expect(screen.getByText('leather-armor')).toBeInTheDocument()
    expect(screen.getByText('shield')).toBeInTheDocument()
  })

  it('shows a fallback label for a legacy sheet without a sex', async () => {
    getCharacter.mockResolvedValue({ ...SHEET, sex: null, appearance: null })
    renderSheet()

    expect(await screen.findByRole('heading', { name: 'Tordek' })).toBeInTheDocument()
    expect(screen.getByText('Sex not set')).toBeInTheDocument()
    expect(screen.getByTestId('character-avatar')).toBeInTheDocument()
  })

  it('renders empty-state lists for a bare sheet', async () => {
    getCharacter.mockResolvedValue({
      ...SHEET,
      skillPicks: [],
      spellIndexes: [],
      featureIndexes: [],
      equipmentIndexes: [],
    })
    renderSheet()

    expect(await screen.findByRole('heading', { name: 'Tordek' })).toBeInTheDocument()
    expect(screen.getAllByText('None')).toHaveLength(4)
  })

  it('renders the cached PNG portrait when a snapshot exists', async () => {
    setAvatarSnapshot(
      avatarSnapshotKey(SHEET.raceIndex, SHEET.sex, SHEET.appearance),
      'data:image/png;base64,SHEET',
    )
    getCharacter.mockResolvedValue(SHEET)
    const { container } = renderSheet()

    expect(await screen.findByRole('heading', { name: 'Tordek' })).toBeInTheDocument()
    const img = container.querySelector('img[data-testid="character-avatar"]')
    expect(img).toBeTruthy()
    expect(img.getAttribute('src')).toBe('data:image/png;base64,SHEET')
  })

  it('shows an error when the character cannot be loaded', async () => {
    getCharacter.mockRejectedValue(new Error('nope'))
    renderSheet()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load this character sheet')
  })

  it('deletes the character from the sheet after a confirm dialog', async () => {
    getCharacter.mockResolvedValue(SHEET)
    deleteCharacter.mockResolvedValue(undefined)
    renderSheet()

    fireEvent.click(await screen.findByRole('button', { name: 'Delete character' }))
    const confirm = await screen.findByRole('dialog')
    expect(within(confirm).getByText(/Delete Tordek\?/)).toBeInTheDocument()

    fireEvent.click(within(confirm).getByRole('button', { name: 'Delete' }))
    await waitFor(() => expect(deleteCharacter).toHaveBeenCalledWith('7'))
    expect(await screen.findByText('characters list')).toBeInTheDocument()
  })
})