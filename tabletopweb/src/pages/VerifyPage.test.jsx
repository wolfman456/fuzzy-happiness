import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import VerifyPage from './VerifyPage'

function renderVerify(initialEntry) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/verify" element={<VerifyPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function mockFetch(status, body) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status,
    ok: status >= 200 && status < 300,
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  }))
}

afterEach(() => vi.unstubAllGlobals())

describe('VerifyPage', () => {
  it('confirms a valid token', async () => {
    mockFetch(200, 'Email verified. You can now log in.')
    renderVerify('/verify?token=abc')

    expect(await screen.findByText(/email verified/i)).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/verify?token=abc'),
      expect.anything(),
    )
  })

  it('shows the server message for a rejected token', async () => {
    mockFetch(400, { status: 400, message: 'Invalid or expired token' })
    renderVerify('/verify?token=expired')

    expect(await screen.findByText(/verification failed/i)).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid or expired token')
  })

  it('handles a missing token', () => {
    renderVerify('/verify')
    expect(screen.getByText(/no verification token provided/i)).toBeInTheDocument()
  })
})