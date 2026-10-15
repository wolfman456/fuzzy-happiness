export const RACE_SPEEDS = [
  { name: 'Dwarf', speedFeet: 25 },
  { name: 'Halfling', speedFeet: 25 },
  { name: 'Gnome', speedFeet: 25 },
  { name: 'Human', speedFeet: 30 },
  { name: 'Elf', speedFeet: 30 },
  { name: 'Half-Elf', speedFeet: 30 },
  { name: 'Half-Orc', speedFeet: 30 },
  { name: 'Dragonborn', speedFeet: 30 },
  { name: 'Tiefling', speedFeet: 30 },
  { name: 'Wood Elf', speedFeet: 35 },
  { name: 'Centaur', speedFeet: 40 },
]

export const TOKEN_COLORS = [
  '#ef4444',
  '#f97316',
  '#eab308',
  '#22c55e',
  '#3b82f6',
  '#8b5cf6',
  '#ec4899',
  '#14b8a6',
]

export function squaresFor(speedFeet, squareFeet) {
  if (!Number.isInteger(speedFeet) || !Number.isInteger(squareFeet)) return 0
  if (speedFeet < 0 || squareFeet <= 0) return 0
  return Math.floor(speedFeet / squareFeet)
}

export function distanceInSquares(from, to) {
  return Math.max(Math.abs(from.x - to.x), Math.abs(from.y - to.y))
}

export function costInFeet(from, to, squareFeet) {
  return distanceInSquares(from, to) * squareFeet
}

export function remainingFeet(token) {
  return Math.max(0, token.speedFeet - token.movedFeet)
}

export function remainingSquares(token, squareFeet) {
  return Math.floor(remainingFeet(token) / squareFeet)
}

const cellKey = (x, y) => `${x},${y}`

/**
 * Expands every movement-blocking object on the map into the set of squares its
 * axis-aligned footprint covers (clamped to the map). Mirrors the backend's
 * blocked-square expansion so the client highlights exactly what the server
 * will allow.
 */
export function blockedSquares(map) {
  const blocked = new Set()
  const objects = map.objects ?? []
  for (const object of objects) {
    if (!object.blocksMovement) continue
    for (let x = object.x; x < object.x + object.w; x += 1) {
      for (let y = object.y; y < object.y + object.h; y += 1) {
        if (x >= 0 && y >= 0 && x < map.width && y < map.height) blocked.add(cellKey(x, y))
      }
    }
  }
  return blocked
}

/**
 * Breadth-first flood over the 8 surrounding squares (each step costs one
 * square), refusing to enter blocked or off-map squares. A diagonal step is
 * only allowed when at least one of its two orthogonal neighbours is open, so a
 * token cannot cut through a solid corner. Returns the origin square plus every
 * square reachable within the token's remaining movement budget; pass a
 * precomputed `blocked` set to avoid rebuilding it per token.
 */
export function reachableSquares(token, map, blocked = blockedSquares(map)) {
  const remaining = remainingSquares(token, map.squareFeet)
  if (remaining <= 0) return []
  const origin = { x: token.posX, y: token.posY }
  const visited = new Set([cellKey(origin.x, origin.y)])
  const squares = [origin]
  let frontier = [origin]
  for (let depth = 0; depth < remaining && frontier.length > 0; depth += 1) {
    const next = []
    for (const cell of frontier) {
      for (let dx = -1; dx <= 1; dx += 1) {
        for (let dy = -1; dy <= 1; dy += 1) {
          if (dx === 0 && dy === 0) continue
          const x = cell.x + dx
          const y = cell.y + dy
          if (x < 0 || y < 0 || x >= map.width || y >= map.height) continue
          const key = cellKey(x, y)
          if (visited.has(key) || blocked.has(key)) continue
          if (
            dx !== 0 &&
            dy !== 0 &&
            blocked.has(cellKey(cell.x + dx, cell.y)) &&
            blocked.has(cellKey(cell.x, cell.y + dy))
          ) {
            continue
          }
          visited.add(key)
          squares.push({ x, y })
          next.push({ x, y })
        }
      }
    }
    frontier = next
  }
  return squares
}