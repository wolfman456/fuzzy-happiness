import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../auth/authContext'
import { setAuthToken } from '../lib/api'
import SettingsPage from './SettingsPage'

const user = {
  id: 1,
  username: 'aria',
  displayName: 'Aria',
  email: 'aria@example.com',
  role: 'USER',
  emailVerified: true,
}

function mockFetch(status, body) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status,
    ok: status >= 200 && status < 300,
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  }))
}

function renderSettings(overrides = {}) {
  const refreshMe = vi.fn().mockResolvedValue(user)
  return {
    refreshMe,
    ...render(
      <AuthContext.Provider value={{ user, refreshMe, ...overrides }}>
        <SettingsPage />
      </AuthContext.Provider>,
    ),
  }
}

afterEach(() => {
  vi.unstubAllGlobals()
  setAuthToken(null)
})

describe('SettingsPage', () => {
  it('shows the current account details', () => {
    renderSettings()
    expect(screen.getByText('aria')).toBeInTheDocument()
    expect(screen.getByText('aria@example.com')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /change username/i })).toBeInTheDocument()
  })

  it('updates the username and refreshes the session', async () => {
    const refreshed = { ...user, username: 'drake' }
    const refreshMe = vi.fn().mockResolvedValue(refreshed)
    mockFetch(200, refreshed)
    renderSettings({ refreshMe })

    fireEvent.change(screen.getByLabelText(/new username/i), { target: { value: 'drake' } })
    fireEvent.click(screen.getByRole('button', { name: /change username/i }))

    expect(await screen.findByText(/username changed to drake/i)).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/users/me/username',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ username: 'drake' }),
      }),
    )
    expect(refreshMe).toHaveBeenCalledTimes(1)
  })

  it('shows a conflict message when the username is taken', async () => {
    mockFetch(409, { status: 409, message: 'Username is already taken' })
    renderSettings()

    fireEvent.change(screen.getByLabelText(/new username/i), { target: { value: 'taken' } })
    fireEvent.click(screen.getByRole('button', { name: /change username/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/already taken/i)
  })

  it('rejects an invalid username locally', async () => {
    mockFetch(200, user)
    renderSettings()
    fireEvent.change(screen.getByLabelText(/new username/i), { target: { value: 'x' } })
    fireEvent.click(screen.getByRole('button', { name: /change username/i }))
    expect(screen.getByText(/username must be 3–30 characters/i)).toBeInTheDocument()
    expect(fetch).not.toHaveBeenCalled()
  })
})