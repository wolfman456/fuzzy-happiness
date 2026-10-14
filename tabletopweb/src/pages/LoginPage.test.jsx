import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/api'
import { AuthContext } from '../auth/authContext'
import LoginPage from './LoginPage'

function renderLogin(value, initialEntry = '/login') {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>home</div>} />
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

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status: 200,
    ok: true,
    text: () => Promise.resolve('[]'),
  }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LoginPage', () => {
  it('logs in with identifier and password then navigates home', async () => {
    const login = vi.fn().mockResolvedValue({})
    renderLogin({ ...baseValue, login })
    fireEvent.change(screen.getByLabelText(/username or email/i), { target: { value: 'aria@example.com' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'Password1!' } })
    fireEvent.click(screen.getByRole('button', { name: /log in/i }))

    const home = await screen.findByText('home')
    expect(home).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({ identifier: 'aria@example.com', password: 'Password1!' })
  })

  it('shows an invalid-credentials message on 401', async () => {
    renderLogin({
      ...baseValue,
      login: vi.fn().mockRejectedValue(new ApiError(401, 'Bad credentials')),
    })
    fireEvent.change(screen.getByLabelText(/username or email/i), { target: { value: 'ghost' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'Password1!' } })
    fireEvent.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid/i)
  })

  it('redirects to home when already authenticated', () => {
    renderLogin({ ...baseValue, isAuthenticated: true, user: { username: 'aria' } })
    expect(screen.getByText('home')).toBeInTheDocument()
  })

  it('surfaces a social-login failure from the query string', () => {
    renderLogin(baseValue, '/login?oauthError=social_login_failed&oauthMessage=Email%20already%20linked')
    expect(screen.getByRole('alert')).toHaveTextContent('Email already linked')
  })
})