import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api'

export default function VerifyPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [result, setResult] = useState(() => (token ? 'loading' : 'error'))
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!token) return
    let cancelled = false
    api(`/api/auth/verify?token=${encodeURIComponent(token)}`)
      .then(() => {
        if (!cancelled) setResult('success')
      })
      .catch((err) => {
        if (!cancelled) {
          setResult('error')
          setMessage(err.message)
        }
      })
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <div className="grid min-h-screen place-items-center bg-zinc-100 px-4">
      <div className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-8 text-center shadow-sm">
        {result === 'loading' && <p className="text-sm text-zinc-600">Verifying your email…</p>}
        {result === 'success' && (
          <>
            <h1 className="mb-3 text-xl font-semibold">Email verified</h1>
            <p className="text-sm text-zinc-600">You can now log in.</p>
            <Link to="/login" className="mt-5 inline-block text-sm font-medium text-zinc-900 underline">
              Go to log in
            </Link>
          </>
        )}
        {result === 'error' && (
          <>
            <h1 className="mb-3 text-xl font-semibold">Verification failed</h1>
            <p role="alert" className="text-sm text-red-700">
              {message || 'No verification token provided.'}
            </p>
            <Link to="/login" className="mt-5 inline-block text-sm font-medium text-zinc-900 underline">
              Back to log in
            </Link>
          </>
        )}
      </div>
    </div>
  )
}