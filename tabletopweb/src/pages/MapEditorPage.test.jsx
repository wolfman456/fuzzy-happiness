import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MapEditorPage from './MapEditorPage'

const battleMapMock = vi.hoisted(() => ({
  getMap: vi.fn(),
  getMapCatalog: vi.fn(),
  paintTiles: vi.fn(),
  addObject: vi.fn(),
  updateObject: vi.fn(),
  removeObject: vi.fn(),
}))

vi.mock('../lib/battleMap', () => battleMapMock)
vi.mock('../lib/api', () => ({ api: vi.fn() }))
vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ user: { id: 1, username: 'ginger', displayName: 'Ginger' } }),
}))

import { api } from '../lib/api'

const SESSION = {
  id: 7,
  name: 'Grumm’s Revenge',
  participants: [
    { user: { id: 1, username: 'ginger', displayName: 'Ginger' }, role: 'GM' },
    { user: { id: 2, username: 'ivo', displayName: 'Ivo' }, role: 'PLAYER' },
  ],
}

const BASE_MAP = {
  id: 5,
  sessionId: 7,
  name: 'Hall',
  width: 4,
  height: 3,
  squareFeet: 10,
  currentTurnTokenId: null,
  initiativeIndex: -1,
  initiative: [],
  tiles: [],
  objects: [],
  tokens: [],
}

const CATALOG = {
  tiles: [
    { index: 0, name: 'Stone Floor', color: '#9ca3af' },
    { index: 7, name: 'Water', color: '#60a5fa' },
  ],
  objects: [
    { index: 0, name: 'Wall', color: '#6b7280', width: 1, height: 1, blocksMovement: true },
    { index: 6, name: 'Rug', color: '#b91c1c', width: 2, height: 2, blocksMovement: false },
  ],
}

