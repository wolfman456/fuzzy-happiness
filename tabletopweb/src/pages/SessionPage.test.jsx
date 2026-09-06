import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import SessionPage from './SessionPage'

const realtime = {
  connect: vi.fn(),
  disconnect: vi.fn(),
  sendChat: vi.fn(),
  handlers: {},
}

vi.mock('../lib/api', () => ({
  api: vi.fn(),
  getSession: vi.fn(),
  leaveSession: vi.fn(),
}))

vi.mock('../lib/stomp', () => ({
  createRealtimeClient: vi.fn().mockImplementation(({ sessionId, onSnapshot, onEvent, onError }) => {
    realtime.handlers = { sessionId, onSnapshot, onEvent, onError }
    return realtime
  }),
}))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ user: { id: 1, username: 'ginger', displayName: 'Ginger' } }),
}))

import { api, leaveSession } from '../lib/api'

function SessionProbe() {
  const { id } = useParams()
  return <span data-testid="location">{id ? `/sessions/${id}` : '/sessions'}</span>
}

const snapshot = {
  id: 7,
  name: 'Grumm’s Revenge',
  inviteCode: 'AB12CD',
  gameSlug: 'dnd-5e',
  gameDisplayName: 'D&D 5e',
  status: 'OPEN',
  createdBy: { id: 1, username: 'ginger', displayName: 'Ginger' },
  participants: [
    { user: { id: 1, username: 'ginger', displayName: 'Ginger' }, role: 'GM', joinedAt: '2026-01-01T10:00:00' },
    { user: { id: 2, username: 'ivo', displayName: 'Ivo' }, role: 'PLAYER', joinedAt: '2026-01-01T10:05:00' },
  ],
  recentEvents: [],
}

function renderSession(initialEntry = '/sessions/7') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/sessions/:id" element={<SessionPage />} />
        <Route path="/sessions" element={<SessionProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('SessionPage', () => {
  it('renders participants, invite code and connects realtime', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()

    expect(await screen.findByRole('heading', { name: 'Grumm’s Revenge' })).toBeInTheDocument()
    expect(screen.getByText('AB12CD')).toBeInTheDocument()
    expect(screen.getByText('Ginger')).toBeInTheDocument()
    expect(screen.getByText('Ivo')).toBeInTheDocument()
    await waitFor(() => expect(realtime.connect).toHaveBeenCalled())
  })

  it('seeds the feed with recent events from the snapshot', async () => {
    api.mockResolvedValue({
      ...snapshot,
      recentEvents: [
        { id: 1, type: 'CHAT', payload: { sender: { displayName: 'Ginger' }, text: 'Welcome!' }, createdAt: 'x' },
      ],
    })
    renderSession()

    expect(await screen.findByText('Welcome!')).toBeInTheDocument()
  })

  it('appends a live chat event and renders presence lines', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    // Live chat event arrives from the topic subscription
    const { onEvent } = realtime.handlers
    onEvent({ id: 9, type: 'CHAT', payload: { sender: { displayName: 'Ivo' }, text: 'Rolling!' }, createdAt: 'y' })
    onEvent({ id: 10, type: 'PRESENCE', payload: { sender: { displayName: 'Ivo' }, action: 'joined' }, createdAt: 'z' })

    expect(await screen.findByText('Rolling!')).toBeInTheDocument()
    expect(screen.getByText('Ivo joined the table.')).toBeInTheDocument()
  })

  it('sends chat over STOMP on submit', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    fireEvent.change(screen.getByPlaceholderText('Type a message…'), { target: { value: 'Hello table!' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() => expect(realtime.sendChat).toHaveBeenCalledWith('Hello table!'))
    expect(screen.getByPlaceholderText('Type a message…')).toHaveValue('')
  })

  it('leave posts to the API and navigates back to the lobby', async () => {
    api.mockResolvedValue(snapshot)
    leaveSession.mockResolvedValue('You have left the session')
    renderSession()
    await screen.findByRole('button', { name: 'Leave' })

    fireEvent.click(screen.getByRole('button', { name: 'Leave' }))

    await waitFor(() => expect(leaveSession).toHaveBeenCalledWith('7'))
    expect(await screen.findByTestId('location')).toHaveTextContent('/sessions')
  })

  it('shows a closed banner for closed sessions', async () => {
    api.mockResolvedValue({ ...snapshot, status: 'CLOSED' })
    renderSession()

    expect(await screen.findByText(/has closed/i)).toBeInTheDocument()
  })

  it('surfaces load errors', async () => {
    api.mockRejectedValue(new Error('You are not a participant of this session'))
    renderSession()

    expect(await screen.findByRole('alert')).toHaveTextContent('not a participant')
  })

  it('reports realtime errors', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    act(() => {
      realtime.handlers.onError?.('You are not a participant of this session')
    })

    expect(screen.getByRole('alert')).toHaveTextContent('not a participant')
  })
})