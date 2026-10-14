import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import AuthBackground from '../components/AuthBackground'

function readToken() {
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ''))
  return params.get('token')
}

export default function OAuthCallbackPage() {
  const { loginWithToken, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState(() => (readToken() ? null : 'Sign-in did not return a token.'))
  const exchanged = useRef(false)

  useEffect(() => {
    if (exchanged.current) return
    exchanged.current = true
    const token = readToken()
    if (!token) return
    loginWithToken(token)
      .then(() => navigate('/', { replace: true }))
      .catch(() => setError('That sign-in link is no longer valid. Please try again.'))
  }, [loginWithToken, navigate])

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return (
    <AuthBackground>
      <div className="mx-auto w-full max-w-sm rounded-xl border border-zinc-700 bg-zinc-100 p-8 text-center shadow-xl">
        <h1 className="mb-3 text-xl font-semibold">Signing you in…</h1>
        {error ? (
          <p role="alert" className="text-sm text-red-700">
            {error}{' '}
            <Link to="/login" className="underline">
              Back to log in
            </Link>
          </p>
        ) : (
          <p className="text-sm text-zinc-600">One moment while we finish connecting your account.</p>
        )}
      </div>
    </AuthBackground>
  )
}
