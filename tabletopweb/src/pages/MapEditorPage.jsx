import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../auth/useAuth'
import {
  addObject,
  getMap,
  getMapCatalog,
  paintTiles,
  removeObject,
  updateObject,
} from '../lib/battleMap'
import { Alert, LoadingState } from '../components/ui'

const SQUARE_PX = 36

const TOOLS = [
  { id: 'paint', label: 'Paint' },
  { id: 'erase', label: 'Erase' },
  { id: 'eyedropper', label: 'Pick' },
  { id: 'object', label: 'Object' },
  { id: 'select', label: 'Select' },
]

/**
 * Dedicated full-screen GM map editor (design-draft-v2 §2/§6, Slice 3c). The
 * play view (BattleMapPanel) stays read-only; this route owns authoring: tile
 * paint/erase/eyedropper and object place/move/rotate/delete. Every edit posts
 * to the same Slice 3 endpoints and repaints from the returned full map DTO.
 */
export default function MapEditorPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const [session, setSession] = useState(null)
  const [pageState, setPageState] = useState('loading')
  const [loadError, setLoadError] = useState('')
  const [map, setMap] = useState(null)
  const [catalog, setCatalog] = useState({ tiles: [], objects: [] })
  const [tool, setTool] = useState('paint')
  const [tileIndex, setTileIndex] = useState(0)
  const [objectIndex, setObjectIndex] = useState(0)
  const [selectedObjectId, setSelectedObjectId] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const paintingRef = useRef(false)
  const strokeRef = useRef(new Map())

  useEffect(() => {
    let cancelled = false
    Promise.all([api(`/api/sessions/${id}`), getMap(id), getMapCatalog(id)])
      .then(([snapshot, loadedMap, loadedCatalog]) => {
        if (cancelled) return
        setSession(snapshot)
        setMap(loadedMap)
        setCatalog({
          tiles: loadedCatalog?.tiles ?? [],
          objects: loadedCatalog?.objects ?? [],
        })
        setPageState('ready')
      })
      .catch((loadFailure) => {
        if (cancelled) return
        if (loadFailure.status === 404) {
          setPageState('none')
        } else {
          setLoadError(loadFailure.message)
          setPageState('error')
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const me = session?.participants?.find((participant) => participant.user.id === user?.id)
  const isGm = me?.role === 'GM'

  const tileColors = useMemo(() => {
    const colors = new Map()
    catalog.tiles.forEach((tile) => colors.set(tile.index, tile.color))
    return colors
  }, [catalog])

  const objectColors = useMemo(() => {
    const colors = new Map()
    catalog.objects.forEach((object) => colors.set(object.index, object.color))
    return colors
  }, [catalog])

  const tileAt = useMemo(() => {
    const lookup = new Map()
    ;(map?.tiles ?? []).forEach((tile) => lookup.set(`${tile.x},${tile.y}`, tile.tileIndex))
    return lookup
  }, [map])

  const selectedObject = useMemo(
    () => (map?.objects ?? []).find((object) => object.id === selectedObjectId) ?? null,
    [map, selectedObjectId],
  )

  function objectAt(x, y) {
    return (
      (map?.objects ?? []).find(
        (object) =>
          x >= object.x &&
          x < object.x + object.w &&
          y >= object.y &&
          y < object.y + object.h,
      ) ?? null
    )
  }

  async function runEdit(action) {
    setBusy(true)
    setError('')
    try {
      const next = await action()
      setMap(next)
    } catch (callError) {
      setError(callError.message)
    } finally {
      setBusy(false)
    }
  }

  function flushStroke(tiles) {
    return runEdit(() => paintTiles(id, { tiles }))
  }

  function handleCellDown(x, y) {
    if (busy) return
    if (tool === 'paint' || tool === 'erase') {
      paintingRef.current = true
      strokeRef.current = new Map()
      strokeRef.current.set(`${x},${y}`, {
        x,
        y,
        tileIndex: tool === 'erase' ? null : tileIndex,
      })
      return
    }
    if (tool === 'eyedropper') {
      const picked = tileAt.get(`${x},${y}`)
      if (picked != null) {
        setTileIndex(picked)
        setTool('paint')
      }
      return
    }
    if (tool === 'object') {
      runEdit(() => addObject(id, { objectIndex, x, y }))
      setSelectedObjectId(null)
      return
    }
    if (tool === 'select') {
      const object = objectAt(x, y)
      if (object) {
        setSelectedObjectId(object.id)
        return
      }
      if (selectedObjectId != null) {
        runEdit(() => updateObject(id, selectedObjectId, { x, y }))
        return
      }
      setSelectedObjectId(null)
    }
  }

  function handleCellEnter(x, y) {
    if (!paintingRef.current) return
    strokeRef.current.set(`${x},${y}`, {
      x,
      y,
      tileIndex: tool === 'erase' ? null : tileIndex,
    })
  }

  function handleCellUp() {
    if (!paintingRef.current) return
    paintingRef.current = false
    const tiles = Array.from(strokeRef.current.values())
    strokeRef.current = new Map()
    if (tiles.length > 0) flushStroke(tiles)
  }

  function handleRotate() {
    if (!selectedObject) return
    runEdit(() =>
      updateObject(id, selectedObject.id, {
        rotation: (selectedObject.rotation + 90) % 360,
      }),
    )
  }

  function handleDelete() {
    if (selectedObjectId == null) return
    runEdit(() => removeObject(id, selectedObjectId))
    setSelectedObjectId(null)
  }

  if (pageState === 'loading') {
    return <LoadingState tone="light" message="Loading map editor…" />
  }

  if (pageState === 'error') {
    return (
      <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <Alert tone="error">{loadError}</Alert>
      </div>
    )
  }

  if (pageState === 'none') {
    return (
      <div className="space-y-4">
        <Alert tone="warning">
          No battle map exists for this session yet — set one up from the session page first.
        </Alert>
        <Link to={`/sessions/${id}`} className="text-sm font-medium text-indigo-600 hover:underline">
          ← Back to session
        </Link>
      </div>
    )
  }

  if (!isGm) {
    return (
      <div className="space-y-4">
        <Alert tone="warning">Only the game master can edit this map.</Alert>
        <Link to={`/sessions/${id}`} className="text-sm font-medium text-indigo-600 hover:underline">
          ← Back to session
        </Link>
      </div>
    )
  }

  const gridWidth = map.width * SQUARE_PX
  const gridHeight = map.height * SQUARE_PX

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <div>
          <h1 className="text-xl font-semibold text-zinc-900">Map editor</h1>
          <p className="text-sm text-zinc-500">
            {session.name} · {map.name ?? 'Battle map'} · {map.width}×{map.height}
          </p>
        </div>
        <Link
          to={`/sessions/${id}`}
          className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-zinc-50"
        >
          ← Back to session
        </Link>
      </header>

      {error && <Alert tone="error">{error}</Alert>}

      <div className="flex flex-col gap-4 lg:flex-row">
        <aside className="w-full shrink-0 space-y-4 rounded-xl border border-zinc-200 bg-white p-4 shadow-sm lg:w-64">
          <div data-testid="editor-toolbar" className="flex flex-wrap gap-2">
            {TOOLS.map((entry) => (
              <button
                key={entry.id}
                type="button"
                data-testid={`tool-${entry.id}`}
                onClick={() => setTool(entry.id)}
                className={`rounded-md border px-3 py-1.5 text-sm ${
                  tool === entry.id
                    ? 'border-indigo-600 bg-indigo-600 text-white'
                    : 'border-zinc-300 text-zinc-700 hover:bg-zinc-50'
                }`}
              >
                {entry.label}
              </button>
            ))}
          </div>

          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-zinc-400">Tiles</p>
            <div className="mt-2 grid grid-cols-6 gap-1">
              {catalog.tiles.map((tile) => (
                <button
                  key={tile.index}
                  type="button"
                  data-testid={`tile-swatch-${tile.index}`}
                  aria-label={`Tile ${tile.name}`}
                  title={tile.name}
                  onClick={() => {
                    setTileIndex(tile.index)
                    setTool('paint')
                  }}
                  className={`h-7 w-7 rounded ${
                    tileIndex === tile.index && tool !== 'erase'
                      ? 'ring-2 ring-indigo-600 ring-offset-1'
                      : 'ring-1 ring-zinc-300'
                  }`}
                  style={{ backgroundColor: tile.color }}
                />
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-zinc-400">Objects</p>
            <div className="mt-2 grid grid-cols-3 gap-1">
              {catalog.objects.map((object) => (
                <button
                  key={object.index}
                  type="button"
                  data-testid={`object-swatch-${object.index}`}
                  aria-label={`Object ${object.name}`}
                  title={`${object.name}${object.blocksMovement ? ' (blocks)' : ''}`}
                  onClick={() => {
                    setObjectIndex(object.index)
                    setTool('object')
                  }}
                  className={`flex h-8 items-center justify-center rounded text-[9px] font-semibold text-white ${
                    objectIndex === object.index && tool === 'object'
                      ? 'ring-2 ring-indigo-600 ring-offset-1'
                      : 'ring-1 ring-zinc-300'
                  }`}
                  style={{ backgroundColor: object.color }}
                >
                  {object.blocksMovement ? '■' : '·'}
                </button>
              ))}
            </div>
          </div>

          {selectedObject && (
            <div
              data-testid="object-actions"
              className="space-y-2 rounded-lg bg-zinc-50 p-3"
            >
              <p className="text-xs text-zinc-500">
                Selected object #{selectedObject.id} · {selectedObject.rotation}°
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  data-testid="rotate-object"
                  disabled={busy}
                  onClick={handleRotate}
                  className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 hover:bg-white disabled:opacity-50"
                >
                  Rotate
                </button>
                <button
                  type="button"
                  data-testid="delete-object"
                  disabled={busy}
                  onClick={handleDelete}
                  className="rounded-md border border-red-300 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50 disabled:opacity-50"
                >
                  Delete
                </button>
              </div>
            </div>
          )}
        </aside>

        <div className="flex-1 overflow-auto rounded-lg border border-zinc-200 bg-white p-2">
          <div
            data-testid="editor-grid"
            className="relative"
            style={{
              width: `${gridWidth}px`,
              height: `${gridHeight}px`,
              backgroundImage:
                'linear-gradient(to right, #e4e4e7 1px, transparent 1px), linear-gradient(to bottom, #e4e4e7 1px, transparent 1px)',
              backgroundSize: `${SQUARE_PX}px ${SQUARE_PX}px`,
              backgroundPosition: '-1px -1px',
            }}
            onMouseUp={handleCellUp}
            onMouseLeave={handleCellUp}
          >
            {(map.tiles ?? []).map((tile) => {
              const color = tileColors.get(tile.tileIndex)
              if (!color) return null
              if (tile.x < 0 || tile.y < 0 || tile.x >= map.width || tile.y >= map.height) {
                return null
              }
              return (
                <div
                  key={`tile-${tile.x}-${tile.y}`}
                  data-testid={`editor-tile-${tile.x}-${tile.y}`}
                  aria-hidden="true"
                  className="pointer-events-none absolute"
                  style={{
                    left: `${tile.x * SQUARE_PX}px`,
                    top: `${tile.y * SQUARE_PX}px`,
                    width: `${SQUARE_PX}px`,
                    height: `${SQUARE_PX}px`,
                    backgroundColor: color,
                    boxShadow: 'inset 0 0 0 1px rgba(0, 0, 0, 0.12)',
                  }}
                />
              )
            })}
            {(map.objects ?? []).map((object) => {
              const color = objectColors.get(object.objectIndex)
              if (!color) return null
              if (
                object.x < 0 ||
                object.y < 0 ||
                object.x >= map.width ||
                object.y >= map.height
              ) {
                return null
              }
              const selected = object.id === selectedObjectId
              return (
                <div
                  key={`object-${object.id}`}
                  data-testid={`editor-object-${object.id}`}
                  aria-hidden="true"
                  className="pointer-events-none absolute flex items-center justify-center"
                  style={{
                    left: `${object.x * SQUARE_PX}px`,
                    top: `${object.y * SQUARE_PX}px`,
                    width: `${object.w * SQUARE_PX}px`,
                    height: `${object.h * SQUARE_PX}px`,
                    backgroundColor: color,
                    opacity: 0.85,
                    borderRadius: '3px',
                    boxShadow: selected
                      ? 'inset 0 0 0 2px #4f46e5, 0 0 0 2px #4f46e5'
                      : 'inset 0 0 0 1px rgba(0, 0, 0, 0.35)',
                    zIndex: 20,
                  }}
                >
                  <span className="text-[8px] font-bold uppercase tracking-wide text-white/80">
                    {object.blocksMovement ? '■' : '·'}
                  </span>
                </div>
              )
            })}
            {Array.from({ length: map.height }, (_, y) =>
              Array.from({ length: map.width }, (_, x) => (
                <button
                  key={`cell-${x}-${y}`}
                  type="button"
                  data-testid={`cell-${x}-${y}`}
                  aria-label={`Square ${x}, ${y}`}
                  onMouseDown={() => handleCellDown(x, y)}
                  onMouseEnter={() => handleCellEnter(x, y)}
                  className="absolute"
                  style={{
                    left: `${x * SQUARE_PX}px`,
                    top: `${y * SQUARE_PX}px`,
                    width: `${SQUARE_PX}px`,
                    height: `${SQUARE_PX}px`,
                    zIndex: 1,
                  }}
                />
              )),
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
