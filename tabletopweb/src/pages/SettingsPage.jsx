import { useState } from 'react'
import { useAuth } from '../auth/useAuth'
import { changePassword, updateProfile, updateUsername } from '../lib/api'

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/
const PASSWORD_HINT = 'Password needs 8+ characters with an upper, lower, digit and symbol.'

const inputClass =
  'mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500'
const buttonClass =
  'rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50'

export default function SettingsPage() {
  const { user, refreshMe } = useAuth()
  const [username, setUsername] = useState('')
  const [fieldError, setFieldError] = useState(null)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const [displayName, setDisplayName] = useState(user.displayName ?? '')
  const [realName, setRealName] = useState(user.realName ?? '')
  const [profileError, setProfileError] = useState(null)
  const [profileSuccess, setProfileSuccess] = useState(null)
  const [profileSubmitting, setProfileSubmitting] = useState(false)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [pwFieldError, setPwFieldError] = useState(null)
  const [pwError, setPwError] = useState(null)
  const [pwSuccess, setPwSuccess] = useState(null)
  const [pwSubmitting, setPwSubmitting] = useState(false)

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

  async function handleProfileSubmit(event) {
    event.preventDefault()
    setProfileError(null)
    setProfileSuccess(null)
    if (!displayName.trim() || !realName.trim()) {
      setProfileError('Display name and real name are required.')
      return
    }
    setProfileSubmitting(true)
    try {
      const trimmedDisplayName = displayName.trim()
      const trimmedRealName = realName.trim()
      await updateProfile({ displayName: trimmedDisplayName, realName: trimmedRealName })
      await refreshMe()
      setDisplayName(trimmedDisplayName)
      setRealName(trimmedRealName)
      setProfileSuccess('Profile updated.')
    } catch (err) {
      setProfileError(err.message)
    } finally {
      setProfileSubmitting(false)
    }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setPwError(null)
    setPwSuccess(null)
    if (!PASSWORD_PATTERN.test(newPassword)) {
      setPwFieldError(PASSWORD_HINT)
      return
    }
    if (newPassword !== confirmPassword) {
      setPwFieldError('Passwords do not match.')
      return
    }
    setPwFieldError(null)
    setPwSubmitting(true)
    try {
      await changePassword({ currentPassword, newPassword, confirmPassword })
      setPwSuccess('Password changed. Use the new one the next time you sign in.')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      setPwError(err.message)
    } finally {
      setPwSubmitting(false)
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
              className={inputClass}
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
          <button type="submit" disabled={submitting} className={buttonClass}>
            {submitting ? 'Saving…' : 'Change username'}
          </button>
        </form>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 className="font-semibold">Edit profile</h2>
        <p className="mt-1 text-sm text-zinc-500">
          Update how your name appears to other players. Your email and date of birth stay the same.
        </p>
        <form onSubmit={handleProfileSubmit} className="mt-4 space-y-3">
          <label className="block text-sm font-medium" htmlFor="displayName">
            Display name
            <input
              id="displayName"
              type="text"
              autoComplete="name"
              value={displayName}
              onChange={(event) => {
                setDisplayName(event.target.value)
                setProfileError(null)
              }}
              className={inputClass}
            />
          </label>
          <label className="block text-sm font-medium" htmlFor="realName">
            Real name
            <input
              id="realName"
              type="text"
              autoComplete="name"
              value={realName}
              onChange={(event) => {
                setRealName(event.target.value)
                setProfileError(null)
              }}
              className={inputClass}
            />
          </label>
          {profileError && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
              {profileError}
            </p>
          )}
          {profileSuccess && (
            <p role="status" className="rounded-md bg-green-50 p-3 text-sm text-green-700">
              {profileSuccess}
            </p>
          )}
          <button type="submit" disabled={profileSubmitting} className={buttonClass}>
            {profileSubmitting ? 'Saving…' : 'Save profile'}
          </button>
        </form>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 className="font-semibold">Change password</h2>
        <p className="mt-1 text-sm text-zinc-500">
          Choose a new password. Your current password is required to make the change.
        </p>
        <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-3">
          <label className="block text-sm font-medium" htmlFor="currentPassword">
            Current password
            <input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => {
                setCurrentPassword(event.target.value)
                setPwError(null)
              }}
              className={inputClass}
            />
          </label>
          <label className="block text-sm font-medium" htmlFor="newPassword">
            New password
            <input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => {
                setNewPassword(event.target.value)
                setPwError(null)
              }}
              className={inputClass}
            />
            <span className="mt-1 block text-xs text-zinc-500">{PASSWORD_HINT}</span>
          </label>
          <label className="block text-sm font-medium" htmlFor="confirmPassword">
            Confirm new password
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => {
                setConfirmPassword(event.target.value)
                setPwError(null)
              }}
              className={inputClass}
            />
          </label>
          {pwFieldError && <p className="text-xs text-red-600">{pwFieldError}</p>}
          {pwError && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
              {pwError}
            </p>
          )}
          {pwSuccess && (
            <p role="status" className="rounded-md bg-green-50 p-3 text-sm text-green-700">
              {pwSuccess}
            </p>
          )}
          <button type="submit" disabled={pwSubmitting} className={buttonClass}>
            {pwSubmitting ? 'Saving…' : 'Change password'}
          </button>
        </form>
      </section>
    </div>
  )
}