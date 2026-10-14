import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createSession, joinSession, listGames } from '../lib/api'
import { Alert, Card, PageHeading } from '../components/ui'

export default function LobbyPage() {
  const navigate = useNavigate()
  const [games, setGames] = useState([])
  const [gamesError, setGamesError] = useState('')
  const [formError, setFormError] = useState('')
  const [creating, setCreating] = useState(false)
  const [joining, setJoining] = useState(false)
  const [name, setName] = useState('')
  const [gameSlug, setGameSlug] = useState('')
  const [inviteCode, setInviteCode] = useState('')

  useEffect(() => {
    listGames()
      .then((loaded) => {
        setGames(loaded)
        setGameSlug((current) => current || loaded[0]?.slug)
      })
      .catch(() => setGamesError('Could not load available games'))
  }, [])

  async function handleCreate(event) {
    event.preventDefault()
    setFormError('')
    setCreating(true)
    try {
      const session = await createSession({ name, gameSlug })
      navigate(`/sessions/${session.id}`)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setCreating(false)
    }
  }

  async function handleJoin(event) {
    event.preventDefault()
    setFormError('')
    setJoining(true)
    try {
      const session = await joinSession(inviteCode.trim())
      navigate(`/sessions/${session.id}`)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setJoining(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeading
        tone="light"
        title="Game sessions"
        subtitle="Create a room to game master, or join a friend's table with their invite code."
      />

      {formError && <Alert tone="error">{formError}</Alert>}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Card>
          <h2 className="text-lg font-semibold">Create a session</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Start a new room as the game master.
          </p>
          <form onSubmit={handleCreate} className="mt-4 space-y-3">
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Session name</span>
              <input
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Grumm's Revenge"
                required
                maxLength={100}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Game</span>
              <select
                value={gameSlug}
                onChange={(event) => setGameSlug(event.target.value)}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm"
              >
                {gamesError ? (
                  <option value="">{gamesError}</option>
                ) : (
                  games.map((game) => (
                    <option key={game.slug} value={game.slug}>
                      {game.displayName}
                    </option>
                  ))
                )}
              </select>
            </label>
            <button
              type="submit"
              disabled={creating || !gameSlug}
              className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
            >
              {creating ? 'Creating…' : 'Create session'}
            </button>
          </form>
        </Card>

        <Card>
          <h2 className="text-lg font-semibold">Join a session</h2>
          <p className="mt-1 text-sm text-zinc-500">
            Enter the invite code shared by a game master.
          </p>
          <form onSubmit={handleJoin} className="mt-4 space-y-3">
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Invite code</span>
              <input
                type="text"
                value={inviteCode}
                onChange={(event) => setInviteCode(event.target.value)}
                placeholder="AB12CD"
                required
                maxLength={6}
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 font-mono uppercase text-sm tracking-widest"
              />
            </label>
            <button
              type="submit"
              disabled={joining}
              className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
            >
              {joining ? 'Joining…' : 'Join session'}
            </button>
          </form>
        </Card>
      </div>
    </div>
  )
}