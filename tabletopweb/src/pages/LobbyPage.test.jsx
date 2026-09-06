import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import LobbyPage from './LobbyPage'

vi.mock('../lib/api', () => ({
  listGames: vi.fn(),
  createSession: vi.fn(),
  joinSession: vi.fn(),
}))

import { createSession, joinSession, listGames } from '../lib/api'

function SessionProbe() {
  const { id } = useParams()
  return <span data-testid="location">/sessions/{id}</span>
}

function renderLobby() {
  return render(
    <MemoryRouter initialEntries={['/sessions']}>
      <Routes>
        <Route path="/sessions" element={<LobbyPage />} />
        <Route path="/sessions/:id" element={<SessionProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LobbyPage', () => {
  it('loads games into the create form', async () => {
    listGames.mockResolvedValue([
      { slug: 'dnd-5e', displayName: 'D&D 5e' },
      { slug: 'fate', displayName: 'Fate Core' },
    ])
    renderLobby()

    const select = await screen.findByLabelText('Game')
    expect(select).toHaveValue('dnd-5e')
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['D&D 5e', 'Fate Core'])
  })

  it('creates a session and navigates to it', async () => {
    listGames.mockResolvedValue([{ slug: 'dnd-5e', displayName: 'D&D 5e' }])
    createSession.mockResolvedValue({ id: 42, name: 'Grumm’s Revenge' })
    renderLobby()

    await screen.findByLabelText('Game')
    fireEvent.change(screen.getByLabelText('Session name'), { target: { value: 'Grumm’s Revenge' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create session' }))

    await waitFor(() => expect(createSession).toHaveBeenCalledWith({ name: 'Grumm’s Revenge', gameSlug: 'dnd-5e' }))
    expect(await screen.findByTestId('location')).toHaveTextContent('/sessions/42')
  })

  it('joins a session by invite code and navigates to it', async () => {
    listGames.mockResolvedValue([{ slug: 'dnd-5e', displayName: 'D&D 5e' }])
    joinSession.mockResolvedValue({ id: 13, inviteCode: 'AB12CD' })
    renderLobby()

    await screen.findByLabelText('Game')
    fireEvent.change(screen.getByLabelText('Invite code'), { target: { value: 'ab12cd' } })
    fireEvent.click(screen.getByRole('button', { name: 'Join session' }))

    await waitFor(() => expect(joinSession).toHaveBeenCalledWith('ab12cd'))
    expect(await screen.findByTestId('location')).toHaveTextContent('/sessions/13')
  })

  it('shows an error when joining fails', async () => {
    listGames.mockResolvedValue([{ slug: 'dnd-5e', displayName: 'D&D 5e' }])
    joinSession.mockRejectedValue(new Error('Invite code not found'))
    renderLobby()

    await screen.findByLabelText('Game')
    fireEvent.change(screen.getByLabelText('Invite code'), { target: { value: 'ZZZZZZ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Join session' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invite code not found')
    expect(screen.queryByTestId('location')).not.toBeInTheDocument()
  })
})