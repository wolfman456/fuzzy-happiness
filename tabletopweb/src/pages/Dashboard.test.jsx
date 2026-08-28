import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AuthContext } from '../auth/authContext'
import Dashboard from './Dashboard'

function renderDashboard(user) {
  return render(
    <AuthContext.Provider value={{ user }}>
      <Dashboard />
    </AuthContext.Provider>,
  )
}

const user = {
  id: 1,
  username: 'aria',
  displayName: 'Aria',
  email: 'aria@example.com',
  role: 'USER',
  emailVerified: true,
}

describe('Dashboard', () => {
  it('shows the profile and stub cards', () => {
    renderDashboard(user)

    expect(screen.getByText(/welcome, aria/i)).toBeInTheDocument()
    expect(screen.getByText('aria@example.com')).toBeInTheDocument()
    expect(screen.getByText('USER')).toBeInTheDocument()
    expect(screen.getByText(/verified/i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /sessions/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /characters/i })).toBeInTheDocument()
  })

  it('marks an unverified email', () => {
    renderDashboard({ ...user, emailVerified: false })
    expect(screen.getByText(/unverified/i)).toBeInTheDocument()
  })

  it('shows the admin section for ADMIN users', () => {
    renderDashboard({ ...user, role: 'ADMIN' })
    expect(screen.getByRole('heading', { name: /administration/i })).toBeInTheDocument()
  })

  it('hides the admin section for non-admin users', () => {
    renderDashboard(user)
    expect(screen.queryByRole('heading', { name: /administration/i })).not.toBeInTheDocument()
  })
})