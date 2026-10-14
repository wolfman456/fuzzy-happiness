import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import AuthBackground from '../components/AuthBackground'
import SocialLoginButtons from '../components/SocialLoginButtons'

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const oauthMessage = searchParams.get('oauthMessage')

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login({ identifier, password })
      navigate('/', { replace: true })
    } catch (err) {
      setError(
        err.status === 401 ? 'Invalid username/email or password.' : err.message,
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthBackground>
      <div className="mx-auto w-full max-w-sm rounded-xl border border-zinc-700 bg-zinc-100 p-8 shadow-xl">
        <h1 className="mb-6 text-2xl font-semibold tracking-tight">Log in</h1>
        {error || oauthMessage ? (
          <p role="alert" className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">
            {error ?? oauthMessage}
          </p>
        ) : null}
        <form onSubmit={handleSubmit} className="space-y-4">
          <label className="block text-sm font-medium" htmlFor="identifier">
            Username or email
            <input
              id="identifier"
              type="text"
              autoComplete="username"
              required
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </label>
          <label className="block text-sm font-medium" htmlFor="password">
            Password
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-md bg-zinc-900 px-3 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>
        <SocialLoginButtons />
        <p className="mt-4 text-sm text-zinc-600">
          No account yet?{' '}
          <Link to="/register" className="text-zinc-900 underline">
            Register
          </Link>
        </p>
      </div>
    </AuthBackground>
  )
}