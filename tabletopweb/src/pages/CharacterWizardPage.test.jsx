import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CharacterWizardPage from './CharacterWizardPage'

vi.mock('../lib/srd', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, srdList: vi.fn(), srdDetail: vi.fn(), srdSubresource: vi.fn() }
})

vi.mock('../lib/characters', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, compileCharacter: vi.fn(), createCharacter: vi.fn(), rollScores: vi.fn() }
})

import { createCharacter, compileCharacter, rollScores } from '../lib/characters'
import { srdDetail, srdList, srdSubresource } from '../lib/srd'

beforeEach(() => {
  vi.clearAllMocks()
})

const CLERIC = {
  index: 'cleric',
  name: 'Cleric',
  hit_die: 8,
  proficiency_choices: [
    {
      choose: 2,
      from: {
        options: [
          { item: { index: 'skill-history', name: 'History' } },
          { item: { index: 'skill-insight', name: 'Insight' } },
          { item: { index: 'skill-medicine', name: 'Medicine' } },
          { item: { index: 'skill-persuasion', name: 'Persuasion' } },
          { item: { index: 'skill-religion', name: 'Religion' } },
        ],
      },
    },
  ],
  saving_throws: [{ index: 'wis' }, { index: 'cha' }],
  subclasses: [{ index: 'life', name: 'Life' }],
  subclass_level: 1,
  spellcasting: { level: 1, spellcasting_ability: { index: 'wis' }, info: [] },
}

const LEVELS = [
  {
    level: 1,
    prof_bonus: 2,
    spellcasting: {
      cantrips_known: 3,
      spell_slots_level_1: 2,
      spell_slots_level_2: 0,
      spell_slots_level_3: 0,
      spell_slots_level_4: 0,
      spell_slots_level_5: 0,
      spell_slots_level_6: 0,
      spell_slots_level_7: 0,
      spell_slots_level_8: 0,
      spell_slots_level_9: 0,
    },
  },
  { level: 2, prof_bonus: 2, spellcasting: { cantrips_known: 3, spell_slots_level_1: 3 } },
]

const SPELLS = {
  count: 2,
  results: [
    { index: 'sacred-flame', name: 'Sacred Flame', level: 0 },
    { index: 'bless', name: 'Bless', level: 1 },
  ],
}

function setupSrd() {
  srdList.mockImplementation(async (collection) => {
    const byCollection = {
      races: [{ index: 'dwarf', name: 'Dwarf' }],
      classes: [{ index: 'cleric', name: 'Cleric' }],
      backgrounds: [{ index: 'acolyte', name: 'Acolyte' }],
      skills: [
        { index: 'acrobatics', name: 'Acrobatics' },
        { index: 'history', name: 'History' },
        { index: 'insight', name: 'Insight' },
        { index: 'medicine', name: 'Medicine' },
        { index: 'persuasion', name: 'Persuasion' },
        { index: 'religion', name: 'Religion' },
        { index: 'stealth', name: 'Stealth' },
      ],
      equipment: [
        { index: 'leather-armor', name: 'Leather Armor' },
        { index: 'shield', name: 'Shield' },
        { index: 'plate-armor', name: 'Plate Armor' },
      ],
    }
    return { results: byCollection[collection] ?? [] }
  })
  srdDetail.mockImplementation(async (collection, index) => {
    if (collection === 'classes' && index === 'cleric') return CLERIC
    return null
  })
  srdSubresource.mockImplementation(async (collection, index, subresource) => {
    if (collection === 'classes' && index === 'cleric' && subresource === 'levels') return LEVELS
    if (collection === 'classes' && index === 'cleric' && subresource === 'spells') return SPELLS
    return null
  })
}

const VALID_SHEET = {
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
}

function SheetProbe() {
  const { id } = useParams()
  return <span data-testid="location">/characters/{id}</span>
}

