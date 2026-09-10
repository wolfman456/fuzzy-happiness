import { useState } from 'react'
import { useAuth } from '../auth/useAuth'
import { updateUsername } from '../lib/api'

export default function SettingsPage() {
  const { user, refreshMe } = useAuth()
  const [username, setUsername] = useState('')
  const [fieldError, setFieldError] = useState(null)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    setUsername(event.target.value)
    setError(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    const next = username.trim()
    if (!/^.{3,30}$/.test(next)) {
      setFieldError('Username must be 3–30 characters.')
      return
    }
    setFieldError(null)
    setSubmitting(true)
    try {
      await updateUsername(next)
      await refreshMe()
      setSuccess(`Username changed to ${next}.`)
      setUsername('')
    } catch (err) {
      setError(err.status === 409 ? 'That username is already taken.' : err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-8">
      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold">Settings</h1>
        <div className="mt-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4">
          <h2 className="font-semibold">Account</h2>
          <dl className="mt-3 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-zinc-500">Username</dt>
              <dd className="font-medium">{user.username}</dd>
            </div>
            <div>
              <dt className="text-zinc-500">Email</dt>
              <dd className="font-medium">{user.email}</dd>
            </div>
          </dl>
        </div>
        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <label className="block text-sm font-medium" htmlFor="username">
            New username
            <input
              id="username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={handleChange}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
            <span className="mt-1 block text-xs text-zinc-500">
              3–30 characters. You&apos;ll sign in with this going forward.
            </span>
          </label>
          {fieldError && <p className="text-xs text-red-600">{fieldError}</p>}
          {error && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
              {error}
            </p>
          )}
          {success && (
            <p role="status" className="rounded-md bg-green-50 p-3 text-sm text-green-700">
              {success}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {submitting ? 'Saving…' : 'Change username'}
          </button>
        </form>
      </section>
    </div>
  )
}