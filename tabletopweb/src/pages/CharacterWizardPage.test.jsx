import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
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
  vi.useRealTimers()
})

const DWARF = {
  index: 'dwarf',
  name: 'Dwarf',
  ability_bonuses: [{ ability_score: { index: 'con' }, bonus: 2 }],
}

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
  starting_equipment: [
    { equipment: { index: 'plate-armor', name: 'Plate Armor' } },
    { equipment: { index: 'shield', name: 'Shield' } },
  ],
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

const EQUIPMENT_COSTS = {
  'leather-armor': { quantity: 10, unit: 'gp' },
  shield: { quantity: 10, unit: 'gp' },
  'plate-armor': { quantity: 1500, unit: 'gp' },
  spellbook: { quantity: 50, unit: 'gp' },
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
        { index: 'spellbook', name: 'Spellbook' },
      ],
    }
    return { results: byCollection[collection] ?? [] }
  })
  srdDetail.mockImplementation(async (collection, index) => {
    if (collection === 'classes' && index === 'cleric') return CLERIC
    if (collection === 'races' && index === 'dwarf') return DWARF
    if (collection === 'equipment' && EQUIPMENT_COSTS[index]) {
      return { index, cost: EQUIPMENT_COSTS[index] }
    }
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

function chooseStandardArray() {
  fireEvent.change(screen.getByLabelText('Ability scores'), { target: { value: 'STANDARD_ARRAY' } })
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))
}