function renderWizard(initialState) {
  const entries = initialState ? [{ pathname: '/characters/new', state: { draft: initialState } }] : ['/characters/new']
  return render(
    <MemoryRouter initialEntries={entries}>
      <Routes>
        <Route path="/characters/new" element={<CharacterWizardPage />} />
        <Route path="/characters/:id" element={<SheetProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

async function walkToReview() {
  fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('button', { name: 'Use the standard array (as written)' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('button', { name: 'Dwarf' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('button', { name: 'Cleric' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled())
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('button', { name: 'Life' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('button', { name: 'Acolyte' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('checkbox', { name: 'Medicine' }))
  fireEvent.click(screen.getByRole('checkbox', { name: 'Religion' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('checkbox', { name: 'Sacred Flame' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('checkbox', { name: /Leather Armor/ }))
  fireEvent.click(screen.getByRole('checkbox', { name: /Shield/ }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  await screen.findByText('Review your choices')
}

describe('CharacterWizardPage', () => {
  it('prefills from a quick-build draft passed via location state', async () => {
    setupSrd()
    renderWizard({ name: 'Zog', classIndex: 'cleric' })

    const name = await screen.findByLabelText('Character name')
    expect(name).toHaveValue('Zog')
  })

  it('forces a server roll for rolled score methods before continuing', async () => {
    setupSrd()
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.change(screen.getByLabelText('Ability scores'), { target: { value: 'FOUR_D6_DROP_LOWEST' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    const rollButton = await screen.findByRole('button', { name: 'Roll ability scores' })
    expect(rollButton).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getAllByRole('spinbutton')[0]).toBeDisabled()
    expect(rollScores).not.toHaveBeenCalled()
  })

  it('applies the server roll to the scores and unlocks the next step', async () => {
    setupSrd()
    rollScores.mockResolvedValue({
      scoreSource: 'FOUR_D6_DROP_LOWEST',
      strength: 15,
      dexterity: 14,
      constitution: 13,
      intelligence: 12,
      wisdom: 10,
      charisma: 8,
    })
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.change(screen.getByLabelText('Ability scores'), { target: { value: 'FOUR_D6_DROP_LOWEST' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Roll ability scores' }))

    await waitFor(() => expect(rollScores).toHaveBeenCalledWith({ scoreSource: 'FOUR_D6_DROP_LOWEST' }))
    expect(await screen.findByRole('button', { name: 'Roll again' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
    expect(screen.getByDisplayValue('15')).toBeInTheDocument()
  })

  it('blocks continuing when the class starting-gold budget is blown', async () => {
    setupSrd()
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Use the standard array (as written)' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Dwarf' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Cleric' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled())
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Life' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Acolyte' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('checkbox', { name: 'Medicine' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Religion' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('checkbox', { name: 'Sacred Flame' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    const plate = await screen.findByRole('checkbox', { name: /Plate Armor/ })
    fireEvent.click(plate)
    expect(screen.getByRole('alert')).toHaveTextContent(/more than your class's starting gold/)
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('guides through all steps, compiles a legal sheet and creates the character', async () => {
    setupSrd()
    compileCharacter.mockResolvedValue({ valid: true, violations: [], sheet: VALID_SHEET })
    createCharacter.mockResolvedValue({ id: 42 })
    renderWizard()

    await walkToReview()
    fireEvent.click(screen.getByRole('button', { name: 'Compile sheet' }))

    await waitFor(() => expect(compileCharacter).toHaveBeenCalledWith(expect.objectContaining({ name: 'Tordek' })))
    expect(await screen.findByText('This sheet is legal.')).toBeInTheDocument()
    expect(screen.getByText(/HP 9 · AC 16/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Create character' }))
    await waitFor(() =>
      expect(createCharacter).toHaveBeenCalledWith(expect.objectContaining({ name: 'Tordek', classIndex: 'cleric' })),
    )
    expect(await screen.findByTestId('location')).toHaveTextContent('/characters/42')
  })

  it('lists violations instead of a create button when the sheet is illegal', async () => {
    setupSrd()
    compileCharacter.mockResolvedValue({ valid: false, violations: ['unknown score source: CHEAT'], sheet: null })
    renderWizard()

    await walkToReview()
    fireEvent.click(screen.getByRole('button', { name: 'Compile sheet' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/unknown score source: CHEAT/)
    expect(screen.queryByRole('button', { name: 'Create character' })).not.toBeInTheDocument()
  })
})