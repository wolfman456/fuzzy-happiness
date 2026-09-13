import { describe, expect, it } from 'vitest'
import { ageInYears, isAdult } from './age'

const FEB_29 = new Date(2021, 1, 28)

describe('ageInYears', () => {
  it('counts a completed birthday as a full calendar year', () => {
    expect(ageInYears('2013-09-12', new Date(2026, 8, 12))).toBe(13)
    expect(ageInYears('2013-09-12', new Date(2026, 8, 13))).toBe(13)
  })

  it('does not count the year until the birthday has passed', () => {
    expect(ageInYears('2013-09-12', new Date(2026, 8, 11))).toBe(12)
    expect(ageInYears('2013-09-30', new Date(2026, 8, 29))).toBe(12)
    expect(ageInYears('2013-12-31', new Date(2026, 0, 1))).toBe(12)
  })

  it('treats a leap-day birth like the backend Period.between', () => {
    expect(ageInYears('2008-02-29', FEB_29)).toBe(12)
    expect(ageInYears('2008-02-29', new Date(2021, 2, 1))).toBe(13)
  })

  it('returns NaN for an unparseable date', () => {
    expect(Number.isNaN(ageInYears(''))).toBe(true)
    expect(Number.isNaN(ageInYears('not-a-date'))).toBe(true)
  })
})

describe('isAdult', () => {
  it('accepts a user who is exactly the minimum age', () => {
    expect(isAdult('2013-09-12', 13, new Date(2026, 8, 12))).toBe(true)
  })

  it('rejects a user one day short of the birthday', () => {
    expect(isAdult('2013-09-12', 13, new Date(2026, 8, 11))).toBe(false)
  })

  it('rejects invalid dates', () => {
    expect(isAdult('', 13, new Date(2026, 8, 12))).toBe(false)
  })
})