async function walkToEquipment() {
  fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
  chooseStandardArray()

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

  fireEvent.click(screen.getByRole('checkbox', { name: 'Medicine' }))
  fireEvent.click(screen.getByRole('checkbox', { name: 'Religion' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  fireEvent.click(await screen.findByRole('checkbox', { name: 'Sacred Flame' }))
  fireEvent.click(screen.getByRole('button', { name: 'Next' }))

  await screen.findByRole('checkbox', { name: /Leather Armor/ })
}

async function walkToReview() {
  await walkToEquipment()
  fireEvent.click(screen.getByRole('checkbox', { name: /Leather Armor/ }))
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

  it('defaults to rolling dice with no arrow steppers', async () => {
    setupSrd()
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('button', { name: 'Roll all ability scores' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Roll Strength' })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^Roll / })).toHaveLength(7)
    expect(screen.queryAllByRole('spinbutton')).toHaveLength(0)
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(rollScores).not.toHaveBeenCalled()
  })

  it('animates then finalizes a single ability from the server roll', async () => {
    setupSrd()
    rollScores.mockResolvedValue({
      scoreSource: 'FOUR_D6_DROP_LOWEST',
      strength: 16,
      dexterity: 11,
      constitution: 9,
      intelligence: 14,
      wisdom: 12,
      charisma: 7,
    })
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    vi.useFakeTimers()
    fireEvent.click(screen.getByRole('button', { name: 'Roll Strength' }))
    expect(rollScores).toHaveBeenCalledWith({ scoreSource: 'FOUR_D6_DROP_LOWEST' })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000)
    })

    expect(screen.getByTestId('score-strength')).toHaveTextContent('16')
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    vi.useRealTimers()
  })

  it('rolls all six abilities and unlocks the next step', async () => {
    setupSrd()
    rollScores.mockResolvedValue({
      scoreSource: 'FOUR_D6_DROP_LOWEST',
      strength: 16,
      dexterity: 11,
      constitution: 9,
      intelligence: 14,
      wisdom: 12,
      charisma: 7,
    })
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    vi.useFakeTimers()
    fireEvent.click(screen.getByRole('button', { name: 'Roll all ability scores' }))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000)
    })

    expect(screen.getByTestId('score-strength')).toHaveTextContent('16')
    expect(screen.getByTestId('score-charisma')).toHaveTextContent('7')
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
    vi.useRealTimers()
  })

  it('assigns the standard array from the preset button with dropdowns', async () => {
    setupSrd()
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    chooseStandardArray()

    expect(screen.queryAllByRole('spinbutton')).toHaveLength(0)
    fireEvent.click(await screen.findByRole('button', { name: 'Use the standard array (as written)' }))
    expect(screen.getByLabelText('Strength score')).toHaveValue('15')
    expect(screen.getByLabelText('Charisma score')).toHaveValue('8')
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('tracks point-buy spend as scores are assigned', async () => {
    setupSrd()
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    fireEvent.change(screen.getByLabelText('Ability scores'), { target: { value: 'POINT_BUY' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    fireEvent.change(screen.getByLabelText('Strength score'), { target: { value: '15' } })
    expect(screen.getByText(/Points used: 9 \/ 27/)).toBeInTheDocument()
  })

  it('prices selected gear from the live SRD so the total matches compile', async () => {
    setupSrd()
    renderWizard()

    await walkToEquipment()

    // Spellbook is not in the static price map — the live SRD price must drive the total.
    fireEvent.click(await screen.findByRole('checkbox', { name: /Spellbook/ }))
    await waitFor(() =>
      expect(screen.getAllByText((_, node) => node?.textContent?.includes('Spent 50 gp')).length).toBeGreaterThan(0),
    )
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('blocks the shop while gear has no live price and offers a retry', async () => {
    setupSrd()
    srdDetail.mockImplementation(async (collection, index) => {
      if (collection === 'classes' && index === 'cleric') return CLERIC
      if (collection === 'races' && index === 'dwarf') return DWARF
      return null
    })
    renderWizard()

    await walkToEquipment()

    fireEvent.click(await screen.findByRole('checkbox', { name: /Spellbook/ }))
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/not priced yet/))
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Retry pricing' })).toBeInTheDocument()
  })

  it("trims the class's recommended kit to fit the starting budget", async () => {
    setupSrd()
    renderWizard()

    await walkToEquipment()

    // Kit is plate-armor (1500) + shield (10); only the shield can fit a 125 gp budget.
    fireEvent.click(await screen.findByRole('button', { name: "Take your class's recommended kit" }))
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Shield/ })).toBeChecked())
    expect(screen.getByRole('checkbox', { name: /Plate Armor/ })).not.toBeChecked()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('restores a quick-build draft without double-applying the race bonus', async () => {
    setupSrd()
    renderWizard({
      name: 'Zog',
      scoreSource: 'STANDARD_ARRAY',
      raceIndex: 'dwarf',
      classIndex: 'cleric',
      strength: 15,
      dexterity: 14,
      constitution: 15,
      intelligence: 12,
      wisdom: 10,
      charisma: 8,
    })

    fireEvent.click(await screen.findByRole('button', { name: 'Next' }))
    await waitFor(() => expect(screen.getByLabelText('Constitution score')).toHaveValue('13'))
    expect(screen.getByLabelText('Strength score')).toHaveValue('15')
  })

  it('blocks and offers a retry when race bonuses cannot be loaded', async () => {
    setupSrd()
    srdDetail.mockImplementation(async (collection, index) => {
      if (collection === 'races') throw new Error('races down')
      if (collection === 'classes' && index === 'cleric') return CLERIC
      return null
    })
    renderWizard()

    fireEvent.change(screen.getByLabelText('Character name'), { target: { value: 'Tordek' } })
    chooseStandardArray()
    fireEvent.click(await screen.findByRole('button', { name: 'Use the standard array (as written)' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Dwarf' }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Could not load ability bonuses/),
    )
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()

    srdDetail.mockImplementation(async (collection, index) => {
      if (collection === 'races' && index === 'dwarf') return DWARF
      if (collection === 'classes' && index === 'cleric') return CLERIC
      return null
    })
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled())
  })

  it('blocks continuing when the class starting-gold budget is blown', async () => {
    setupSrd()
    renderWizard()

    await walkToEquipment()

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
