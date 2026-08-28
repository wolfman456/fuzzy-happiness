import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/api'
import { AuthContext } from '../auth/authContext'
import LoginPage from './LoginPage'

function renderLogin(value) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/login']}>
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
  register: vi.fn(),
  logout: vi.fn(),
  refreshMe: vi.fn(),
}

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

  it('shows the verify-email hint on 403', async () => {
    renderLogin({
      ...baseValue,
      login: vi.fn().mockRejectedValue(new ApiError(403, 'Email not verified.')),
    })
    fireEvent.change(screen.getByLabelText(/username or email/i), { target: { value: 'aria' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'Password1!' } })
    fireEvent.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/not verified/i)
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
})