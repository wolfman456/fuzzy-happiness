import { useEffect, useState } from 'react'
import { addToken } from '../lib/battleMap'
import { TOKEN_COLORS } from '../lib/mapGeometry'
import {
  MONSTER_CRS,
  MONSTER_EDITIONS,
  MONSTER_ROLES,
  generateMonster,
  listMyMonsters,
} from '../lib/monsters'

const INITIAL_FORM = {
  cr: '1',
  role: 'AUTO',
  edition: 'SRD_2014',
  name: '',
  concept: '',
  seed: '',
}

function firstFreeSquare(map) {
  const occupied = new Set(
    (map?.tokens ?? []).map((token) => `${token.posX},${token.posY}`),
  )
  for (let y = 0; y < (map?.height ?? 0); y += 1) {
    for (let x = 0; x < (map?.width ?? 0); x += 1) {
      if (!occupied.has(`${x},${y}`)) return { x, y }
    }
  }
  return { x: 0, y: 0 }
}

export default function MonsterGenerator({ sessionId, map, disabled, onMapChange }) {
  const [form, setForm] = useState(INITIAL_FORM)
  const [generated, setGenerated] = useState(null)
  const [mine, setMine] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    listMyMonsters()
      .then(setMine)
      .catch(() => setMine([]))
  }, [])

  async function handleGenerate(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const body = {
        cr: form.cr,
        role: form.role,
        edition: form.edition,
        seed: form.seed ? Number(form.seed) : undefined,
      }
      if (form.name.trim()) body.name = form.name.trim()
      if (form.concept.trim()) body.concept = form.concept.trim()
      const monster = await generateMonster(body)
      setGenerated(monster)
      setMine(await listMyMonsters())
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleAddToMap(monster) {
    if (busy) return
    if (!map) {
      setError('Create a battle map first, then add the monster.')
      return
    }
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const position = firstFreeSquare(map)
      const next = await addToken(sessionId, {
        name: monster.name,
        category: 'MONSTER_NPC',
        speedFeet: monster.speedFeet,
        color: TOKEN_COLORS[Math.abs(monster.id ?? 0) % TOKEN_COLORS.length],
        posX: position.x,
        posY: position.y,
      })
      onMapChange(next)
      setNotice(`${monster.name} added to the map at (${position.x}, ${position.y}).`)
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section
      className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm"
      data-testid="monster-generator"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Monster generator</h2>
      </div>
      <p className="mt-1 text-sm text-zinc-500">
        Build deterministic statblocks from a challenge rating and combat role.
      </p>

      <form
        onSubmit={handleGenerate}
        className="mt-4 grid gap-3 rounded-lg bg-zinc-50 p-4 sm:grid-cols-3"
        data-testid="monster-form"
      >
        <label className="text-sm text-zinc-700">
          Challenge rating
          <select
            value={form.cr}
            onChange={(event) => setForm({ ...form, cr: event.target.value })}
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-cr"
          >
            {MONSTER_CRS.map((cr) => (
              <option key={cr} value={cr}>
                {cr}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm text-zinc-700">
          Combat role
          <select
            value={form.role}
            onChange={(event) => setForm({ ...form, role: event.target.value })}
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-role"
          >
            {MONSTER_ROLES.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm text-zinc-700">
          Edition
          <select
            value={form.edition}
            onChange={(event) => setForm({ ...form, edition: event.target.value })}
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-edition"
          >
            {MONSTER_EDITIONS.map((edition) => (
              <option key={edition} value={edition}>
                {edition.replace('SRD_', 'SRD ')}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm text-zinc-700">
          Name
          <input
            type="text"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            placeholder="Optional"
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-name"
          />
        </label>
        <label className="text-sm text-zinc-700">
          Concept
          <input
            type="text"
            value={form.concept}
            onChange={(event) => setForm({ ...form, concept: event.target.value })}
            placeholder="e.g. a lurking swamp horror"
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-concept"
          />
        </label>
        <label className="text-sm text-zinc-700">
          Seed
          <input
            type="number"
            min="0"
            value={form.seed}
            onChange={(event) => setForm({ ...form, seed: event.target.value })}
            placeholder="Optional"
            className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            data-testid="monster-seed"
          />
        </label>
        <div className="flex items-end gap-2 sm:col-span-3">
          <button
            type="submit"
            disabled={busy || disabled}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            data-testid="monster-generate"
          >
            {busy ? 'Working…' : 'Generate monster'}
          </button>
        </div>
      </form>

      {error && (
        <p role="alert" data-testid="monster-error" className="mt-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="mt-3 text-sm text-emerald-700">
          {notice}
        </p>
      )}

      {generated && <StatblockCard monster={generated} onAdd={handleAddToMap} />}

      {mine.length > 0 && (
        <div className="mt-6">
          <h3 className="text-sm font-semibold text-zinc-700">My monsters</h3>
          <ul className="mt-2 divide-y divide-zinc-100" data-testid="monster-list">
            {mine.map((monster) => (
              <li
                key={monster.id}
                className="flex flex-wrap items-center justify-between gap-2 py-2"
              >
                <span className="flex items-center gap-2 text-sm">
                  <span className="font-medium">{monster.name}</span>
                  <span className="text-xs text-zinc-400">
                    CR {monster.cr} · {monster.role} · {monster.hitPoints} HP
                  </span>
                </span>
                <button
                  type="button"
                  disabled={busy || disabled}
                  onClick={() => handleAddToMap(monster)}
                  className="rounded border border-indigo-300 px-2 py-0.5 text-xs text-indigo-700 hover:bg-indigo-50 disabled:opacity-40"
                >
                  Add to map
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}

function StatblockCard({ monster, onAdd }) {
  return (
    <div
      data-testid="monster-statblock"
      className="mt-4 rounded-lg border border-zinc-200 p-4"
    >
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 className="text-base font-semibold">{monster.name}</h3>
          <p className="text-xs text-zinc-500">
            {monster.size} {monster.type} · {monster.alignment} · CR {monster.cr} ·{' '}
            {monster.xp} XP
          </p>
        </div>
        <button
          type="button"
          onClick={() => onAdd(monster)}
          data-testid={`add-generated-to-map`}
          className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-500"
        >
          Add to map
        </button>
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 text-sm sm:grid-cols-4">
        <div>
          <dt className="text-xs text-zinc-400">Armor class</dt>
          <dd>{monster.armorClass}</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-400">Hit points</dt>
          <dd>{monster.hitPoints}</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-400">Speed</dt>
          <dd>{monster.speedFeet} ft</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-400">Proficiency</dt>
          <dd>+{monster.proficiencyBonus}</dd>
        </div>
        <div className="col-span-2 sm:col-span-4">
          <dt className="text-xs text-zinc-400">Abilities</dt>
          <dd className="font-mono text-xs">
            STR {monster.strength} · DEX {monster.dexterity} · CON{' '}
            {monster.constitution} · INT {monster.intelligence} · WIS {monster.wisdom}{' '}
            · CHA {monster.charisma}
          </dd>
        </div>
      </dl>
      <p className="mt-3 text-sm text-zinc-600">{monster.description}</p>
      {monster.traits?.length > 0 && (
        <ul className="mt-2 space-y-1 text-sm text-zinc-700">
          {monster.traits.map((trait) => (
            <li key={trait}>{trait}</li>
          ))}
        </ul>
      )}
      {monster.actions?.length > 0 && (
        <ul className="mt-2 space-y-1 text-sm text-zinc-700">
          {monster.actions.map((action) => (
            <li key={action.name}>
              <span className="font-medium italic">{action.name}.</span>{' '}
              {action.description}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}