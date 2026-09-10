import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  ABILITIES,
  SCORE_SOURCES,
  SCORE_SOURCE_LABELS,
  abilityModifier,
  compileCharacter,
  createCharacter,
  pointBuyCost,
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
  const [result, setResult] = useState(null)
  const [compiledDraft, setCompiledDraft] = useState(null)
  const [hint, setHint] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (location.state?.draft) setDraft(location.state.draft)
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

  function update(field, value) {
    setDraft((current) => ({ ...current, [field]: value }))
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

  function canAdvance() {
    if (draft.name.trim().length === 0 && step === 0) return false
    if (step === 2 && !draft.raceIndex) return false
    if (step === 3 && !draft.classIndex) return false
    if (step === 4 && hasSubclasses && !draft.subclassIndex) return false
    if (step === 5 && !draft.backgroundIndex) return false
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
          <ScoreStep draft={draft} update={update} onChange={() => setResult(null)} />
        )}

        {step === 2 && <PickGrid label="Race" rows={catalog.races} value={draft.raceIndex} onSelect={(value) => update('raceIndex', value)} />}

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
          <div className="space-y-3">
            <p className="text-sm text-zinc-500">Pick your starting equipment (armor and shields count toward AC).</p>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {catalog.equipment.map((item) => {
                const checked = draft.equipmentIndexes.includes(item.index)
                return (
                  <label
                    key={item.index}
                    className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                      checked ? 'border-zinc-900 bg-zinc-900 text-white' : 'border-zinc-300 hover:bg-zinc-50'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => togglePick('equipmentIndexes', item.index)}
                      className="accent-zinc-900"
                    />
                    {item.name}
                  </label>
                )
              })}
            </div>
          </div>
        )}

        {step === 9 && (
          <ReviewStep
            draft={draft}
            result={result}
            compiledDraft={compiledDraft}
            classSkills={classSkills}
            hasSubclasses={hasSubclasses}
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

function ScoreStep({ draft, update, onChange }) {
  const { scoreSource } = draft
  const values = ABILITIES.map((ability) => draft[ability])
  const usedPoints =
    scoreSource === 'POINT_BUY' ? values.reduce((sum, value) => sum + pointBuyCost(value), 0) : null

  return (
    <div className="space-y-4">
      {scoreSource === 'STANDARD_ARRAY' && (
        <button
          type="button"
          onClick={() => {
            const assigned = { strength: 15, dexterity: 14, constitution: 13, intelligence: 12, wisdom: 10, charisma: 8 }
            Object.entries(assigned).forEach(([ability, value]) => update(ability, value))
            onChange()
          }}
          className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50"
        >
          Use the standard array (as written)
        </button>
      )}
      {scoreSource === 'POINT_BUY' && (
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
                min={scoreSource === 'HOUSE_RULE_D20' ? 1 : scoreSource === 'FOUR_D6_DROP_LOWEST' ? 3 : 8}
                max={scoreSource === 'HOUSE_RULE_D20' ? 30 : scoreSource === 'FOUR_D6_DROP_LOWEST' ? 18 : 15}
                value={draft[ability]}
                onChange={(event) => {
                  update(ability, Number(event.target.value))
                  onChange()
                }}
                className="w-24 rounded-md border border-zinc-300 px-3 py-2 text-sm"
              />
              <span className="text-xs text-zinc-500">
                mod {abilityModifier(draft[ability]) >= 0 ? `+${abilityModifier(draft[ability])}` : abilityModifier(draft[ability])}
              </span>
            </div>
          </label>
        ))}
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

function ReviewStep({ draft, result, compiledDraft, classSkills, hasSubclasses, busy, onCompile, onCreate, saving }) {
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