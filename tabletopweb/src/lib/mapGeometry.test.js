import { describe, expect, it } from 'vitest'
import {
  RACE_SPEEDS,
  costInFeet,
  distanceInSquares,
  reachableSquares,
  remainingFeet,
  remainingSquares,
  squaresFor,
} from './mapGeometry'

describe('squaresFor', () => {
  it('returns floor(speed / squareFeet)', () => {
    expect(squaresFor(30, 10)).toBe(3)
    expect(squaresFor(25, 10)).toBe(2)
    expect(squaresFor(40, 10)).toBe(4)
    expect(squaresFor(60, 10)).toBe(6)
  })

  it('handles non-positive and non-integer input safely', () => {
    expect(squaresFor(0, 10)).toBe(0)
    expect(squaresFor(-5, 10)).toBe(0)
    expect(squaresFor(30, 0)).toBe(0)
    expect(squaresFor(30.5, 10)).toBe(0)
  })
})

describe('distanceInSquares and costInFeet', () => {
  it('measures Chebyshev distance', () => {
    expect(distanceInSquares({ x: 1, y: 1 }, { x: 2, y: 4 })).toBe(3)
    expect(distanceInSquares({ x: 5, y: 5 }, { x: 1, y: 6 })).toBe(4)
    expect(distanceInSquares({ x: 3, y: 3 }, { x: 3, y: 3 })).toBe(0)
  })

  it('converts to feet via squareFeet', () => {
    expect(costInFeet({ x: 1, y: 1 }, { x: 2, y: 4 }, 10)).toBe(30)
    expect(costInFeet({ x: 1, y: 1 }, { x: 2, y: 4 }, 5)).toBe(15)
  })
})

describe('remaining budget', () => {
  it('computes remaining feet and squares', () => {
    expect(remainingFeet({ speedFeet: 30, movedFeet: 10 })).toBe(20)
    expect(remainingFeet({ speedFeet: 30, movedFeet: 40 })).toBe(0)
    expect(remainingSquares({ speedFeet: 25, movedFeet: 0 }, 10)).toBe(2)
    expect(remainingSquares({ speedFeet: 25, movedFeet: 21 }, 10)).toBe(0)
  })
})

describe('reachableSquares', () => {
  const map = { width: 24, height: 18, squareFeet: 10 }

  it('lists every square within the movement budget', () => {
    const token = { posX: 5, posY: 5, speedFeet: 30, movedFeet: 0 }
    const squares = reachableSquares(token, map)
    expect(squares).toHaveLength(49)
    expect(squares).toContainEqual({ x: 2, y: 2 })
    expect(squares).toContainEqual({ x: 5, y: 8 })
    expect(squares).toContainEqual({ x: 8, y: 5 })
  })

  it('clips to map edges', () => {
    const token = { posX: 0, posY: 0, speedFeet: 30, movedFeet: 0 }
    const squares = reachableSquares(token, map)
    expect(squares).toHaveLength(16)
    expect(squares).not.toContainEqual({ x: -1, y: 0 })
  })

  it('returns an empty set when the budget is spent', () => {
    const token = { posX: 5, posY: 5, speedFeet: 30, movedFeet: 30 }
    expect(reachableSquares(token, map)).toEqual([])
  })
})

describe('RACE_SPEEDS', () => {
  it('matches the built-in race -> speed table', () => {
    expect(RACE_SPEEDS.find((race) => race.name === 'Dwarf').speedFeet).toBe(25)
    expect(RACE_SPEEDS.find((race) => race.name === 'Wood Elf').speedFeet).toBe(35)
    expect(RACE_SPEEDS.find((race) => race.name === 'Centaur').speedFeet).toBe(40)
  })
})