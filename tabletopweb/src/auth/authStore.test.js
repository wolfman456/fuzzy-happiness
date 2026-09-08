import { beforeEach, describe, expect, it } from 'vitest'
import { clearSession, loadStoredSession, saveSession } from './authStore'

describe('authStore', () => {
  beforeEach(() => localStorage.clear())

  it('returns null when nothing is stored', () => {
    expect(loadStoredSession()).toBeNull()
  })

  it('round-trips a saved session', () => {
    saveSession('jwt-1', { username: 'aria' })
    expect(loadStoredSession()).toEqual({ token: 'jwt-1', user: { username: 'aria' } })
  })

  it('clears the session', () => {
    saveSession('jwt-1', { username: 'aria' })
    clearSession()
    expect(loadStoredSession()).toBeNull()
  })

  it('returns null for corrupt JSON', () => {
    localStorage.setItem('tt.auth', 'not-json{{')
    expect(loadStoredSession()).toBeNull()
  })

  it('returns null for an empty token', () => {
    localStorage.setItem('tt.auth', JSON.stringify({ token: '', user: null }))
    expect(loadStoredSession()).toBeNull()
  })
})