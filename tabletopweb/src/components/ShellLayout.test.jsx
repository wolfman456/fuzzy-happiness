import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import ShellLayout from './ShellLayout'

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({
    user: { username: 'aria', displayName: 'Aria', role: 'USER' },
    logout: vi.fn(),
  }),
}))

function renderShell() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <ShellLayout />
    </MemoryRouter>,
  )
}

describe('ShellLayout', () => {
  it('keeps the navigation drawer closed by default', () => {
    renderShell()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /sessions/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument()
  })

  it('opens the drawer from the hamburger button', () => {
    renderShell()
    fireEvent.click(screen.getByRole('button', { name: /open menu/i }))
    expect(screen.getByRole('navigation')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /sessions/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^close menu$/i })).toBeInTheDocument()
  })

  it('closes the drawer when the backdrop is clicked', () => {
    renderShell()
    fireEvent.click(screen.getByRole('button', { name: /open menu/i }))
    fireEvent.click(screen.getByRole('button', { name: /close menu overlay/i }))
    expect(screen.queryByRole('link', { name: /sessions/i })).not.toBeInTheDocument()
  })

  it('closes the drawer with the Escape key', () => {
    renderShell()
    fireEvent.click(screen.getByRole('button', { name: /open menu/i }))
    fireEvent.keyDown(document.body, { key: 'Escape' })
    expect(screen.queryByRole('link', { name: /sessions/i })).not.toBeInTheDocument()
  })

  it('navigates and closes the drawer when a drawer link is clicked', () => {
    renderShell()
    fireEvent.click(screen.getByRole('button', { name: /open menu/i }))
    fireEvent.click(screen.getByRole('link', { name: /characters/i }))
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })
})