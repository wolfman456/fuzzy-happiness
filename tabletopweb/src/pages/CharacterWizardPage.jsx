import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  ABILITIES,
  SCORE_SOURCES,
  SCORE_SOURCE_LABELS,
  abilityModifier,
  compileCharacter,
  createCharacter,
  equipmentPriceGp,
  pointBuyCost,
  rollScores,
  startingGoldClassBudget,
  validateBaseScores,
} from '../lib/characters'
import {
  offeredClassSkills,
  srdDetail,
  srdList,
  srdRows,
  srdSubresource,
  subclassLevel,
} from '../lib/srd'

const STEPS = [
  'Basics',
  'Ability scores',
  'Race',
  'Class',
  'Subclass',
  'Background',
  'Skills',
  'Spells',
  'Equipment',
  'Review',
]

const ABILITY_LABELS = {
  strength: 'Strength',
  dexterity: 'Dexterity',
  constitution: 'Constitution',
  intelligence: 'Intelligence',
  wisdom: 'Wisdom',
  charisma: 'Charisma',
}

const ROLLED_SOURCES = new Set(['FOUR_D6_DROP_LOWEST', 'HOUSE_RULE_D20'])

function plainSkill(index) {
  return index.startsWith('skill-') ? index.slice('skill-'.length) : index
}

function initialDraft() {
  return {
    name: '',
    strength: 10,
    dexterity: 10,
    constitution: 10,
    intelligence: 10,
    wisdom: 10,
    charisma: 10,
    scoreSource: 'STANDARD_ARRAY',
    startingLevel: 1,
    raceIndex: '',
    classIndex: '',
    subclassIndex: '',
    backgroundIndex: '',
    skillPickIndexes: [],
    spellIndexes: [],
    equipmentIndexes: [],
  }
}

function abilityBonusMap(raceDetail) {
  const bonuses = {}
  for (const entry of raceDetail?.ability_bonuses ?? []) {
    const ability = entry?.ability_score?.index
    const bonus = entry?.bonus ?? 0
    if (ability) bonuses[ability] = bonus
  }
  return bonuses
}

function applyBonus(base, bonus) {
  const final = {}
  for (const ability of ABILITIES) {
    final[ability] = base[ability] + (bonus[ability] ?? 0)
  }
  return final
}

function isCompleteBase(base) {
  return base && ABILITIES.every((ability) => Number.isInteger(base[ability]))
}

