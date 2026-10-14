import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Card, PageHeading } from '../components/ui'

const ADMIN_USERS_URL = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/admin/users`

export default function Dashboard() {
  const { user } = useAuth()

  return (
    <div className="space-y-8">
      <Card>
        <PageHeading title={`Welcome, ${user.displayName}`} />
        <dl className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-zinc-500">Username</dt>
            <dd className="font-medium">{user.username}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Email</dt>
            <dd className="font-medium">{user.email}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Role</dt>
            <dd className="font-medium">{user.role}</dd>
          </div>
        </dl>
      </Card>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Sessions</h2>
          <p className="mt-1 text-sm text-zinc-500">Create or join a game room with an invite code.</p>
          <Link to="/sessions" className="mt-2 inline-block text-sm text-zinc-900 underline">
            Go to sessions
          </Link>
        </div>
        <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold">Characters</h2>
          <p className="mt-1 text-sm text-zinc-500">Build a D&D character sheet for your game.</p>
          <Link to="/characters" className="mt-2 inline-block text-sm text-zinc-900 underline">
            Build your characters
          </Link>
        </div>
      </section>

      {user.role === 'ADMIN' && (
        <Card>
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
        </Card>
      )}
    </div>
  )
}