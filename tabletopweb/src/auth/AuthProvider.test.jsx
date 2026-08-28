import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

const BASE = 'http://localhost:8080'

function Probe() {
  const { user, loading, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="user">{user ? user.username : 'none'}</span>
      <button type="button" onClick={() => login({ identifier: 'aria', password: 'Password1!' })}>
        login
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