function renderEditor() {
  return render(
    <MemoryRouter initialEntries={['/sessions/7/map/edit']}>
      <Routes>
        <Route path="/sessions/:id/map/edit" element={<MapEditorPage />} />
        <Route path="/sessions/:id" element={<div>session page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function openEditor(map = BASE_MAP) {
  api.mockResolvedValue(SESSION)
  battleMapMock.getMap.mockResolvedValue(map)
  battleMapMock.getMapCatalog.mockResolvedValue(CATALOG)
  renderEditor()
  await screen.findByTestId('editor-grid')
}

beforeEach(() => {
  vi.clearAllMocks()
  battleMapMock.getMap.mockReset()
  battleMapMock.getMapCatalog.mockReset()
  battleMapMock.paintTiles.mockReset()
  battleMapMock.addObject.mockReset()
  battleMapMock.updateObject.mockReset()
  battleMapMock.removeObject.mockReset()
  api.mockReset()
})

describe('MapEditorPage', () => {
  it('renders the GM palette, tools and grid', async () => {
    await openEditor()
    expect(screen.getByRole('heading', { name: 'Map editor' })).toBeInTheDocument()
    expect(screen.getByTestId('tool-paint')).toBeInTheDocument()
    expect(screen.getByTestId('tile-swatch-0')).toBeInTheDocument()
    expect(screen.getByTestId('tile-swatch-7')).toBeInTheDocument()
    expect(screen.getByTestId('object-swatch-0')).toBeInTheDocument()
    expect(screen.getByTestId('cell-3-2')).toBeInTheDocument()
  })

  it('refuses non-GM users', async () => {
    api.mockResolvedValue({
      ...SESSION,
      participants: [{ user: { id: 1, username: 'ginger' }, role: 'PLAYER' }],
    })
    battleMapMock.getMap.mockResolvedValue(BASE_MAP)
    battleMapMock.getMapCatalog.mockResolvedValue(CATALOG)
    renderEditor()
    expect(
      await screen.findByText('Only the game master can edit this map.'),
    ).toBeInTheDocument()
    expect(screen.queryByTestId('editor-grid')).not.toBeInTheDocument()
  })

  it('paints a single cell with the selected tile', async () => {
    battleMapMock.paintTiles.mockResolvedValue({
      ...BASE_MAP,
      tiles: [{ x: 2, y: 1, tileIndex: 0 }],
    })
    await openEditor()
    fireEvent.mouseDown(screen.getByTestId('cell-2-1'))
    fireEvent.mouseUp(screen.getByTestId('editor-grid'))
    await waitFor(() =>
      expect(battleMapMock.paintTiles).toHaveBeenCalledWith('7', {
        tiles: [{ x: 2, y: 1, tileIndex: 0 }],
      }),
    )
    expect(await screen.findByTestId('editor-tile-2-1')).toBeInTheDocument()
  })

  it('paints a dragged stroke as one batch', async () => {
    battleMapMock.paintTiles.mockResolvedValue(BASE_MAP)
    await openEditor()
    fireEvent.mouseDown(screen.getByTestId('cell-0-0'))
    fireEvent.mouseEnter(screen.getByTestId('cell-1-0'))
    fireEvent.mouseEnter(screen.getByTestId('cell-2-0'))
    fireEvent.mouseUp(screen.getByTestId('editor-grid'))
    await waitFor(() =>
      expect(battleMapMock.paintTiles).toHaveBeenCalledWith('7', {
        tiles: [
          { x: 0, y: 0, tileIndex: 0 },
          { x: 1, y: 0, tileIndex: 0 },
          { x: 2, y: 0, tileIndex: 0 },
        ],
      }),
    )
  })

  it('erases a painted square with the erase tool', async () => {
    battleMapMock.paintTiles.mockResolvedValue(BASE_MAP)
    await openEditor({ ...BASE_MAP, tiles: [{ x: 1, y: 1, tileIndex: 7 }] })
    fireEvent.click(screen.getByTestId('tool-erase'))
    fireEvent.mouseDown(screen.getByTestId('cell-1-1'))
    fireEvent.mouseUp(screen.getByTestId('editor-grid'))
    await waitFor(() =>
      expect(battleMapMock.paintTiles).toHaveBeenCalledWith('7', {
        tiles: [{ x: 1, y: 1, tileIndex: null }],
      }),
    )
  })

  it('picks a tile with the eyedropper and re-selects the paint tool', async () => {
    battleMapMock.paintTiles.mockResolvedValue(BASE_MAP)
    await openEditor({ ...BASE_MAP, tiles: [{ x: 0, y: 0, tileIndex: 7 }] })
    fireEvent.click(screen.getByTestId('tool-eyedropper'))
    fireEvent.mouseDown(screen.getByTestId('cell-0-0'))
    fireEvent.mouseDown(screen.getByTestId('cell-3-2'))
    fireEvent.mouseUp(screen.getByTestId('editor-grid'))
    await waitFor(() =>
      expect(battleMapMock.paintTiles).toHaveBeenCalledWith('7', {
        tiles: [{ x: 3, y: 2, tileIndex: 7 }],
      }),
    )
  })

  it('places the selected object on a square', async () => {
    battleMapMock.addObject.mockResolvedValue({
      ...BASE_MAP,
      objects: [
        { id: 21, objectIndex: 6, x: 2, y: 2, w: 2, h: 2, rotation: 0, blocksMovement: false },
      ],
    })
    await openEditor()
    fireEvent.click(screen.getByTestId('object-swatch-6'))
    fireEvent.mouseDown(screen.getByTestId('cell-2-2'))
    await waitFor(() =>
      expect(battleMapMock.addObject).toHaveBeenCalledWith('7', {
        objectIndex: 6,
        x: 2,
        y: 2,
      }),
    )
    expect(await screen.findByTestId('editor-object-21')).toBeInTheDocument()
  })

  it('selects a placed object and moves it to another square', async () => {
    battleMapMock.updateObject.mockResolvedValue({
      ...BASE_MAP,
      objects: [
        { id: 21, objectIndex: 0, x: 3, y: 2, w: 1, h: 1, rotation: 0, blocksMovement: true },
      ],
    })
    await openEditor({
      ...BASE_MAP,
      objects: [
        { id: 21, objectIndex: 0, x: 1, y: 1, w: 1, h: 1, rotation: 0, blocksMovement: true },
      ],
    })
    fireEvent.click(screen.getByTestId('tool-select'))
    fireEvent.mouseDown(screen.getByTestId('cell-1-1'))
    expect(await screen.findByTestId('object-actions')).toBeInTheDocument()
    fireEvent.mouseDown(screen.getByTestId('cell-3-2'))
    await waitFor(() =>
      expect(battleMapMock.updateObject).toHaveBeenCalledWith('7', 21, { x: 3, y: 2 }),
    )
  })

  it('rotates a selected object a quarter turn', async () => {
    battleMapMock.updateObject.mockResolvedValue(BASE_MAP)
    await openEditor({
      ...BASE_MAP,
      objects: [
        { id: 21, objectIndex: 0, x: 1, y: 1, w: 1, h: 1, rotation: 270, blocksMovement: true },
      ],
    })
    fireEvent.click(screen.getByTestId('tool-select'))
    fireEvent.mouseDown(screen.getByTestId('cell-1-1'))
    fireEvent.click(await screen.findByTestId('rotate-object'))
    await waitFor(() =>
      expect(battleMapMock.updateObject).toHaveBeenCalledWith('7', 21, { rotation: 0 }),
    )
  })

  it('deletes a selected object', async () => {
    battleMapMock.removeObject.mockResolvedValue(BASE_MAP)
    await openEditor({
      ...BASE_MAP,
      objects: [
        { id: 21, objectIndex: 0, x: 1, y: 1, w: 1, h: 1, rotation: 0, blocksMovement: true },
      ],
    })
    fireEvent.click(screen.getByTestId('tool-select'))
    fireEvent.mouseDown(screen.getByTestId('cell-1-1'))
    fireEvent.click(await screen.findByTestId('delete-object'))
    await waitFor(() => expect(battleMapMock.removeObject).toHaveBeenCalledWith('7', 21))
  })

  it('shows a message when no map exists yet', async () => {
    api.mockResolvedValue(SESSION)
    battleMapMock.getMap.mockRejectedValue({ status: 404, message: 'Battle map not found' })
    battleMapMock.getMapCatalog.mockResolvedValue(CATALOG)
    renderEditor()
    expect(
      await screen.findByText(/No battle map exists for this session yet/),
    ).toBeInTheDocument()
  })
})
