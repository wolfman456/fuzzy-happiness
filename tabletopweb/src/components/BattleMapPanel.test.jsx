import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BattleMapPanel from './BattleMapPanel'

const battleMapMock = vi.hoisted(() => ({
  addToken: vi.fn(),
  moveToken: vi.fn(),
  removeToken: vi.fn(),
  turnCommand: vi.fn(),
  updateMap: vi.fn(),
  updateToken: vi.fn(),
}))

vi.mock('../lib/battleMap', () => battleMapMock)

const user = { id: 1, username: 'ginger', displayName: 'Ginger' }

function makeMap(overrides = {}) {
  return {
    id: 1,
    sessionId: 7,
    name: 'Test map',
    width: 24,
    height: 18,
    squareFeet: 10,
    currentTurnTokenId: null,
    tokens: [
      { id: 11, name: 'Aria Sol', category: 'PLAYER', color: '#3b82f6', speedFeet: 30, posX: 1, posY: 1, movedFeet: 0, linkedParticipantId: null, linkedUserId: 1 },
      { id: 12, name: 'Goblin', category: 'MONSTER_NPC', color: '#ef4444', speedFeet: 30, posX: 5, posY: 5, movedFeet: 0, linkedParticipantId: null, linkedUserId: null },
    ],
    ...overrides,
  }
}

function renderPanel(props = {}) {
  const onMapChange = vi.fn()
  render(
    <BattleMapPanel
      sessionId="7"
      map={makeMap()}
      user={user}
      isGm={false}
      disabled={false}
      onMapChange={onMapChange}
      {...props}
    />,
  )
  return { onMapChange }
}

beforeEach(() => {
  vi.clearAllMocks()
  battleMapMock.moveToken.mockResolvedValue(makeMap())
  battleMapMock.turnCommand.mockResolvedValue(makeMap())
  battleMapMock.addToken.mockResolvedValue(makeMap())
  battleMapMock.updateToken.mockResolvedValue(makeMap())
  battleMapMock.updateMap.mockResolvedValue(makeMap({ width: 12, height: 10 }))
  battleMapMock.removeToken.mockResolvedValue(makeMap({ tokens: [makeMap().tokens[1]] }))
})

