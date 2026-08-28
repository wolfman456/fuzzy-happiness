import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export default function ShellLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-zinc-100">
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <nav className="flex items-center gap-6">
            <Link to="/" className="text-lg font-semibold tracking-tight">
              Tabletop
            </Link>
            <Link
              to="/sessions"
              className="text-sm text-zinc-500 hover:text-zinc-900"
              aria-disabled="true"
              onClick={(e) => e.preventDefault()}
              title="Coming soon"
            >
              Sessions
            </Link>
            <Link
              to="/characters"
              className="text-sm text-zinc-500 hover:text-zinc-900"
              aria-disabled="true"
              onClick={(e) => e.preventDefault()}
              title="Coming soon"
            >
              Characters
            </Link>
          </nav>
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-zinc-600">
                {user.displayName}
                <span className="ml-1 text-zinc-400">({user.username})</span>
              </span>
            )}
            <button
              type="button"
              className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50"
              onClick={logout}
            >
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}