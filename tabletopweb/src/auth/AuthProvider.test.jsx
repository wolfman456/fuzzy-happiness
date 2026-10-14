import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

const BASE = 'http://localhost:8080'

function Probe() {
  const { user, loading, login, loginWithToken, logout, refreshMe } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="user">{user ? user.username : 'none'}</span>
      <button type="button" onClick={() => login({ identifier: 'aria', password: 'Password1!' })}>
        login
      </button>
      <button type="button" onClick={() => loginWithToken('jwt-oauth').catch(() => {})}>
        loginWithToken
      </button>
      <button type="button" onClick={refreshMe}>
        refresh
      </button>
      <button type="button" onClick={logout}>
        logout
      </button>
    </div>
  )
}

function mockFetchOnce(status, body) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status,
    ok: status >= 200 && status < 300,
    text: () => Promise.resolve(JSON.stringify(body)),
  }))
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('AuthProvider', () => {
  it('restores a stored session by fetching /users/me', async () => {
    localStorage.setItem('tt.auth', JSON.stringify({ token: 'jwt-1', user: { username: 'aria' } }))
    mockFetchOnce(200, { id: 1, username: 'aria', displayName: 'Aria', email: 'a@e.com', role: 'USER', emailVerified: true })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('loading').textContent).toBe('true')
    await screen.findByText('aria')
    expect(fetch).toHaveBeenCalledWith(
      `${BASE}/api/users/me`,
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer jwt-1' }) }),
    )
  })

  it('clears the session when a stored token is rejected', async () => {
    localStorage.setItem('tt.auth', JSON.stringify({ token: 'expired', user: { username: 'aria' } }))
    mockFetchOnce(401, { status: 401, message: 'Authentication required' })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(localStorage.getItem('tt.auth')).toBeNull())
    expect(screen.getByTestId('loading').textContent).toBe('false')
    expect(screen.getByTestId('user').textContent).toBe('none')
  })

  it('logs in, persists the session and exposes the user', async () => {
    mockFetchOnce(200, {
      token: 'jwt-new',
      tokenType: 'Bearer',
      expiresInSeconds: 86400,
      user: { id: 2, username: 'sam', displayName: 'Sam', email: 's@e.com', role: 'USER', emailVerified: true },
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    fireEvent.click(screen.getByText('login'))
    await screen.findByText('sam')
    expect(JSON.parse(localStorage.getItem('tt.auth'))).toMatchObject({ token: 'jwt-new' })
    expect(fetch.mock.calls[0][1].body).toBe(JSON.stringify({ identifier: 'aria', password: 'Password1!' }))
  })

  it('adopts a social-login token by fetching the user', async () => {
    mockFetchOnce(200, { id: 3, username: 'ghost', displayName: 'Ghost', email: 'g@e.com', role: 'USER', emailVerified: true })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    fireEvent.click(screen.getByText('loginWithToken'))
    await screen.findByText('ghost')
    expect(JSON.parse(localStorage.getItem('tt.auth'))).toMatchObject({ token: 'jwt-oauth' })
    expect(fetch.mock.calls[0][0]).toBe(`${BASE}/api/users/me`)
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer jwt-oauth')
  })

  it('rejects an invalid social-login token and keeps the session empty', async () => {
    mockFetchOnce(401, { status: 401, message: 'Authentication required' })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    fireEvent.click(screen.getByText('loginWithToken'))
    await waitFor(() => expect(localStorage.getItem('tt.auth')).toBeNull())
    expect(screen.getByTestId('user').textContent).toBe('none')
  })

  it('refreshMe persists a refreshed user to the stored session', async () => {
    localStorage.setItem('tt.auth', JSON.stringify({ token: 'jwt-1', user: { username: 'aria' } }))
    mockFetchOnce(200, { id: 1, username: 'drake', displayName: 'Aria', email: 'a@e.com', role: 'USER', emailVerified: true })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await screen.findByText('drake')
    fireEvent.click(screen.getByText('refresh'))
    await waitFor(() => expect(JSON.parse(localStorage.getItem('tt.auth')).user.username).toBe('drake'))
    expect(screen.getByTestId('user').textContent).toBe('drake')
  })

  it('logs out and clears the session', async () => {
    localStorage.setItem('tt.auth', JSON.stringify({ token: 'jwt-1', user: { username: 'aria' } }))
    mockFetchOnce(200, { id: 1, username: 'aria', displayName: 'Aria', email: 'a@e.com', role: 'USER', emailVerified: true })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await screen.findByText('aria')
    fireEvent.click(screen.getByText('logout'))
    expect(localStorage.getItem('tt.auth')).toBeNull()
    expect(screen.getByTestId('user').textContent).toBe('none')
  })
})