export default function CharacterWizardPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [step, setStep] = useState(0)
  const [draft, setDraft] = useState(initialDraft)
  const [catalog, setCatalog] = useState({ races: [], classes: [], backgrounds: [], skills: [], equipment: [] })
  const [catalogError, setCatalogError] = useState('')
  const [classDetail, setClassDetail] = useState(null)
  const [classLevels, setClassLevels] = useState(null)
  const [classSpells, setClassSpells] = useState(null)
  const [raceDetail, setRaceDetail] = useState(null)
  const [raceBonus, setRaceBonus] = useState({})
  const [baseScores, setBaseScores] = useState(null)
  const [equipmentPrices, setEquipmentPrices] = useState({})
  const [result, setResult] = useState(null)
  const [compiledDraft, setCompiledDraft] = useState(null)
  const [hint, setHint] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [busy, setBusy] = useState(false)
  const [rolling, setRolling] = useState(false)

  function commitScores(next) {
    setBaseScores(next)
    if (isCompleteBase(next)) {
      const final = applyBonus(next, raceBonus)
      setDraft((current) => ({ ...current, ...final }))
      setResult(null)
      setCompiledDraft(null)
      setError('')
    }
  }

  function setRaceBonusFromDetail(detail) {
    const bonus = abilityBonusMap(detail)
    setRaceBonus(bonus)
    setRaceDetail(detail)
    if (isCompleteBase(baseScores)) {
      const final = applyBonus(baseScores, bonus)
      setDraft((current) => ({ ...current, ...final }))
      setResult(null)
      setCompiledDraft(null)
    }
  }

  useEffect(() => {
    if (location.state?.draft) {
      setDraft(location.state.draft)
      const base = ABILITIES.reduce((acc, ability) => ({ ...acc, [ability]: location.state.draft[ability] }), {})
      setBaseScores(ROLLED_SOURCES.has(location.state.draft.scoreSource) ? null : base)
    }
  }, [location.state])

  useEffect(() => {
    Promise.all([
      srdList('races'),
      srdList('classes'),
      srdList('backgrounds'),
      srdList('skills'),
      srdList('equipment'),
    ])
      .then(([races, classes, backgrounds, skills, equipment]) => {
        setCatalog({
          races: srdRows(races),
          classes: srdRows(classes),
          backgrounds: srdRows(backgrounds),
          skills: srdRows(skills),
          equipment: srdRows(equipment),
        })
      })
      .catch(() => setCatalogError('Could not load character options (SRD data unavailable)'))
  }, [])

  useEffect(() => {
    if (draft.raceIndex) {
      srdDetail('races', draft.raceIndex)
        .then(setRaceBonusFromDetail)
        .catch(() => setRaceBonusFromDetail(null))
    } else {
      setRaceBonusFromDetail(null)
    }
  }, [draft.raceIndex])

  useEffect(() => {
    if (!draft.classIndex) return
    setBusy(true)
    setClassDetail(null)
    setClassLevels(null)
    setClassSpells(null)
    srdDetail('classes', draft.classIndex)
      .then((detail) => {
        setClassDetail(detail)
        setDraft((current) => ({ ...current, subclassIndex: '' }))
        if (detail?.spellcasting && detail.spellcasting.level === 1) {
          return Promise.all([
            srdSubresource('classes', draft.classIndex, 'levels'),
            srdSubresource('classes', draft.classIndex, 'spells'),
          ]).then(([levels, spells]) => {
            setClassLevels(levels)
            setClassSpells(spells)
          })
        }
        return undefined
      })
      .catch(() => setCatalogError('Could not load class details'))
      .finally(() => setBusy(false))
  }, [draft.classIndex])

  const caster = Boolean(classDetail?.spellcasting)
  const classSkills = useMemo(
    () =>
      [...offeredClassSkills(classDetail)]
        .map(plainSkill)
        .filter((index) => catalog.skills.some((skill) => skill.index === index)),
    [classDetail, catalog.skills],
  )
  const classCap = classDetail?.proficiency_choices?.[0]?.choose ?? 2
  const skillCap = classCap + 2
  const hasSubclasses = (classDetail?.subclasses ?? []).length > 0
  const requiredSubclassLevel = subclassLevel(classDetail)

  const castingRow = useMemo(() => {
    if (!Array.isArray(classLevels)) return null
    return classLevels.find((row) => row.level === draft.startingLevel)?.spellcasting ?? null
  }, [classLevels, draft.startingLevel])
  const cantripsKnown = castingRow?.cantrips_known ?? 0
  const maxSlot = useMemo(() => {
    if (!castingRow) return 0
    let max = 0
    for (let i = 1; i <= 9; i++) {
      const count = castingRow[`spell_slots_level_${i}`] ?? 0
      if (count > 0) max = i
    }
    return max
  }, [castingRow])
  const spellGroups = useMemo(() => {
    const groups = {}
    for (const spell of classSpells?.results ?? []) {
      const level = spell.level ?? 0
      groups[level] ??= []
      groups[level].push(spell)
    }
    return Object.keys(groups)
      .sort((a, b) => Number(a) - Number(b))
      .map((level) => ({ level: Number(level), spells: groups[level] }))
  }, [classSpells])

  const spellsStepVisible = caster && step === 7
  const spellsSkipped = !caster && step === 7

  const budget = startingGoldClassBudget(draft.classIndex)
  const spentGold = (draft.equipmentIndexes ?? []).reduce(
    (sum, index) => sum + (equipmentPriceGp(index) ?? equipmentPrices[index] ?? 0),
    0,
  )
  const overBudget = spentGold > budget

  function update(field, value) {
    setDraft((current) => ({ ...current, [field]: value }))
    if (field === 'scoreSource') {
      if (ROLLED_SOURCES.has(value)) {
        setBaseScores(null)
      }
    }
    setResult(null)
    setCompiledDraft(null)
    setError('')
  }

  function togglePick(field, value) {
    setDraft((current) => {
      const list = current[field]
      const next = list.includes(value) ? list.filter((item) => item !== value) : [...list, value]
      return { ...current, [field]: next }
    })
  }

  function toggleSkill(value) {
    setHint('')
    const picks = draft.skillPickIndexes
    const isOther = !classSkills.includes(value)
    if (!picks.includes(value)) {
      const others = picks.filter((pick) => !classSkills.includes(pick)).length
      if (isOther && others >= 2) {
        setHint('At most 2 skill picks may come from outside your class list (background picks).')
        return
      }
      if (picks.length >= skillCap) {
        setHint(`This class allows at most ${classCap} class skills plus 2 background picks.`)
        return
      }
    }
    togglePick('skillPickIndexes', value)
  }

  function toggleSpell(value) {
    const spell = (classSpells?.results ?? []).find((item) => item.index === value)
    const isCantrip = spell && (spell.level ?? 0) === 0
    const picks = draft.spellIndexes
    if (!picks.includes(value) && isCantrip) {
      const cantrips = picks.filter((pick) => (classSpells?.results ?? []).find((s) => s.index === pick)?.level === 0)
      if (cantrips.length >= cantripsKnown) {
        setHint(`This class knows ${cantripsKnown} cantrip${cantripsKnown === 1 ? '' : 's'} at level ${draft.startingLevel}.`)
        return
      }
    }
    setHint('')
    togglePick('spellIndexes', value)
  }

  function toggleEquipment(value) {
    togglePick('equipmentIndexes', value)
    if (equipmentPriceGp(value) == null && !(value in equipmentPrices)) {
      srdDetail('equipment', value)
        .then((detail) => {
          const cost = detail?.cost
          if (cost?.unit) {
            const unit = cost.unit
            const quantity = cost.quantity ?? 0
            const gp =
              unit === 'gp' ? quantity : unit === 'sp' ? quantity / 10 : unit === 'cp' ? quantity / 100 : unit === 'pp' ? quantity * 10 : quantity
            setEquipmentPrices((current) => ({ ...current, [value]: gp }))
          }
        })
        .catch(() => {})
    }
  }

  async function handleRoll() {
    setRolling(true)
    setError('')
    try {
      const rolled = await rollScores({ scoreSource: draft.scoreSource })
commitScores({
        strength: rolled.strength,
        dexterity: rolled.dexterity,
        constitution: rolled.constitution,
        intelligence: rolled.intelligence,
        wisdom: rolled.wisdom,
        charisma: rolled.charisma,
      })

    } catch (rollError) {
      setError(rollError.message)
    } finally {
      setRolling(false)
    }
  }

  function pickStandardArray() {
    commitScores({ strength: 15, dexterity: 14, constitution: 13, intelligence: 12, wisdom: 10, charisma: 8 })
  }

  function recommendedKit() {
    const kit = new Set((classDetail?.starting_equipment ?? []).map((row) => row.equipment?.index))
    for (const group of classDetail?.starting_equipment_options ?? []) {
      const first = (group?.from?.options ?? []).find(
        (option) => option.option_type === 'counted_reference' || option.option_type === 'multiple',
      )
      if (first?.option_type === 'multiple') {
        for (const item of first.items ?? []) kit.add(item?.of?.index)
      } else if (first?.of?.index) {
        kit.add(first.of.index)
      }
    }
    return [...kit].filter((index) => catalog.equipment.some((row) => row.index === index))
  }

  function applyRecommendedKit() {
    setHint('')
    setDraft((current) => ({
      ...current,
      equipmentIndexes: [...new Set([...current.equipmentIndexes, ...recommendedKit()])],
    }))
    setResult(null)
    setCompiledDraft(null)
  }

  async function handleCompile() {
    setError('')
    setResult(null)
    setCompiledDraft(null)
    setBusy(true)
    try {
      const compiled = await compileCharacter(draft)
      setResult(compiled)
      if (compiled.valid) setCompiledDraft(draft)
    } catch (compilationError) {
      setError(compilationError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleCreate() {
    setError('')
    setSaving(true)
    try {
      const created = await createCharacter(compiledDraft)
      navigate(`/characters/${created.id}`)
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setSaving(false)
    }
  }

  function baseScoresLegal() {
    if (ROLLED_SOURCES.has(draft.scoreSource)) {
      return isCompleteBase(baseScores)
    }
    return validateBaseScores(draft.scoreSource, baseScores ?? {})
  }

  function canAdvance() {
    if (draft.name.trim().length === 0 && step === 0) return false
    if (step === 1 && !baseScoresLegal()) return false
    if (step === 2 && !draft.raceIndex) return false
    if (step === 3 && !draft.classIndex) return false
    if (step === 4 && hasSubclasses && !draft.subclassIndex) return false
    if (step === 5 && !draft.backgroundIndex) return false
    if (step === 8 && overBudget) return false
    if (step === 7 && !caster) return true
    return true
  }

  const otherSkills = catalog.skills.filter((skill) => !classSkills.includes(skill.index))

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Character wizard</h1>

      <ol className="flex flex-wrap items-center gap-1 text-xs">
        {STEPS.map((label, index) => (
          <li
            key={label}
            className={index === step ? 'font-semibold text-zinc-900' : 'text-zinc-400'}
            aria-current={index === step ? 'step' : undefined}
          >
            {index > 0 && <span className="mx-1 text-zinc-300">›</span>}
            {index === 7 && classDetail && !caster ? `${label} (skipped)` : label}
          </li>
        ))}
      </ol>

      {catalogError && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {catalogError}
        </p>
      )}
      {hint && (
        <p role="status" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {hint}
        </p>
      )}
      {error && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        {step === 0 && (
          <div className="space-y-4">
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Character name</span>
              <input
                type="text"
                value={draft.name}
                onChange={(event) => update('name', event.target.value)}
                placeholder="Tordek the Earnest"
                maxLength={60}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Starting level (1–3)</span>
              <select
                value={draft.startingLevel}
                onChange={(event) => update('startingLevel', Number(event.target.value))}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm"
              >
                {[1, 2, 3].map((level) => (
                  <option key={level} value={level}>
                    Level {level}
                  </option>
                ))}
              </select>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Ability scores</span>
              <select
                value={draft.scoreSource}
                onChange={(event) => update('scoreSource', event.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm"
              >
                {SCORE_SOURCES.map((source) => (
                  <option key={source} value={source}>
                    {SCORE_SOURCE_LABELS[source]}
                  </option>
                ))}
              </select>
            </label>
          </div>
        )}

        {step === 1 && (
          <ScoreStep
            draft={draft}
            baseScores={baseScores}
            raceBonus={raceBonus}
            rolling={rolling}
            onRoll={handleRoll}
            onSetScore={commitScores}
            onStandardArray={pickStandardArray}
          />
        )}

        {step === 2 && (
          <div className="space-y-3">
            <PickGrid label="Race" rows={catalog.races} value={draft.raceIndex} onSelect={(value) => update('raceIndex', value)} />
            {raceDetail && (
              <p className="text-sm text-zinc-500">
                {raceDetail.name} ability bonuses:{' '}
                {abilityBonusMap(raceDetail) && Object.keys(abilityBonusMap(raceDetail)).length
                  ? Object.entries(abilityBonusMap(raceDetail))
                      .map(([ability, bonus]) => `+${bonus} ${ABILITY_LABELS[ability]}`)
                      .join(', ')
                  : 'none'}
              </p>
            )}
          </div>
        )}

        {step === 3 && <PickGrid label="Class" rows={catalog.classes} value={draft.classIndex} onSelect={(value) => update('classIndex', value)} />}

        {step === 4 && (
          <div className="space-y-3">
            {!classDetail && <p className="text-sm text-zinc-500">Pick a class first.</p>}
            {classDetail && !hasSubclasses && (
              <p className="text-sm text-zinc-500">This class has no subclasses at the start — nothing to choose.</p>
            )}
            {classDetail && hasSubclasses && (
              <>
                <p className="text-sm text-zinc-500">
                  {draft.startingLevel < requiredSubclassLevel
                    ? `Subclasses unlock at level ${requiredSubclassLevel}.`
                    : 'Choose your subclass.'}
                </p>
                {draft.startingLevel >= requiredSubclassLevel && (
                  <PickGrid
                    label="Subclass"
                    rows={(classDetail.subclasses ?? []).map((item) => ({ index: item.index, name: item.name }))}
                    value={draft.subclassIndex}
                    onSelect={(value) => update('subclassIndex', value)}
                  />
                )}
              </>
            )}
          </div>
        )}

        {step === 5 && <PickGrid label="Background" rows={catalog.backgrounds} value={draft.backgroundIndex} onSelect={(value) => update('backgroundIndex', value)} />}

        {step === 6 && (
          <div className="space-y-4">
            <p className="text-sm text-zinc-500">
              Pick up to {classCap} skills from your class and up to 2 more from your background.
            </p>
            <SkillGroup
              title="Class skills"
              rows={catalog.skills.filter((skill) => classSkills.includes(skill.index))}
              picks={draft.skillPickIndexes}
              onToggle={toggleSkill}
            />
            <SkillGroup
              title="Background picks"
              rows={otherSkills}
              picks={draft.skillPickIndexes}
              onToggle={toggleSkill}
            />
          </div>
        )}

        {spellsSkipped && (
          <p className="text-sm text-zinc-500">This class cannot cast spells at the start — nothing to choose.</p>
        )}

        {spellsStepVisible && (
          <div className="space-y-4">
            <p className="text-sm text-zinc-500">
              Cantrips known: {cantripsKnown} · spells up to level {maxSlot}.
            </p>
            {spellGroups
              .filter((group) => group.level <= maxSlot)
              .map((group) => (
                <div key={group.level}>
                  <h3 className="text-sm font-semibold text-zinc-700">
                    {group.level === 0 ? 'Cantrips' : `Level ${group.level}`}
                  </h3>
                  <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {group.spells.map((spell) => {
                      const checked = draft.spellIndexes.includes(spell.index)
                      return (
                        <label
                          key={spell.index}
                          className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                            checked ? 'border-zinc-900 bg-zinc-900 text-white' : 'border-zinc-300 hover:bg-zinc-50'
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleSpell(spell.index)}
                            className="accent-zinc-900"
                          />
                          {spell.name}
                        </label>
                      )
                    })}
                  </div>
                </div>
              ))}
          </div>
        )}

        {step === 8 && (
          <ShopStep
            items={catalog.equipment}
            selected={draft.equipmentIndexes}
            budget={budget}
            spentGold={spentGold}
            overBudget={overBudget}
            priceOf={(index) => equipmentPriceGp(index) ?? equipmentPrices[index] ?? null}
            recommendedKit={recommendedKit()}
            onToggle={toggleEquipment}
            onTakeRecommended={applyRecommendedKit}
          />
        )}

        {step === 9 && (
          <ReviewStep
            draft={draft}
            result={result}
            compiledDraft={compiledDraft}
            classSkills={classSkills}
            hasSubclasses={hasSubclasses}
            budget={budget}
            spentGold={spentGold}
            busy={busy}
            onCompile={handleCompile}
            onCreate={handleCreate}
            saving={saving}
          />
        )}
      </section>

      <div className="flex items-center justify-between">
        <button
          type="button"
          disabled={step === 0}
          onClick={() => {
            setStep((current) => current - 1)
            setHint('')
            setError('')
          }}
          className="rounded-md border border-zinc-300 px-4 py-2 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
        >
          Back
        </button>
        {step < STEPS.length - 1 && (
          <button
            type="button"
            disabled={!canAdvance() || busy}
            onClick={() => {
              setStep((current) => current + 1)
              setHint('')
              setError('')
              setResult(null)
              setCompiledDraft(null)
            }}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            Next
          </button>
        )}
      </div>
    </div>
  )
}

function modText(score) {
  const mod = abilityModifier(score)
  return mod >= 0 ? `+${mod}` : String(mod)
}

function ScoreStep({ draft, baseScores, raceBonus, rolling, onRoll, onSetScore, onStandardArray }) {
  const { scoreSource } = draft
  const rolled = ROLLED_SOURCES.has(scoreSource)
  const rolledDone = rolled && isCompleteBase(baseScores)
  const values = ABILITIES.map((ability) => (rolledDone || !rolled ? baseScores?.[ability] ?? '' : ''))
  const usedPoints = !rolled
    ? values.reduce((sum, value) => sum + pointBuyCost(Number(value) || 0), 0)
    : null
  const legal = validateBaseScores(scoreSource, baseScores ?? {})

  const finalModNote = (ability) => {
    if (rolled && !rolledDone) return '—'
    const base = baseScores?.[ability]
    const bonus = raceBonus[ability] ?? 0
    if (Number.isInteger(base) && bonus) return `base ${base} +${bonus} = ${base + bonus}`
    return modText(base)
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-zinc-500">
        {rolled
          ? `The ${SCORE_SOURCE_LABELS[scoreSource]} roll happens on the server so nobody can stack the dice — these six values are what you get.`
          : `Assign the ${scoreSource === 'STANDARD_ARRAY' ? 'standard array (15,14,13,12,10,8)' : '27-point-buy values (8–15)'} yourself.`}
      </p>

      {rolled && (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onRoll}
            disabled={rolling}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {rolling ? 'Rolling…' : rolledDone ? 'Roll again' : 'Roll ability scores'}
          </button>
          {rolledDone && <span className="text-sm text-zinc-500">Locked in — the race bonus is applied next step.</span>}
        </div>
      )}

      {!rolled && scoreSource === 'STANDARD_ARRAY' && (
        <button
          type="button"
          onClick={onStandardArray}
          className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50"
        >
          Use the standard array (as written)
        </button>
      )}
      {!rolled && scoreSource === 'POINT_BUY' && (
        <p className={usedPoints <= 27 ? 'text-sm text-zinc-500' : 'text-sm text-red-600'}>
          Points used: {usedPoints} / 27
        </p>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {ABILITIES.map((ability) => (
          <label key={ability} className="block">
            <span className="text-sm font-medium text-zinc-700">{ABILITY_LABELS[ability]}</span>
            <div className="mt-1 flex items-center gap-2">
              <input
                type="number"
                disabled={rolled}
                min={scoreSource === 'HOUSE_RULE_D20' ? 1 : scoreSource === 'FOUR_D6_DROP_LOWEST' ? 3 : 8}
                max={scoreSource === 'HOUSE_RULE_D20' ? 30 : scoreSource === 'FOUR_D6_DROP_LOWEST' ? 18 : 15}
                value={rolled && !rolledDone ? '' : (baseScores?.[ability] ?? '')}
                onChange={(event) => {
                  const next = { ...baseScores, [ability]: Number(event.target.value) }
                  onSetScore(next)
                }}
                placeholder={rolled ? '—' : ''}
                className={`w-28 rounded-md border px-3 py-2 text-sm ${
                  rolled && !rolledDone ? 'border-zinc-200 bg-zinc-50 text-zinc-400' : 'border-zinc-300'
                }`}
              />
              <span className="text-xs text-zinc-500">mod {finalModNote(ability)}</span>
            </div>
          </label>
        ))}
      </div>

      {!rolled && !legal && (
        <p role="status" className="text-sm text-red-600">
          {scoreSource === 'STANDARD_ARRAY'
            ? 'Standard array needs exactly 15, 14, 13, 12, 10 and 8.'
            : 'Point-buy scores must sit between 8 and 15 and add up to 27 points or fewer.'}
        </p>
      )}
    </div>
  )
}

function ShopStep({ items, selected, budget, spentGold, overBudget, priceOf, recommendedKit, onToggle, onTakeRecommended }) {
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-zinc-700">
          Starting gold <span className="text-zinc-500">({budget} gp)</span> · Spent{' '}
          <span className={overBudget ? 'text-red-600' : 'text-zinc-500'}>
            {Math.round(spentGold * 100) / 100} gp
          </span>{' '}
          · Remaining <span className={overBudget ? 'text-red-600' : 'text-emerald-700'}>{Math.max(0, Math.round((budget - spentGold) * 100) / 100)} gp</span>
        </p>
        {recommendedKit.length > 0 && (
          <button
            type="button"
            onClick={onTakeRecommended}
            className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50"
          >
            Take your class's recommended kit
          </button>
        )}
      </div>
      {overBudget && (
        <p role="alert" className="text-sm text-red-600">
          You've spent more than your class's starting gold — remove a few items.
        </p>
      )}
      <p className="text-sm text-zinc-500">Every item is bought out of your starting gold. A class kit is suggested but you choose.</p>
      {recommendedKit.length > 0 && (
        <p className="text-sm text-zinc-500">Recommended kit: {recommendedKit.join(', ')}</p>
      )}
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {items.map((item) => {
          const checked = selected.includes(item.index)
          const price = priceOf(item.index)
          return (
            <label
              key={item.index}
              className={`flex cursor-pointer items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm ${
                checked ? 'border-zinc-900 bg-zinc-900 text-white' : 'border-zinc-300 hover:bg-zinc-50'
              }`}
            >
              <span className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => onToggle(item.index)}
                  className="accent-zinc-900"
                />
                {item.name}
              </span>
              <span className="text-xs">{price == null ? '—' : `${price} gp`}</span>
            </label>
          )
        })}
      </div>
    </div>
  )
}

function PickGrid({ label, rows, value, onSelect }) {
  return (
    <div className="space-y-3">
      <p className="text-sm text-zinc-500">Choose your {label.toLowerCase()}.</p>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {rows.map((row) => {
          const selected = value === row.index
          return (
            <button
              key={row.index}
              type="button"
              onClick={() => onSelect(row.index)}
              className={`rounded-md border px-3 py-2 text-left text-sm ${
                selected ? 'border-zinc-900 bg-zinc-900 text-white' : 'border-zinc-300 hover:bg-zinc-50'
              }`}
            >
              {row.name}
            </button>
          )
        })}
      </div>
    </div>
  )
}

function SkillGroup({ title, rows, picks, onToggle }) {
  if (rows.length === 0) return null
  return (
    <div>
      <h3 className="text-sm font-semibold text-zinc-700">{title}</h3>
      <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
        {rows.map((skill) => {
          const checked = picks.includes(skill.index)
          return (
            <label
              key={skill.index}
              className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                checked ? 'border-zinc-900 bg-zinc-900 text-white' : 'border-zinc-300 hover:bg-zinc-50'
              }`}
            >
              <input type="checkbox" checked={checked} onChange={() => onToggle(skill.index)} className="accent-zinc-900" />
              {skill.name}
            </label>
          )
        })}
      </div>
    </div>
  )
}

function ReviewStep({ draft, result, compiledDraft, classSkills, hasSubclasses, budget, spentGold, busy, onCompile, onCreate, saving }) {
  const selectedSkills = draft.skillPickIndexes
  const backgroundPicks = selectedSkills.filter((pick) => !classSkills.includes(pick))

  return (
    <div className="space-y-4">
      <h2 className="font-semibold">Review your choices</h2>
      <dl className="grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
        <ReviewRow label="Name" value={draft.name} />
        <ReviewRow label="Level" value={draft.startingLevel} />
        <ReviewRow label="Scores" value={SCORE_SOURCE_LABELS[draft.scoreSource]} />
        <ReviewRow label="Race" value={draft.raceIndex || '—'} />
        <ReviewRow label="Class" value={draft.classIndex || '—'} />
        <ReviewRow
          label="Subclass"
          value={draft.subclassIndex ? draft.subclassIndex : hasSubclasses ? '—' : 'none'}
        />
        <ReviewRow label="Background" value={draft.backgroundIndex || '—'} />
        <ReviewRow label="Skills" value={selectedSkills.length ? selectedSkills.join(', ') : 'none'} />
        <ReviewRow
          label="Background picks"
          value={backgroundPicks.length ? `${backgroundPicks.join(', ')} (${backgroundPicks.length}/2)` : 'none'}
        />
        <ReviewRow label="Spells" value={draft.spellIndexes.length ? draft.spellIndexes.length + ' selected' : 'none'} />
        <ReviewRow label="Equipment" value={draft.equipmentIndexes.length ? draft.equipmentIndexes.join(', ') : 'none'} />
        <ReviewRow label="Starting gold" value={`${budget} gp`} />
        <ReviewRow label="Spent on gear" value={`${Math.round(spentGold * 100) / 100} gp`} />
      </dl>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onCompile}
          disabled={busy}
          className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
        >
          {busy ? 'Compiling…' : 'Compile sheet'}
        </button>
      </div>

      {result && !result.valid && (
        <ul role="alert" className="space-y-1">
          {result.violations.map((violation) => (
            <li key={violation} className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {violation}
            </li>
          ))}
        </ul>
      )}

      {result && result.valid && compiledDraft && (
        <div className="rounded-xl border border-emerald-300 bg-emerald-50 p-4">
          <p className="text-sm font-medium text-emerald-900">This sheet is legal.</p>
          <p className="mt-1 text-sm text-emerald-800">
            {result.sheet.raceIndex} {result.sheet.classIndex} · level {result.sheet.level} · HP{' '}
            {result.sheet.hitPoints} · AC {result.sheet.armorClass} · speed {result.sheet.speedFeet} ft
          </p>
          <button
            type="button"
            onClick={onCreate}
            disabled={saving}
            className="mt-3 rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {saving ? 'Creating…' : 'Create character'}
          </button>
        </div>
      )}
    </div>
  )
}

function ReviewRow({ label, value }) {
  return (
    <div>
      <dt className="text-zinc-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  )
}