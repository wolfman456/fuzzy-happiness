import { useState } from 'react'
import {
  nextInitiative,
  removeInitiativeEntry,
  rerollInitiative,
  setInitiative,
} from '../lib/battleMap'

export default function InitiativeRail({ sessionId, map, isGm, disabled, onMapChange }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [tokenId, setTokenId] = useState('')
  const [label, setLabel] = useState('')
  const [score, setScore] = useState('')

  const entries = map.initiative ?? []
  const usedTokenIds = new Set(
    entries.map((entry) => entry.tokenId).filter(Boolean),
  )
  const freeTokens = (map.tokens ?? []).filter(
    (token) => !usedTokenIds.has(token.id),
  )
  const currentEntry =
    map.initiativeIndex >= 0 ? entries[map.initiativeIndex] : null

  async function run(call) {
    setBusy(true)
    setError('')
    try {
      onMapChange(await call())
      return true
    } catch (callError) {
      setError(callError.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  function handleNext() {
    return run(() => nextInitiative(sessionId))
  }

  function handleReroll(entryId) {
    return run(() => rerollInitiative(sessionId, entryId))
  }

  function handleRemove(entryId) {
    return run(() => removeInitiativeEntry(sessionId, entryId))
  }

  async function handleAdd(event) {
    event.preventDefault()
    const newEntry = {
      tokenId: tokenId || undefined,
      label: tokenId ? undefined : (label.trim() || undefined),
      score: score === '' ? undefined : Number(score),
    }
    if (!newEntry.tokenId && !newEntry.label) return
    const merged = entries.map((entry) => ({
      label: entry.label ?? undefined,
      tokenId: entry.tokenId ?? undefined,
      score: entry.score,
    }))
    merged.push(newEntry)
    if (await run(() => setInitiative(sessionId, { entries: merged }))) {
      setOpen(false)
      setTokenId('')
      setLabel('')
      setScore('')
    }
  }

  return (
    <section
      className="mt-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4"
      data-testid="initiative-rail"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="text-sm font-semibold text-zinc-800">Initiative</h3>
          <p className="text-xs text-zinc-500">
            {currentEntry
              ? `Turns: ${(currentEntry.tokenName ?? currentEntry.label) ?? '…'}`
              : 'No active turn yet'}
          </p>
        </div>
        {isGm && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={disabled || busy || entries.length === 0}
              onClick={handleNext}
              className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-40"
            >
              Next turn
            </button>
            <button
              type="button"
              disabled={disabled || busy}
              onClick={() => setOpen((current) => !current)}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-40"
            >
              Add entry
            </button>
          </div>
        )}
      </div>

      {entries.length === 0 ? (
        <p className="mt-2 text-sm text-zinc-500">
          {isGm ? 'No initiative order yet — add entries to get started.' : 'No initiative order yet.'}
        </p>
      ) : (
        <ol className="mt-3 space-y-1">
          {entries.map((entry, index) => {
            const isCurrent = index === map.initiativeIndex
            const name = entry.tokenName ?? entry.label ?? 'Unknown'
            return (
              <li
                key={entry.id}
                data-testid={`initiative-entry-${entry.id}`}
                className={`flex items-center justify-between gap-2 rounded-md px-2 py-1 text-sm ${
                  isCurrent ? 'bg-amber-100 ring-1 ring-amber-300' : ''
                }`}
              >
                <span className="flex items-baseline gap-2">
                  <span className="w-5 text-right font-mono text-xs text-zinc-400">
                    {index + 1}
                  </span>
                  <span className="font-medium text-zinc-800">{name}</span>
                  {isCurrent && (
                    <span className="rounded bg-amber-500 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-white">
                      Current
                    </span>
                  )}
                </span>
                <span className="flex items-center gap-2">
                  <span className="font-mono text-xs text-zinc-600">
                    {entry.score}
                  </span>
                  {isGm && (
                    <span className="flex items-center gap-1">
                      <button
                        type="button"
                        aria-label={`Reroll ${name}`}
                        disabled={disabled || busy}
                        onClick={() => handleReroll(entry.id)}
                        className="rounded border border-zinc-300 px-2 py-0.5 text-xs text-zinc-700 hover:bg-zinc-100 disabled:opacity-40"
                      >
                        Reroll
                      </button>
                      <button
                        type="button"
                        aria-label={`Remove ${name}`}
                        disabled={disabled || busy}
                        onClick={() => handleRemove(entry.id)}
                        className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-700 hover:bg-red-50 disabled:opacity-40"
                      >
                        Remove
                      </button>
                    </span>
                  )}
                </span>
              </li>
            )
          })}
        </ol>
      )}

      {isGm && open && (
        <form
          onSubmit={handleAdd}
          className="mt-3 flex flex-wrap items-end gap-2"
          data-testid="initiative-add-form"
        >
          <label className="text-xs text-zinc-700">
            Token
            <select
              value={tokenId}
              onChange={(event) => setTokenId(event.target.value)}
              className="mt-1 block w-36 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
            >
              <option value="">Custom label…</option>
              {freeTokens.map((token) => (
                <option key={token.id} value={token.id}>
                  {token.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-xs text-zinc-700">
            Label
            <input
              type="text"
              value={label}
              disabled={Boolean(tokenId)}
              onChange={(event) => setLabel(event.target.value)}
              placeholder="Orc · Strahd · Trap"
              className="mt-1 block w-36 rounded-md border border-zinc-300 px-2 py-1.5 text-sm disabled:bg-zinc-100"
            />
          </label>
          <label className="text-xs text-zinc-700">
            Score (blank = d20)
            <input
              type="number"
              min="1"
              max="999"
              value={score}
              onChange={(event) => setScore(event.target.value)}
              placeholder="Auto"
              className="mt-1 block w-20 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
            />
          </label>
          <div className="flex items-center gap-2">
            <button
              type="submit"
              disabled={
                busy || (!tokenId && !label.trim()) || score === '0'
              }
              className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-40"
            >
              Add to order
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setOpen(false)
                setTokenId('')
                setLabel('')
                setScore('')
              }}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-700">
          {error}
        </p>
      )}
    </section>
  )
}