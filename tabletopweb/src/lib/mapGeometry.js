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

export function reachableSquares(token, map) {
  const remaining = remainingSquares(token, map.squareFeet)
  if (remaining <= 0) return []
  const squares = []
  for (let dy = -remaining; dy <= remaining; dy += 1) {
    for (let dx = -remaining; dx <= remaining; dx += 1) {
      const x = token.posX + dx
      const y = token.posY + dy
      if (x < 0 || y < 0 || x >= map.width || y >= map.height) continue
      if (distanceInSquares({ x: token.posX, y: token.posY }, { x, y }) <= remaining) squares.push({ x, y })
    }
  }
  return squares
}