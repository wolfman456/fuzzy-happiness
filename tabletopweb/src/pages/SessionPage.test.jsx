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

const battleMapMock = vi.hoisted(() => ({
  getMap: vi.fn(),
  createMap: vi.fn(),
  rollDice: vi.fn(),
}))

vi.mock('../lib/api', () => ({
  api: vi.fn(),
  getSession: vi.fn(),
  leaveSession: vi.fn(),
}))

vi.mock('../lib/stomp', () => ({
  createRealtimeClient: vi.fn().mockImplementation(({ sessionId, onSnapshot, onEvent, onPrivateRoll, onError }) => {
    realtime.handlers = { sessionId, onSnapshot, onEvent, onPrivateRoll, onError }
    return realtime
  }),
}))

vi.mock('../lib/battleMap', () => battleMapMock)

const monstersMock = vi.hoisted(() => ({
  generateMonster: vi.fn(),
  listMyMonsters: vi.fn().mockResolvedValue([]),
  MONSTER_CRS: ['1', '2', '3'],
  MONSTER_ROLES: ['AUTO', 'BALANCED'],
  MONSTER_EDITIONS: ['SRD_2014', 'SRD_2024'],
}))

vi.mock('../lib/monsters', () => monstersMock)

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
  beforeEach(() => {
    battleMapMock.getMap.mockReset()
    battleMapMock.getMap.mockRejectedValue({ status: 404, message: 'Battle map not found' })
    battleMapMock.createMap.mockReset()
    battleMapMock.createMap.mockResolvedValue({ id: 1, sessionId: 7, name: 'Grumm’s map', width: 24, height: 18, squareFeet: 10, currentTurnTokenId: null, tokens: [] })
    battleMapMock.rollDice.mockReset()
    monstersMock.generateMonster.mockReset()
    monstersMock.listMyMonsters.mockReset()
    monstersMock.listMyMonsters.mockResolvedValue([])
  })

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

  it('renders public dice rolls into the feed', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    act(() => {
      realtime.handlers.onEvent({
        id: null,
        type: 'DICE',
        payload: {
          rollId: 'r-1',
          rolledBy: { displayName: 'Ivo' },
          expression: '2d6+3',
          label: 'Perception',
          rolls: [3, 5],
          total: 11,
          hidden: false,
        },
        createdAt: 'y',
      })
    })

    expect(await screen.findByText(/Ivo rolls/)).toBeInTheDocument()
    expect(screen.getByText('2d6+3')).toBeInTheDocument()
    expect(screen.getByText('(Perception)')).toBeInTheDocument()
    expect(screen.getByText('→ 3, 5 = 11')).toBeInTheDocument()
  })

  it('hides the result of a GM-private roll from players', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    act(() => {
      realtime.handlers.onEvent({
        id: null,
        type: 'DICE',
        payload: {
          rollId: 'secret-1',
          rolledBy: { displayName: 'Ginger' },
          expression: 'd20',
          label: 'Stealth',
          hidden: true,
        },
        createdAt: 'y',
      })
    })

    expect(await screen.findByText('Ginger rolls')).toBeInTheDocument()
    expect(screen.getByText('d20')).toBeInTheDocument()
    expect(screen.getByText('d20').closest('li')).toHaveTextContent('secret roll hidden')
    expect(screen.queryByText(/(GM only)/)).not.toBeInTheDocument()
  })

  it('reveals the GM-private result to the GM via the user queue', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    act(() => {
      realtime.handlers.onEvent({
        id: null,
        type: 'DICE',
        payload: {
          rollId: 'secret-1',
          rolledBy: { displayName: 'Ginger' },
          expression: 'd20',
          label: 'Stealth',
          hidden: true,
        },
        createdAt: 'y',
      })
      realtime.handlers.onPrivateRoll({
        type: 'DICE',
        payload: {
          rollId: 'secret-1',
          rolledBy: { displayName: 'Ginger' },
          expression: 'd20',
          label: 'Stealth',
          rolls: [14],
          total: 14,
          hidden: true,
        },
      })
    })

    expect(await screen.findByText('Ginger rolls')).toBeInTheDocument()
    expect(screen.getByText(/(GM only)/)).toBeInTheDocument()
    expect(screen.getByText(/secretly →14 = 14/)).toBeInTheDocument()
  })

  it('posts a roll from the dice tray and clears the form', async () => {
    api.mockResolvedValue(snapshot)
    battleMapMock.rollDice.mockResolvedValue({ id: null, type: 'DICE', payload: { rollId: 'r-2' } })
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    fireEvent.change(screen.getByLabelText('Dice expression'), { target: { value: '2d6+3' } })
    fireEvent.change(screen.getByLabelText('Roll label'), { target: { value: 'Insight' } })
    fireEvent.click(screen.getByRole('button', { name: 'Roll' }))

    await waitFor(() =>
      expect(battleMapMock.rollDice).toHaveBeenCalledWith('7', {
        expression: '2d6+3',
        label: 'Insight',
        privateRoll: false,
      }),
    )
    expect(screen.getByLabelText('Dice expression')).toHaveValue('')
    expect(screen.getByLabelText('Roll label')).toHaveValue('')
  })

  it('seeds the GM-visible result when the GM rolls privately', async () => {
    api.mockResolvedValue(snapshot)
    battleMapMock.rollDice.mockResolvedValue({
      id: null,
      type: 'DICE',
      payload: {
        rollId: 'secret-9',
        rolledBy: { displayName: 'Ginger' },
        expression: 'd100',
        rolls: [47],
        total: 47,
        hidden: true,
      },
    })
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    fireEvent.click(screen.getByLabelText('GM private'))
    fireEvent.change(screen.getByLabelText('Dice expression'), { target: { value: 'd100' } })
    fireEvent.click(screen.getByRole('button', { name: 'Roll' }))

    await waitFor(() =>
      expect(battleMapMock.rollDice).toHaveBeenCalledWith('7', {
        expression: 'd100',
        label: undefined,
        privateRoll: true,
      }),
    )

    act(() => {
      realtime.handlers.onEvent({
        id: null,
        type: 'DICE',
        payload: {
          rollId: 'secret-9',
          rolledBy: { displayName: 'Ginger' },
          expression: 'd100',
          hidden: true,
        },
        createdAt: 'y',
      })
    })

    expect(await screen.findByText(/secretly →.+47/)).toBeInTheDocument()
  })

  it('hides the GM-private toggle from players', async () => {
    api.mockResolvedValue({
      ...snapshot,
      participants: [
        { user: { id: 2, username: 'ivo', displayName: 'Ivo' }, role: 'PLAYER', joinedAt: '2026-01-01T10:05:00' },
      ],
    })
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    expect(screen.queryByLabelText('GM private')).not.toBeInTheDocument()
  })

  it('surfaces roll errors from the dice tray', async () => {
    api.mockResolvedValue(snapshot)
    battleMapMock.rollDice.mockRejectedValue(new Error('Unsupported dice expression'))
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    fireEvent.change(screen.getByLabelText('Dice expression'), { target: { value: 'xx' } })
    fireEvent.click(screen.getByRole('button', { name: 'Roll' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unsupported dice expression')
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

  it('renders the battle map from a live TABLE event kept out of the chat feed', async () => {
    api.mockResolvedValue(snapshot)
    battleMapMock.getMap.mockResolvedValue({
      id: 1, sessionId: 7, name: 'Live map', width: 24, height: 18, squareFeet: 10, currentTurnTokenId: null, tokens: [],
    })
    renderSession()
    await screen.findByRole('heading', { name: /Live map/ })

    act(() => {
      realtime.handlers.onEvent({
        id: 50,
        type: 'TABLE',
        payload: {
          id: 1,
          sessionId: 7,
          name: 'Live map',
          width: 24,
          height: 18,
          squareFeet: 10,
          currentTurnTokenId: null,
          tokens: [
            { id: 11, name: 'Goblin', category: 'MONSTER_NPC', color: '#ef4444', speedFeet: 30, posX: 2, posY: 2, movedFeet: 0, linkedParticipantId: null, linkedUserId: null },
          ],
        },
      })
    })

    expect(await screen.findByRole('button', { name: /Token Goblin/ })).toBeInTheDocument()
    expect(screen.getByText('No messages yet.')).toBeInTheDocument()
  })

  it('hydrates the map from snapshot recentEvents and hides TABLE rows from the feed', async () => {
    api.mockResolvedValue({
      ...snapshot,
      recentEvents: [
        { id: 1, type: 'CHAT', payload: { sender: { displayName: 'Ginger' }, text: 'Hi' }, createdAt: 'x' },
        { id: 2, type: 'TABLE', payload: { id: 1, sessionId: 7, name: 'Snapshot map', width: 24, height: 18, squareFeet: 10, currentTurnTokenId: null, tokens: [] }, createdAt: 'y' },
      ],
    })
    battleMapMock.getMap.mockResolvedValue({
      id: 1, sessionId: 7, name: 'Snapshot map', width: 24, height: 18, squareFeet: 10, currentTurnTokenId: null, tokens: [],
    })
    renderSession()

    expect(await screen.findByRole('heading', { name: /Snapshot map/ })).toBeInTheDocument()
    expect(screen.getByText('Hi')).toBeInTheDocument()
    expect(screen.queryByText('No messages yet.')).not.toBeInTheDocument()
  })

  it('lets the GM set up a battle map when none exists', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()

    const setup = await screen.findByRole('button', { name: 'Set up battle map' })
    fireEvent.click(setup)

    expect(await screen.findByRole('heading', { name: /Grumm’s map/ })).toBeInTheDocument()
    expect(battleMapMock.createMap).toHaveBeenCalledWith('7')
  })

  it('tells players to ask their GM when no map exists', async () => {
    api.mockResolvedValue({
      ...snapshot,
      participants: [
        { user: { id: 1, username: 'ginger', displayName: 'Ginger' }, role: 'PLAYER', joinedAt: '2026-01-01T10:00:00' },
      ],
    })
    renderSession()

    expect(await screen.findByText(/ask your GM/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Set up battle map' })).not.toBeInTheDocument()
  })

  it('shows the monster generator to the GM', async () => {
    api.mockResolvedValue(snapshot)
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    expect(screen.getByTestId('monster-generator')).toBeInTheDocument()
  })

  it('hides the monster generator from players', async () => {
    api.mockResolvedValue({
      ...snapshot,
      participants: [
        { user: { id: 2, username: 'ivo', displayName: 'Ivo' }, role: 'PLAYER', joinedAt: '2026-01-01T10:05:00' },
      ],
    })
    renderSession()
    await screen.findByRole('heading', { name: 'Grumm’s Revenge' })

    expect(screen.queryByTestId('monster-generator')).not.toBeInTheDocument()
  })
})