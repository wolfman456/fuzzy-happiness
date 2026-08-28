import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, setAuthToken, setUnauthorizedHandler } from './api'

const BASE = 'http://localhost:8080'

function mockFetch(status, body) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status,
    ok: status >= 200 && status < 300,
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  }))
}

afterEach(() => {
  vi.unstubAllGlobals()
  setAuthToken(null)
  setUnauthorizedHandler(null)
})

describe('api', () => {
  it('sends JSON body with content type', async () => {
    mockFetch(200, { ok: true })
    await api('/api/auth/login', { method: 'POST', body: { identifier: 'a' } })

    const [url, init] = fetch.mock.calls[0]
    expect(url).toBe(`${BASE}/api/auth/login`)
    expect(init.method).toBe('POST')
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ identifier: 'a' }))
  })

  it('attaches the bearer token when set', async () => {
    setAuthToken('jwt-abc')
    mockFetch(200, { ok: true })
    await api('/api/users/me')

    const [, init] = fetch.mock.calls[0]
    expect(init.headers.Authorization).toBe('Bearer jwt-abc')
  })

  it('parses JSON responses', async () => {
    mockFetch(200, { token: 't', user: { username: 'a' } })
    await expect(api('/api/auth/login')).resolves.toEqual({ token: 't', user: { username: 'a' } })
  })

  it('returns raw text for non-JSON bodies', async () => {
    mockFetch(200, 'Email verified. You can now log in.')
    await expect(api('/api/auth/verify?token=x')).resolves.toBe('Email verified. You can now log in.')
  })

  it('returns null for empty bodies', async () => {
    mockFetch(204, '')
    await expect(api('/api/test')).resolves.toBeNull()
  })

  it('throws ApiError with the server message for errors', async () => {
    mockFetch(409, { status: 409, message: 'Username is already taken' })
    const err = await api('/api/auth/register', { method: 'POST', body: {} }).catch((e) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(409)
    expect(err.message).toBe('Username is already taken')
  })

  it('clears the token and fires the unauthorized handler on 401 when a token was present', async () => {
    const handler = vi.fn()
    setAuthToken('stale')
    setUnauthorizedHandler(handler)
    mockFetch(401, { status: 401, message: 'Authentication required' })

    await expect(api('/api/users/me')).rejects.toThrow('Authentication required')
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not fire the unauthorized handler on a 401 login attempt', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    mockFetch(401, { status: 401, message: 'Bad credentials' })

    await expect(api('/api/auth/login', { method: 'POST', body: {} })).rejects.toThrow('Bad credentials')
    expect(handler).not.toHaveBeenCalled()
  })
})