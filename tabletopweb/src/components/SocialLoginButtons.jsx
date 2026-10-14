import { useEffect, useState } from 'react'
import { apiBaseUrl, fetchProviders } from '../lib/api'

export default function SocialLoginButtons() {
  const [providers, setProviders] = useState([])

  useEffect(() => {
    let active = true
    fetchProviders()
      .then((list) => {
        if (active) setProviders(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (active) setProviders([])
      })
    return () => {
      active = false
    }
  }, [])

  if (providers.length === 0) {
    return null
  }

  return (
    <div className="mt-6" data-testid="social-login">
      <div className="mb-4 flex items-center gap-3 text-xs uppercase tracking-wide text-zinc-500">
        <span className="h-px flex-1 bg-zinc-300" />
        or continue with
        <span className="h-px flex-1 bg-zinc-300" />
      </div>
      <div className="space-y-2">
        {providers.map((provider) => (
          <a
            key={provider.id}
            href={`${apiBaseUrl}${provider.authorizationPath}`}
            data-testid={`social-${provider.id}`}
            className="block w-full rounded-md border border-zinc-300 px-3 py-2 text-center text-sm font-medium text-zinc-900 hover:bg-zinc-200"
          >
            Continue with {provider.label}
          </a>
        ))}
      </div>
    </div>
  )
}
