import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/
const MIN_AGE_MS = 13 * 365.25 * 24 * 60 * 60 * 1000

function isAdult(dateOfBirth) {
  const dob = new Date(`${dateOfBirth}T00:00:00`)
  return !Number.isNaN(dob.getTime()) && Date.now() - dob.getTime() >= MIN_AGE_MS
}

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const [form, setForm] = useState({
    displayName: '',
    email: '',
    dateOfBirth: '',
    username: '',
    password: '',
  })
  const [fields, setFields] = useState({})
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function validate() {
    const nextFields = {}
    if (!form.displayName.trim()) nextFields.displayName = 'Display name is required.'
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email)) nextFields.email = 'Enter a valid email address.'
    if (!form.dateOfBirth) nextFields.dateOfBirth = 'Date of birth is required.'
    else if (!isAdult(form.dateOfBirth)) nextFields.dateOfBirth = 'You must be at least 13 years old.'
    if (!/^.{3,30}$/.test(form.username)) nextFields.username = 'Username must be 3–30 characters.'
    if (!PASSWORD_PATTERN.test(form.password)) {
      nextFields.password = 'Password needs 8+ characters with an upper, lower, digit and symbol.'
    }
    setFields(nextFields)
    return Object.keys(nextFields).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      await register(form)
      setDone(true)
    } catch (err) {
      if (err.status === 409) {
        setFields({ ...fields, ...(err.message.includes('username') ? { username: err.message } : { email: err.message }) })
      } else {
        setFormError(err.message)
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <div className="grid min-h-screen place-items-center bg-zinc-100 px-4">
        <div className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
          <h1 className="mb-3 text-xl font-semibold">Check your inbox</h1>
          <p className="text-sm text-zinc-600">
            A verification link was sent to <strong>{form.email}</strong>. You can log in
            once your email is confirmed (in dev the link is printed to the backend console).
          </p>
          <Link to="/login" className="mt-5 inline-block text-sm font-medium text-zinc-900 underline">
            Go to log in
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="grid min-h-screen place-items-center bg-zinc-100 px-4 py-8">
      <div className="w-full max-w-md rounded-xl border border-zinc-200 bg-white p-8 shadow-sm">
        <h1 className="mb-6 text-2xl font-semibold tracking-tight">Create account</h1>
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <Field label="Display name" id="displayName" error={fields.displayName}>
            <input
              id="displayName"
              type="text"
              autoComplete="name"
              value={form.displayName}
              onChange={(e) => set('displayName', e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </Field>
          <Field label="Email" id="email" error={fields.email}>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={(e) => set('email', e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </Field>
          <Field label="Date of birth" id="dateOfBirth" error={fields.dateOfBirth}>
            <input
              id="dateOfBirth"
              type="date"
              value={form.dateOfBirth}
              onChange={(e) => set('dateOfBirth', e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </Field>
          <Field label="Username" id="username" error={fields.username}>
            <input
              id="username"
              type="text"
              autoComplete="username"
              value={form.username}
              onChange={(e) => set('username', e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </Field>
          <Field label="Password" id="password" error={fields.password}>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(e) => set('password', e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
            <span className="mt-1 block text-xs text-zinc-500">
              At least 8 characters with an uppercase letter, lowercase letter, digit and symbol.
            </span>
          </Field>
          {formError && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
              {formError}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="mt-4 text-sm text-zinc-600">
          Already have an account?{' '}
          <Link to="/login" className="text-zinc-900 underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}

function Field({ label, id, error, children }) {
  return (
    <label className="block text-sm font-medium" htmlFor={id}>
      {label}
      {children}
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}