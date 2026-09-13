import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ABILITIES, abilityModifier, deleteCharacter, getCharacter } from '../lib/characters'

const ABILITY_LABELS = {
  strength: 'Strength',
  dexterity: 'Dexterity',
  constitution: 'Constitution',
  intelligence: 'Intelligence',
  wisdom: 'Wisdom',
  charisma: 'Charisma',
}

function signed(value) {
  return value >= 0 ? `+${value}` : `${value}`
}

export default function CharacterSheetPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [sheet, setSheet] = useState(null)
  const [error, setError] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    getCharacter(id)
      .then(setSheet)
      .catch(() => setError('Could not load this character sheet'))
  }, [id])

  if (error) {
    return (
      <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
        {error}
      </p>
    )
  }

  if (!sheet) {
    return <p className="text-sm text-zinc-500">Loading character…</p>
  }

  async function handleDelete() {
    setDeleting(true)
    try {
      await deleteCharacter(id)
      navigate('/characters')
    } catch (deleteError) {
      setError(deleteError.message)
      setConfirming(false)
      setDeleting(false)
    }
  }

  return (
    <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
      <div className="space-y-6">
        <header className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">{sheet.name}</h1>
            <p className="text-sm text-zinc-500">
              Level {sheet.level} · {sheet.raceIndex} {sheet.classIndex}
              {sheet.subclassIndex ? ` (${sheet.subclassIndex})` : ''} · {sheet.backgroundIndex}
            </p>
          </div>
          <button
            type="button"
            onClick={() => setConfirming(true)}
            className="rounded-md border border-red-200 px-3 py-1 text-sm text-red-700 hover:bg-red-50"
          >
            Delete character
          </button>
        </header>

        <section className="grid grid-cols-1 gap-3 sm:grid-cols-4">
          <StatCard label="Hit points" value={sheet.hitPoints} />
          <StatCard label="Armor class" value={sheet.armorClass} />
          <StatCard label="Speed" value={`${sheet.speedFeet} ft`} />
          <StatCard label="Initiative" value={signed(abilityModifier(sheet.dexterity))} />
        </section>

        <section>
          <h2 className="text-sm font-semibold text-zinc-700">Ability scores</h2>
          <div className="mt-2 grid grid-cols-2 gap-3 sm:grid-cols-6">
            {ABILITIES.map((ability) => {
              const score = sheet[ability]
              return (
                <div key={ability} className="rounded-lg border border-zinc-200 bg-white p-3 text-center shadow-sm">
                  <p className="text-xs text-zinc-500">{ABILITY_LABELS[ability]}</p>
                  <p className="text-xl font-semibold">{score}</p>
                  <p className="text-xs text-zinc-500">{signed(abilityModifier(score))}</p>
                </div>
              )
            })}
          </div>
        </section>

        <section>
          <h2 className="text-sm font-semibold text-zinc-700">Features</h2>
          {!sheet.featureIndexes || sheet.featureIndexes.length === 0 ? (
            <p className="mt-1 text-sm text-zinc-500">None</p>
          ) : (
            <ul className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {sheet.featureIndexes.map((feature) => (
                <li key={feature} className="rounded-md border border-zinc-200 px-3 py-2 text-sm">
                  {feature}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="text-sm font-semibold text-zinc-700">Skills</h2>
          {sheet.skillPicks.length === 0 ? (
            <p className="mt-1 text-sm text-zinc-500">None</p>
          ) : (
            <ul className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {sheet.skillPicks.map((skill) => (
                <li key={skill} className="rounded-md border border-zinc-200 px-3 py-2 text-sm">
                  {skill}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="text-sm font-semibold text-zinc-700">Spells</h2>
          {sheet.spellIndexes.length === 0 ? (
            <p className="mt-1 text-sm text-zinc-500">None</p>
          ) : (
            <ul className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {sheet.spellIndexes.map((spell) => (
                <li key={spell} className="rounded-md border border-zinc-200 px-3 py-2 text-sm">
                  {spell}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="text-sm font-semibold text-zinc-700">Equipment</h2>
          {sheet.equipmentIndexes.length === 0 ? (
            <p className="mt-1 text-sm text-zinc-500">None</p>
          ) : (
            <ul className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {sheet.equipmentIndexes.map((item) => (
                <li key={item} className="rounded-md border border-zinc-200 px-3 py-2 text-sm">
                  {item}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {confirming && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-confirm-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/50 p-4"
        >
          <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
            <h2 id="delete-confirm-title" className="font-semibold">Delete {sheet.name}?</h2>
            <p className="mt-2 text-sm text-zinc-500">
              This removes the character (and its sheet) for good. This can't be undone.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirming(false)}
                disabled={deleting}
                className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="rounded-md bg-red-700 px-3 py-1 text-sm font-medium text-white hover:bg-red-600 disabled:opacity-50"
              >
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

function StatCard({ label, value }) {
  return (
    <div className="rounded-lg border border-zinc-200 bg-white p-4 text-center shadow-sm">
      <p className="text-xs text-zinc-500">{label}</p>
      <p className="text-2xl font-semibold">{value}</p>
    </div>
  )
}