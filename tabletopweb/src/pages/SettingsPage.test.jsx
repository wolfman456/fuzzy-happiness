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
    expect(screen.getByRole('button', { name: /save profile/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /change password/i })).toBeInTheDocument()
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

  it('updates the profile details and refreshes the session', async () => {
    const refreshMe = vi.fn().mockResolvedValue(user)
    mockFetch(200, { ...user, displayName: 'Aria the Brave' })
    renderSettings({ refreshMe })

    fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: 'Aria the Brave' } })
    fireEvent.change(screen.getByLabelText(/real name/i), { target: { value: 'Aria Ashton' } })
    fireEvent.click(screen.getByRole('button', { name: /save profile/i }))

    expect(await screen.findByText(/profile updated/i)).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/users/me/profile',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ displayName: 'Aria the Brave', realName: 'Aria Ashton' }),
      }),
    )
    expect(refreshMe).toHaveBeenCalledTimes(1)
  })

  it('shows the server error when the profile update is rejected', async () => {
    mockFetch(400, { status: 400, message: 'Real name must be 150 characters or fewer' })
    renderSettings()

    fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: 'Aria' } })
    fireEvent.change(screen.getByLabelText(/real name/i), { target: { value: 'x'.repeat(160) } })
    fireEvent.click(screen.getByRole('button', { name: /save profile/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/150 characters or fewer/i)
  })

  it('changes the password and clears the fields', async () => {
    mockFetch(200, user)
    renderSettings()

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'OldPass1!' } })
    fireEvent.change(screen.getByLabelText(/^New password/i), { target: { value: 'NewPass2!' } })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'NewPass2!' } })
    fireEvent.click(screen.getByRole('button', { name: /change password/i }))

    expect(await screen.findByText(/password changed/i)).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/users/me/password',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({
          currentPassword: 'OldPass1!',
          newPassword: 'NewPass2!',
          confirmPassword: 'NewPass2!',
        }),
      }),
    )
    expect(screen.getByLabelText(/current password/i).value).toBe('')
    expect(screen.getByLabelText(/^New password/i).value).toBe('')
    expect(screen.getByLabelText(/confirm new password/i).value).toBe('')
  })

  it('rejects a weak new password locally', async () => {
    mockFetch(200, user)
    renderSettings()

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'OldPass1!' } })
    fireEvent.change(screen.getByLabelText(/^New password/i), { target: { value: 'short' } })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'short' } })
    fireEvent.click(screen.getByRole('button', { name: /change password/i }))

    expect(screen.getAllByText(/8\+ characters with an upper, lower, digit and symbol/i)).toHaveLength(2)
    expect(fetch).not.toHaveBeenCalled()
  })

  it('rejects a mismatched password confirmation locally', async () => {
    mockFetch(200, user)
    renderSettings()

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'OldPass1!' } })
    fireEvent.change(screen.getByLabelText(/^New password/i), { target: { value: 'NewPass2!' } })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'Different2!' } })
    fireEvent.click(screen.getByRole('button', { name: /change password/i }))

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('shows the server error when the current password is wrong', async () => {
    mockFetch(400, { status: 400, message: 'Current password is incorrect' })
    renderSettings()

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'WrongPass1!' } })
    fireEvent.change(screen.getByLabelText(/^New password/i), { target: { value: 'NewPass2!' } })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'NewPass2!' } })
    fireEvent.click(screen.getByRole('button', { name: /change password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/current password is incorrect/i)
  })
})