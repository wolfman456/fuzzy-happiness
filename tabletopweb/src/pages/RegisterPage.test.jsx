import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/api'
import { AuthContext } from '../auth/authContext'
import RegisterPage from './RegisterPage'

function renderRegister(value) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
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

function fillValidForm() {
  fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: 'Aria' } })
  fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'aria@example.com' } })
  fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: '1990-01-15' } })
  fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'aria' } })
  fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password1!' } })
}

describe('RegisterPage', () => {
  it('registers a valid profile and shows the verify prompt', async () => {
    const register = vi.fn().mockResolvedValue({})
    renderRegister({ ...baseValue, register })
    fillValidForm()
    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/check your inbox/i)).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith(expect.objectContaining({ username: 'aria' }))
  })

  it('rejects a weak password client-side', async () => {
    renderRegister({ ...baseValue })
    fillValidForm()
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'short' } })
    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/8\+ characters/i)).toBeInTheDocument()
    expect(baseValue.register).not.toHaveBeenCalled()
  })

  it('rejects an underage date of birth client-side', async () => {
    renderRegister({ ...baseValue })
    fillValidForm()
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: '2015-01-01' } })
    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/at least 13/i)).toBeInTheDocument()
    expect(baseValue.register).not.toHaveBeenCalled()
  })

  it('maps a username conflict to the username field', async () => {
    renderRegister({
      ...baseValue,
      register: vi.fn().mockRejectedValue(new ApiError(409, 'Username is already taken')),
    })
    fillValidForm()
    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('Username is already taken')).toBeInTheDocument()
  })
})