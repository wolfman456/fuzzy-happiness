import { useAuth } from '../auth/useAuth'

const ADMIN_USERS_URL = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/admin/users`

export default function Dashboard() {
  const { user } = useAuth()

  return (
    <div className="space-y-8">
      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold">Welcome, {user.displayName}</h1>
        <dl className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-zinc-500">Username</dt>
            <dd className="font-medium">{user.username}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Email</dt>
            <dd className="font-medium">
              {user.email}{' '}
              {user.emailVerified ? (
                <span className="ml-1 rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700">verified</span>
              ) : (
                <span className="ml-1 rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-700">unverified</span>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-zinc-500">Role</dt>
            <dd className="font-medium">{user.role}</dd>
          </div>
        </dl>
      </section>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Sessions</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Create or join a game room with an invite code. Coming soon.
          </p>
        </div>
        <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Characters</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Build a character sheet for your game. Coming soon.
          </p>
        </div>
      </section>

      {user.role === 'ADMIN' && (
        <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Administration</h2>
          <a
            href={ADMIN_USERS_URL}
            target="_blank"
            rel="noreferrer"
            className="mt-2 inline-block text-sm text-zinc-900 underline"
          >
            User listing (backend JSON)
          </a>
          <p className="mt-1 text-xs text-zinc-500">
            Raw endpoint until an admin UI is built.
          </p>
        </section>
      )}
    </div>
  )
}