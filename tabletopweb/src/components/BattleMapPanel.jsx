import { useMemo, useState } from 'react'
import {
  RACE_SPEEDS,
  TOKEN_COLORS,
  reachableSquares,
  remainingFeet,
  squaresFor,
} from '../lib/mapGeometry'
import {
  addToken,
  moveToken,
  removeToken,
  turnCommand,
  updateToken,
} from '../lib/battleMap'

const SQUARE_PX = 36

const EMPTY_FORM = {
  name: '',
  category: 'MONSTER_NPC',
  race: 'Human',
  speedFeet: 30,
  color: TOKEN_COLORS[0],
  posX: 0,
  posY: 0,
}

export default function BattleMapPanel({ sessionId, map, user, isGm, disabled, onMapChange }) {
  const [selectedId, setSelectedId] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const selectedToken = useMemo(
    () => map.tokens.find((token) => token.id === selectedId) ?? null,
    [map.tokens, selectedId],
  )

  const mayMove = (token) =>
    !disabled && (isGm || token.linkedUserId === user?.id)

  const reachable = useMemo(() => {
    if (!selectedToken) return []
    if (disabled) return []
    if (!isGm && selectedToken.linkedUserId !== user?.id) return []
    return reachableSquares(selectedToken, map)
  }, [selectedToken, map, disabled, isGm, user?.id])

  const gridWidth = map.width * SQUARE_PX
  const gridHeight = map.height * SQUARE_PX

  function gridBackgroundStyle() {
    const cell = `${SQUARE_PX}px`
    return {
      width: `${gridWidth}px`,
      height: `${gridHeight}px`,
      backgroundImage:
        'linear-gradient(to right, #e4e4e7 1px, transparent 1px), linear-gradient(to bottom, #e4e4e7 1px, transparent 1px)',
      backgroundSize: `${cell} ${cell}`,
      backgroundPosition: '-1px -1px',
    }
  }

  function handleGridClick(event) {
    if (disabled || !selectedId) return
    const rect = event.currentTarget.getBoundingClientRect()
    const x = Math.floor((event.clientX - rect.left) / SQUARE_PX)
    const y = Math.floor((event.clientY - rect.top) / SQUARE_PX)
    if (x < 0 || y < 0 || x >= map.width || y >= map.height) return
    const target = map.tokens.find((token) => token.posX === x && token.posY === y)
    if (!target) setSelectedId(null)
  }

  function handleTokenClick(token) {
    if (disabled) return
    setSelectedId((current) => (current === token.id ? null : token.id))
  }

  async function handleMove(x, y) {
    if (!selectedToken || busy) return
    setBusy(true)
    setError('')
    try {
      const next = await moveToken(sessionId, selectedToken.id, { x, y })
      onMapChange(next)
      setSelectedId(null)
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  function startEdit(token) {
    setForm({
      name: token.name,
      category: token.category,
      race: 'Human',
      speedFeet: token.speedFeet,
      color: token.color,
      posX: token.posX,
      posY: token.posY,
    })
    setEditingId(token.id)
    setFormOpen(true)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    const body = {
      name: form.name.trim(),
      category: form.category,
      speedFeet: Number(form.speedFeet),
      color: form.color,
      posX: Number(form.posX),
      posY: Number(form.posY),
    }
    try {
      const next = editingId
        ? await updateToken(sessionId, editingId, body)
        : await addToken(sessionId, body)
      onMapChange(next)
      setFormOpen(false)
      setEditingId(null)
      setForm(EMPTY_FORM)
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(token) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const next = await removeToken(sessionId, token.id)
      if (selectedId === token.id) setSelectedId(null)
      onMapChange(next)
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleTurn(action, tokenId) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const next = await turnCommand(sessionId, { action, tokenId })
      onMapChange(next)
      setSelectedId((current) =>
        action === 'START' && tokenId ? tokenId : current,
      )
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  const onTurnName = map.tokens.find(
    (token) => token.id === map.currentTurnTokenId,
  )?.name

  return (
    <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">
            {map.name ?? 'Battle map'}
            <span className="ml-2 text-sm font-normal text-zinc-400">
              {map.squareFeet} ft per square · {map.width}×{map.height}
            </span>
          </h2>
          <p className="text-sm text-zinc-500">
            {onTurnName
              ? `Turn: ${onTurnName}`
              : 'No active turn — the GM can start one by selecting a token.'}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {isGm && (
            <button
              type="button"
              disabled={disabled || !selectedToken}
              onClick={() => handleTurn('START', selectedToken?.id)}
              className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-40"
            >
              Start turn
            </button>
          )}
          {isGm && (
            <button
              type="button"
              disabled={disabled}
              onClick={() => handleTurn('END')}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
            >
              End turn
            </button>
          )}
          {isGm && (
            <button
              type="button"
              disabled={disabled}
              onClick={() => handleTurn('NEW_ROUND')}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
            >
              New round
            </button>
          )}
          {isGm && (
            <button
              type="button"
              disabled={disabled || formOpen}
              onClick={() => {
                setEditingId(null)
                setForm(EMPTY_FORM)
                setFormOpen(true)
              }}
              className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-40"
            >
              Add token
            </button>
          )}
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {selectedToken && mayMove(selectedToken) && (
        <p className="mt-3 text-sm text-zinc-500">
          {selectedToken.name} has{' '}
          {squaresFor(remainingFeet(selectedToken), map.squareFeet)} square
          left this turn. Click a highlighted square to move.
        </p>
      )}

      <div className="mt-4 overflow-auto rounded-lg border border-zinc-200 p-2">
        <div
          data-testid="battle-grid"
          className="relative cursor-crosshair"
          style={gridBackgroundStyle()}
          onClick={handleGridClick}
        >
          {reachable.map(({ x, y }) => (
            <button
              type="button"
              key={`${x}-${y}`}
              data-testid={`reachable-${x}-${y}`}
              onClick={(event) => {
                event.stopPropagation()
                handleMove(x, y)
              }}
              style={{
                left: `${x * SQUARE_PX}px`,
                top: `${y * SQUARE_PX}px`,
                width: `${SQUARE_PX}px`,
                height: `${SQUARE_PX}px`,
              }}
              className="absolute rounded border border-emerald-500 bg-emerald-300/40 hover:bg-emerald-400/60"
              aria-label={`Move to square ${x}, ${y}`}
            />
          ))}
          {map.tokens.map((token) => (
            <TokenDisc
              key={token.id}
              token={token}
              selected={token.id === selectedId}
              interactive={!disabled && (isGm || token.linkedUserId === user?.id)}
              squarePx={SQUARE_PX}
              onClick={(event) => {
                event.stopPropagation()
                handleTokenClick(token)
              }}
            />
          ))}
        </div>
      </div>

      {(isGm || selectedToken) && (
        <ul className="mt-4 divide-y divide-zinc-100" data-testid="token-list">
          {map.tokens.map((token) => (
            <li key={token.id} className="flex items-center justify-between py-2">
              <span className="flex items-center gap-2 text-sm">
                <span
                  className="inline-block h-3 w-3 rounded-full"
                  style={{ backgroundColor: token.color }}
                />
                <span className="font-medium">{token.name}</span>
                <span className="text-xs text-zinc-400">
                  {token.category} · {token.speedFeet} ft ·{' '}
                  {token.movedFeet}/{token.speedFeet} ft moved
                </span>
              </span>
              {isGm && (
                <span className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={disabled || busy}
                    onClick={() => startEdit(token)}
                    className="rounded border border-zinc-300 px-2 py-0.5 text-xs text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    disabled={disabled || busy}
                    onClick={() => handleRemove(token)}
                    className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-700 hover:bg-red-50 disabled:opacity-40"
                  >
                    Remove
                  </button>
                </span>
              )}
            </li>
          ))}
        </ul>
      )}

      {isGm && formOpen && (
        <form
          onSubmit={handleSubmit}
          className="mt-4 grid gap-3 rounded-lg bg-zinc-50 p-4 sm:grid-cols-2"
          data-testid="token-form"
        >
          <label className="text-sm text-zinc-700">
            Name
            <input
              type="text"
              required
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            />
          </label>
          <label className="text-sm text-zinc-700">
            Category
            <select
              value={form.category}
              onChange={(event) =>
                setForm({ ...form, category: event.target.value })
              }
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            >
              <option value="MONSTER_NPC">Monster / NPC</option>
              <option value="PLAYER">Player</option>
            </select>
          </label>
          <label className="text-sm text-zinc-700">
            Race
            <select
              value={form.race}
              onChange={(event) => {
                const race = RACE_SPEEDS.find((r) => r.name === event.target.value)
                setForm({
                  ...form,
                  race: event.target.value,
                  speedFeet: race ? race.speedFeet : form.speedFeet,
                })
              }}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            >
              {RACE_SPEEDS.map((race) => (
                <option key={race.name} value={race.name}>
                  {race.name} ({race.speedFeet} ft)
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm text-zinc-700">
            Speed (ft/turn)
            <input
              type="number"
              min="0"
              required
              value={form.speedFeet}
              onChange={(event) =>
                setForm({ ...form, speedFeet: event.target.value })
              }
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
            />
          </label>
          <label className="text-sm text-zinc-700">
            Color
            <span className="mt-1 flex flex-wrap gap-1">
              {TOKEN_COLORS.map((color) => (
                <button
                  type="button"
                  key={color}
                  onClick={() => setForm({ ...form, color })}
                  aria-label={`Pick color ${color}`}
                  className={`h-6 w-6 rounded-full ${
                    form.color === color ? 'ring-2 ring-zinc-800 ring-offset-1' : ''
                  }`}
                  style={{ backgroundColor: color }}
                />
              ))}
            </span>
          </label>
          <div className="grid grid-cols-2 gap-2">
            <label className="text-sm text-zinc-700">
              X
              <input
                type="number"
                min="0"
                max={map.width - 1}
                required
                value={form.posX}
                onChange={(event) => setForm({ ...form, posX: event.target.value })}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
              />
            </label>
            <label className="text-sm text-zinc-700">
              Y
              <input
                type="number"
                min="0"
                max={map.height - 1}
                required
                value={form.posY}
                onChange={(event) => setForm({ ...form, posY: event.target.value })}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-1.5"
              />
            </label>
          </div>
          <div className="flex items-center gap-2 sm:col-span-2">
            <button
              type="submit"
              disabled={busy}
              className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              {editingId ? 'Save' : 'Add token'}
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setFormOpen(false)
                setEditingId(null)
              }}
              className="rounded-md border border-zinc-300 px-4 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

function TokenDisc({ token, selected, interactive, squarePx, onClick }) {
  const initials =
    token.name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word[0].toUpperCase())
      .join('') || '?'
  return (
    <button
      type="button"
      data-testid={`token-${token.id}`}
      onClick={onClick}
      disabled={!interactive}
      aria-label={`Token ${token.name} at ${token.posX}, ${token.posY}`}
      className={`absolute -translate-x-0.5 -translate-y-0.5 rounded-full font-semibold text-white shadow ${
        interactive ? 'cursor-grab hover:ring-2 hover:ring-zinc-500' : 'cursor-default'
      } ${selected ? 'ring-2 ring-zinc-900 ring-offset-1' : ''}`}
      style={{
        left: `${token.posX * squarePx}px`,
        top: `${token.posY * squarePx}px`,
        width: `${squarePx - 4}px`,
        height: `${squarePx - 4}px`,
        backgroundColor: token.color,
        fontSize: '0.7rem',
      }}
    >
      {initials}
    </button>
  )
}