describe('BattleMapPanel', () => {
  it('renders the grid, tokens and stats', () => {
    renderPanel({ isGm: true })
    expect(screen.getByRole('heading', { name: /Test map/ })).toBeInTheDocument()
    expect(screen.getByText(/10 ft per square · 24×18/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Token Aria Sol/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Token Goblin/ })).toBeInTheDocument()
    expect(screen.getByText('Aria Sol')).toBeInTheDocument()
    expect(screen.getByText(/MONSTER_NPC · 30 ft · 0\/30 ft moved/)).toBeInTheDocument()
  })

  it('shows the current turn from the map', () => {
    renderPanel({ map: makeMap({ currentTurnTokenId: 11 }) })
    expect(screen.getByText('Turn: Aria Sol')).toBeInTheDocument()
  })

  it('lets a player move their own token within the budget', async () => {
    const nextMap = makeMap({
      tokens: [
        { id: 11, name: 'Aria Sol', category: 'PLAYER', color: '#3b82f6', speedFeet: 30, posX: 2, posY: 1, movedFeet: 10, linkedParticipantId: null, linkedUserId: 1 },
        ...makeMap().tokens.slice(1),
      ],
    })
    battleMapMock.moveToken.mockResolvedValue(nextMap)
    const { onMapChange } = renderPanel({ isGm: false })

    fireEvent.click(screen.getByRole('button', { name: /Token Aria Sol/ }))
    const destination = screen.getByRole('button', { name: 'Move to square 2, 1' })
    expect(destination).toBeInTheDocument()

    fireEvent.click(destination)
    await waitFor(() => expect(battleMapMock.moveToken).toHaveBeenCalledWith('7', 11, { x: 2, y: 1 }))
    expect(onMapChange).toHaveBeenCalledWith(nextMap)
  })

  it('does not offer movement for tokens the player does not own', () => {
    renderPanel({ isGm: false })
    fireEvent.click(screen.getByRole('button', { name: /Token Goblin/ }))
    expect(screen.queryByRole('button', { name: /Move to square/ })).not.toBeInTheDocument()
    expect(battleMapMock.moveToken).not.toHaveBeenCalled()
  })

  it('lets the GM start a turn for a selected token', async () => {
    battleMapMock.turnCommand.mockResolvedValue(makeMap({ currentTurnTokenId: 11 }))
    const { onMapChange } = renderPanel({ isGm: true })

    fireEvent.click(screen.getByRole('button', { name: /Token Aria Sol/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Start turn' }))

    await waitFor(() => expect(battleMapMock.turnCommand).toHaveBeenCalledWith('7', { action: 'START', tokenId: 11 }))
    expect(onMapChange).toHaveBeenCalledWith(expect.objectContaining({ currentTurnTokenId: 11 }))
  })

  it('sends END and NEW_ROUND commands', async () => {
    renderPanel({ isGm: true })

    fireEvent.click(screen.getByRole('button', { name: 'End turn' }))
    await waitFor(() => expect(battleMapMock.turnCommand).toHaveBeenCalledWith('7', { action: 'END', tokenId: undefined }))

    fireEvent.click(screen.getByRole('button', { name: 'New round' }))
    await waitFor(() => expect(battleMapMock.turnCommand).toHaveBeenCalledWith('7', { action: 'NEW_ROUND', tokenId: undefined }))
  })

  it('adds a token from the GM form with the race-based speed', async () => {
    battleMapMock.addToken.mockResolvedValue(makeMap({
      tokens: [...makeMap().tokens, { id: 13, name: 'Orc', category: 'MONSTER_NPC', color: '#ef4444', speedFeet: 25, posX: 0, posY: 0, movedFeet: 0, linkedParticipantId: null, linkedUserId: null }],
    }))
    const { onMapChange } = renderPanel({ isGm: true })

    fireEvent.click(screen.getByRole('button', { name: 'Add token' }))
    const form = screen.getByTestId('token-form')
    fireEvent.change(within(form).getByLabelText('Name'), { target: { value: 'Orc' } })
    fireEvent.change(within(form).getByLabelText('Race'), { target: { value: 'Dwarf' } })
    expect(within(form).getByLabelText('Speed (ft/turn)')).toHaveValue(25)
    fireEvent.change(within(form).getByLabelText('Category'), { target: { value: 'PLAYER' } })

    fireEvent.click(within(form).getByRole('button', { name: 'Add token' }))

    await waitFor(() =>
      expect(battleMapMock.addToken).toHaveBeenCalledWith('7', {
        name: 'Orc',
        category: 'PLAYER',
        speedFeet: 25,
        color: '#ef4444',
        posX: 0,
        posY: 0,
      }),
    )
    expect(onMapChange).toHaveBeenCalledTimes(1)
  })

  it('edits an existing token', async () => {
    renderPanel({ isGm: true })

    fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0])
    const form = screen.getByTestId('token-form')
    fireEvent.change(within(form).getByLabelText('Name'), { target: { value: 'Aria Sol II' } })
    fireEvent.change(within(form).getByLabelText('Speed (ft/turn)'), { target: { value: '35' } })

    fireEvent.click(within(form).getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(battleMapMock.updateToken).toHaveBeenCalledWith(
        '7',
        11,
        expect.objectContaining({ name: 'Aria Sol II', speedFeet: 35 }),
      ),
    )
  })

  it('removes a token', async () => {
    const { onMapChange } = renderPanel({ isGm: true })

    fireEvent.click(screen.getAllByRole('button', { name: 'Remove' })[0])

    await waitFor(() => expect(battleMapMock.removeToken).toHaveBeenCalledWith('7', 11))
    expect(onMapChange).toHaveBeenCalled()
  })

  it('surfaces server movement errors', async () => {
    battleMapMock.moveToken.mockRejectedValue(new Error('Movement of 40 ft exceeds the 0 ft remaining this turn'))
    renderPanel({ isGm: false })

    fireEvent.click(screen.getByRole('button', { name: /Token Aria Sol/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Move to square 2, 1' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('exceeds the 0 ft remaining')
  })

  it('disables interactions for closed sessions', () => {
    renderPanel({ isGm: true, disabled: true })

    expect(screen.getByRole('button', { name: 'Add token' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'End turn' })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Token Aria Sol/ })).toBeDisabled()
    expect(screen.queryByRole('button', { name: /Move to square/ })).not.toBeInTheDocument()
    expect(battleMapMock.moveToken).not.toHaveBeenCalled()
  })

  it('lets the GM resize the map', async () => {
    const { onMapChange } = renderPanel({ isGm: true })

    fireEvent.click(screen.getByRole('button', { name: 'Resize' }))
    const form = screen.getByTestId('resize-form')
    fireEvent.change(within(form).getAllByRole('spinbutton')[0], { target: { value: '12' } })
    fireEvent.change(within(form).getAllByRole('spinbutton')[1], { target: { value: '10' } })

    fireEvent.click(within(form).getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(battleMapMock.updateMap).toHaveBeenCalledWith('7', { width: 12, height: 10 }))
    expect(onMapChange).toHaveBeenCalledWith(expect.objectContaining({ width: 12, height: 10 }))
  })

  it('hides the resize control from players', () => {
    renderPanel({ isGm: false })
    expect(screen.queryByRole('button', { name: 'Resize' })).not.toBeInTheDocument()
  })

  it('shows the hovered square coordinates', () => {
    renderPanel()
    const grid = screen.getByTestId('battle-grid')
    fireEvent.mouseMove(grid, { clientX: 5, clientY: 5 })
    expect(screen.getByTestId('square-readout')).toHaveTextContent('Square (0, 0)')
    fireEvent.mouseLeave(grid)
    expect(screen.getByTestId('square-readout')).not.toHaveTextContent(/Square/)
  })

  it('renders axis coordinate labels on the grid', () => {
    renderPanel({ map: makeMap({ width: 2, height: 2 }) })
    const grid = screen.getByTestId('battle-grid')
    const cols = within(grid).getAllByText('0')
    expect(cols.length).toBe(2)
    expect(within(grid).getAllByText('1').length).toBe(2)
  })

  it('shows remaining feet on a moved token the player owns', () => {
    renderPanel({
      isGm: false,
      map: makeMap({
        tokens: [
          { id: 11, name: 'Aria Sol', category: 'PLAYER', color: '#3b82f6', speedFeet: 30, posX: 1, posY: 1, movedFeet: 20, linkedParticipantId: null, linkedUserId: 1 },
          ...makeMap().tokens.slice(1),
        ],
      }),
    })
    expect(screen.getByTestId('feet-11')).toHaveTextContent('10ft')
  })

  it('does not show remaining feet on tokens the player does not own', () => {
    renderPanel({
      isGm: false,
      map: makeMap({
        tokens: [
          { id: 11, name: 'Aria Sol', category: 'PLAYER', color: '#3b82f6', speedFeet: 30, posX: 1, posY: 1, movedFeet: 20, linkedParticipantId: null, linkedUserId: 1 },
          { id: 12, name: 'Goblin', category: 'MONSTER_NPC', color: '#ef4444', speedFeet: 30, posX: 5, posY: 5, movedFeet: 15, linkedParticipantId: null, linkedUserId: null },
        ],
      }),
    })
    expect(screen.getByTestId('feet-11')).toBeInTheDocument()
    expect(screen.queryByTestId('feet-12')).not.toBeInTheDocument()
  })
})