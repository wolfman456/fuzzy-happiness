import { useState } from 'react'

export default function DiceTray({ isGm, disabled, onRoll }) {
  const [expression, setExpression] = useState('')
  const [label, setLabel] = useState('')
  const [privateRoll, setPrivateRoll] = useState(false)
  const [rolling, setRolling] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    const expr = expression.trim()
    if (!expr || rolling) return
    setRolling(true)
    setError('')
    try {
      await onRoll({
        expression: expr,
        label: label.trim() || undefined,
        privateRoll: isGm && privateRoll,
      })
      setExpression('')
      setLabel('')
      setPrivateRoll(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setRolling(false)
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-label="Dice tray"
      className="mt-4 flex flex-wrap items-center gap-2 rounded-md border border-zinc-200 bg-zinc-50 px-3 py-2.5"
    >
      <input
        type="text"
        value={expression}
        onChange={(event) => setExpression(event.target.value)}
        placeholder="2d6+3 · d20"
        aria-label="Dice expression"
        disabled={rolling || disabled}
        className="w-28 rounded-md border border-zinc-300 px-3 py-2 text-sm disabled:opacity-50"
      />
      <input
        type="text"
        value={label}
        onChange={(event) => setLabel(event.target.value)}
        placeholder="Label (optional)"
        aria-label="Roll label"
        disabled={rolling || disabled}
        className="w-40 rounded-md border border-zinc-300 px-3 py-2 text-sm disabled:opacity-50"
      />
      {isGm && (
        <label className="flex items-center gap-1.5 text-sm text-zinc-600">
          <input
            type="checkbox"
            checked={privateRoll}
            onChange={(event) => setPrivateRoll(event.target.checked)}
            disabled={rolling || disabled}
          />
          GM private
        </label>
      )}
      <button
        type="submit"
        disabled={rolling || disabled || !expression.trim()}
        className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
      >
        {rolling ? 'Rolling…' : 'Roll'}
      </button>
      {error && (
        <p role="alert" className="w-full text-sm text-red-700">
          {error}
        </p>
      )}
    </form>
  )
}