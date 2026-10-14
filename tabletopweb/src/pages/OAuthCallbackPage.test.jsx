import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../auth/authContext'
import OAuthCallbackPage from './OAuthCallbackPage'

function renderCallback(value) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/oauth/callback']}>
        <Routes>
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route path="/" element={<div>home</div>} />
          <Route path="/login" element={<div>login</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

const baseValue = {
  user: null,
  loading: false,
  isAuthenticated: false,
  login: vi.fn(),
  loginWithToken: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  refreshMe: vi.fn(),
}

afterEach(() => {
  window.location.hash = ''
  vi.restoreAllMocks()
})

describe('OAuthCallbackPage', () => {
  it('exchanges the fragment token and lands on home', async () => {
    window.location.hash = '#token=abc.def&tokenType=Bearer&expiresInSeconds=3600'
    const loginWithToken = vi.fn().mockResolvedValue({ username: 'aria' })
    renderCallback({ ...baseValue, loginWithToken })

    expect(loginWithToken).toHaveBeenCalledWith('abc.def')
    expect(await screen.findByText('home')).toBeInTheDocument()
  })

  it('shows an error when the fragment carries no token', async () => {
    window.location.hash = ''
    const loginWithToken = vi.fn()
    renderCallback({ ...baseValue, loginWithToken })

    expect(await screen.findByRole('alert')).toHaveTextContent(/did not return a token/i)
    expect(loginWithToken).not.toHaveBeenCalled()
  })

  it('shows an error when the token is rejected', async () => {
    window.location.hash = '#token=stale'
    const loginWithToken = vi.fn().mockRejectedValue(new Error('nope'))
    renderCallback({ ...baseValue, loginWithToken })

    expect(await screen.findByRole('alert')).toHaveTextContent(/no longer valid/i)
  })

  it('redirects home when already authenticated', () => {
    const loginWithToken = vi.fn()
    renderCallback({ ...baseValue, isAuthenticated: true, user: { username: 'aria' }, loginWithToken })

    expect(screen.getByText('home')).toBeInTheDocument()
    expect(loginWithToken).not.toHaveBeenCalled()
  })
})
