import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, leaveSession } from '../lib/api'
import { createRealtimeClient } from '../lib/stomp'
import { useAuth } from '../auth/useAuth'
import { createMap, getMap, rollDice } from '../lib/battleMap'
import BattleMapPanel from '../components/BattleMapPanel'
import DiceTray from '../components/DiceTray'

const ROLE_LABEL = { GM: 'GM', PLAYER: 'Player', SPECTATOR: 'Spectator' }

export default function SessionPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const clientRef = useRef(null)
  const [session, setSession] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [events, setEvents] = useState([])
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [realtimeError, setRealtimeError] = useState('')
  const [leaving, setLeaving] = useState(false)
  const [map, setMap] = useState(null)
  const [mapState, setMapState] = useState('loading')
  const [mapError, setMapError] = useState('')
  const [fullRolls, setFullRolls] = useState({})

  useEffect(() => {
    let cancelled = false
    api(`/api/sessions/${id}`)
      .then((snapshot) => {
        if (cancelled) return
        setSession(snapshot)
        setEvents((snapshot.recentEvents ?? []).filter((event) => event.type !== 'TABLE'))
        const tableEvent = (snapshot.recentEvents ?? [])
          .filter((event) => event.type === 'TABLE' && event.payload?.tokens)
          .at(-1)
        if (tableEvent) {
          setMap(tableEvent.payload)
          setMapState('ready')
        }
      })
      .catch((error) => {
        if (cancelled) return
        setLoadError(error.message)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  useEffect(() => {
    let cancelled = false
    getMap(id)
      .then((data) => {
        if (cancelled) return
        setMap(data)
        setMapState('ready')
      })
      .catch((error) => {
        if (cancelled) return
        if (error.status === 404) {
          setMap(null)
          setMapState('none')
        } else {
          setMapError(error.message)
          setMapState('error')
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const onSnapshot = useCallback((snapshot) => {
    if (snapshot?.id) {
      setSession(snapshot)
      setEvents((snapshot.recentEvents ?? []).filter((event) => event.type !== 'TABLE'))
      const tableEvent = (snapshot.recentEvents ?? [])
        .filter((event) => event.type === 'TABLE' && event.payload?.tokens)
        .at(-1)
      if (tableEvent) {
        setMap(tableEvent.payload)
        setMapState('ready')
      }
    }
  }, [])

  const onEvent = useCallback((event) => {
    if (event?.type === 'TABLE') {
      if (event.payload?.tokens) {
        setMap(event.payload)
        setMapState('ready')
      }
      return
    }
    if (event?.type) setEvents((previous) => [...previous, event])
  }, [])

  const onError = useCallback((message) => {
    setRealtimeError(message ?? 'Connection lost')
  }, [])

  const onPrivateRoll = useCallback((event) => {
    const payload = event?.payload
    if (payload?.rollId) {
      setFullRolls((previous) => ({ ...previous, [payload.rollId]: payload }))
    }
  }, [])

  useEffect(() => {
    const client = createRealtimeClient({
      sessionId: id,
      onSnapshot,
      onEvent,
      onPrivateRoll,
      onError,
    })
    clientRef.current = client
    client.connect()
    return () => {
      client.disconnect()
      clientRef.current = null
    }
  }, [id, onEvent, onPrivateRoll, onError, onSnapshot])

  async function handleSend(event) {
    event.preventDefault()
    const text = draft.trim()
    if (!text || !clientRef.current) return
    setSending(true)
    try {
      clientRef.current.sendChat(text)
      setDraft('')
      setRealtimeError('')
    } finally {
      setSending(false)
    }
  }

  async function handleLeave() {
    setLeaving(true)
    try {
      await leaveSession(id)
      navigate('/sessions')
    } finally {
      setLeaving(false)
    }
  }

  async function handleRoll({ expression, label, privateRoll: isPrivate }) {
    const result = await rollDice(id, { expression, label, privateRoll: isPrivate })
    if (isPrivate && result?.payload?.rollId) {
      setFullRolls((previous) => ({ ...previous, [result.payload.rollId]: result.payload }))
    }
    setRealtimeError('')
    return result
  }

  async function handleCreateMap() {
    setMapError('')
    try {
      const created = await createMap(id)
      setMap(created)
      setMapState('ready')
    } catch (error) {
      setMapError(error.message)
      setMapState('error')
    }
  }

  if (loadError) {
    return (
      <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <p role="alert" className="text-sm text-red-700">
          {loadError}
        </p>
      </div>
    )
  }

  if (!session) {
    return <p className="text-sm text-zinc-500">Loading session…</p>
  }

  const isClosed = session.status === 'CLOSED'
  const me = session.participants?.find(
    (participant) => participant.user.id === user?.id,
  )
  const inviteCode = session.inviteCode

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">{session.name}</h1>
          <p className="text-sm text-zinc-500">
            {session.gameDisplayName} · {ROLE_LABEL[me?.role] ?? 'Player'} ·{' '}
            {session.status}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-zinc-900 px-3 py-1.5 font-mono text-sm tracking-widest text-white">
            {inviteCode}
          </div>
          <button
            type="button"
            onClick={handleLeave}
            disabled={leaving || isClosed}
            className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
          >
            {leaving ? 'Leaving…' : 'Leave'}
          </button>
        </div>
      </header>

      {isClosed && (
        <p className="rounded-md bg-zinc-100 px-3 py-2 text-sm text-zinc-600">
          This session has closed. Your game master can start a new one.
        </p>
      )}

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">At the table</h2>
        <ul className="mt-3 divide-y divide-zinc-100">
          {session.participants?.map((participant) => (
            <li
              key={participant.user.id}
              className="flex items-center justify-between py-2"
            >
              <span className="text-sm font-medium">
                {participant.user.displayName}
                <span className="ml-1 text-zinc-400">
                  ({participant.user.username})
                </span>
              </span>
              <span className="rounded bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-600">
                {ROLE_LABEL[participant.role] ?? participant.role}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Battle map</h2>
        <div className="mt-3">
          {mapState === 'loading' && (
            <p className="text-sm text-zinc-500">Loading battle map…</p>
          )}
          {mapState === 'none' &&
            (me?.role === 'GM' ? (
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-zinc-500">
                  No battle map yet for this session.
                </p>
                <button
                  type="button"
                  onClick={handleCreateMap}
                  className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
                >
                  Set up battle map
                </button>
              </div>
            ) : (
              <p className="text-sm text-zinc-500">
                No battle map yet — ask your GM to set one up.
              </p>
            ))}
          {mapState === 'error' && (
            <p role="alert" className="text-sm text-red-700">
              {mapError}
            </p>
          )}
          {mapState === 'ready' && map && (
            <BattleMapPanel
              sessionId={id}
              map={map}
              user={user}
              isGm={me?.role === 'GM'}
              disabled={isClosed}
              onMapChange={setMap}
            />
          )}
        </div>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Table chat</h2>
        {realtimeError && (
          <p role="alert" className="mt-2 text-sm text-amber-700">
            {realtimeError}
          </p>
        )}
        <ul className="mt-3 max-h-72 space-y-3 overflow-y-auto" data-testid="event-feed">
          {events.map((event, index) => {
            const fullRoll =
              event.type === 'DICE' && event.payload?.rollId
                ? fullRolls[event.payload.rollId]
                : undefined
            return (
              <EventRow key={`${event.id ?? index}`} event={event} fullRoll={fullRoll} />
            )
          })}
          {events.length === 0 && (
            <li className="text-sm text-zinc-400">No messages yet.</li>
          )}
        </ul>
        <DiceTray
          isGm={me?.role === 'GM'}
          disabled={isClosed}
          onRoll={handleRoll}
        />
        <form onSubmit={handleSend} className="mt-4 flex gap-2">
          <input
            type="text"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Type a message…"
            disabled={sending || isClosed}
            className="flex-1 rounded-md border border-zinc-300 px-3 py-2 text-sm disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={sending || isClosed}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            Send
          </button>
        </form>
      </section>
    </div>
  )
}

function EventRow({ event, fullRoll }) {
  if (event.type === 'PRESENCE') {
    const action = event.payload?.action === 'left' ? 'left' : 'joined'
    const name = event.payload?.sender?.displayName ?? 'Someone'
    return (
      <li className="text-xs text-zinc-400">
        {action === 'left' ? `${name} left the table.` : `${name} joined the table.`}
      </li>
    )
  }
  if (event.type === 'DICE') {
    const payload = event.payload ?? {}
    const name = payload.rolledBy?.displayName ?? 'Unknown'
    const isHidden = payload.hidden === true
    const revealed = isHidden ? fullRoll : payload
    return (
      <li className="flex flex-wrap items-baseline gap-x-1 text-sm text-zinc-700">
        <span className="font-medium text-zinc-800">{name} rolls</span>
        <span className="font-semibold">{payload.expression}</span>
        {payload.label && <span className="text-zinc-500">({payload.label})</span>}
        {isHidden ? (
          revealed ? (
            <span role="status" className="text-amber-700">
              secretly →
              {revealed.rolls?.join(', ')} = {revealed.total}
              <span className="ml-1 text-xs uppercase text-zinc-400">(GM only)</span>
            </span>
          ) : (
            <span className="text-zinc-400">(secret roll hidden from the table)</span>
          )
        ) : (
          <span role="status">
            → {payload.rolls?.join(', ')} = {payload.total}
          </span>
        )}
      </li>
    )
  }
  const sender = event.payload?.sender?.displayName ?? 'Unknown'
  const text = event.payload?.text ?? ''
  return (
    <li>
      <span className="text-sm font-medium text-zinc-800">{sender}: </span>
      <span className="text-sm text-zinc-700">{text}</span>
    </li>
  )
}