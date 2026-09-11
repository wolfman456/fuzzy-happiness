import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import ThemedBackdrop from './ThemedBackdrop'

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/sessions', label: 'Sessions' },
  { to: '/characters', label: 'Characters' },
  { to: '/settings', label: 'Settings' },
]

export default function ShellLayout() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    if (!menuOpen) return
    const onKey = (event) => {
      if (event.key === 'Escape') setMenuOpen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [menuOpen])

  return (
    <ThemedBackdrop>
      <header className="sticky top-0 z-30 border-b border-zinc-200/60 bg-white/80 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <button
              type="button"
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={menuOpen}
              aria-controls="app-drawer"
              onClick={() => setMenuOpen((open) => !open)}
              className="rounded-md border border-zinc-300 bg-white px-2 py-1 text-base leading-none text-zinc-700 hover:bg-zinc-50"
            >
              {menuOpen ? '\u2715' : '\u2630'}
            </button>
            <Link to="/" className="text-lg font-semibold tracking-tight text-zinc-900">
              Tabletop
            </Link>
          </div>
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-zinc-600">
                {user.displayName}
                <span className="ml-1 text-zinc-400">({user.username})</span>
              </span>
            )}
            <button
              type="button"
              className="rounded-md border border-zinc-300 bg-white px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50"
              onClick={logout}
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      {menuOpen && (
        <>
          <button
            type="button"
            aria-label="Close menu overlay"
            onClick={() => setMenuOpen(false)}
            className="fixed inset-0 z-40 cursor-default bg-zinc-950/50"
          />
          <aside
            id="app-drawer"
            aria-label="Main menu"
            className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-zinc-200 bg-white p-4 shadow-xl"
          >
            <nav className="flex flex-col gap-1">
              {NAV.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  onClick={() => setMenuOpen(false)}
                  className={({ isActive }) =>
                    `rounded-md px-3 py-2 text-sm font-medium ${
                      isActive
                        ? 'bg-zinc-900 text-white'
                        : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </aside>
        </>
      )}

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
        <Outlet />
      </main>
    </ThemedBackdrop>
  )
}