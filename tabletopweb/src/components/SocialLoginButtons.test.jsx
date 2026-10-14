import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import SocialLoginButtons from './SocialLoginButtons'

function stubProviders(payload, { ok = true } = {}) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    status: ok ? 200 : 500,
    ok,
    text: () => Promise.resolve(JSON.stringify(payload)),
  }))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('SocialLoginButtons', () => {
  it('renders a link per configured provider', async () => {
    stubProviders([
      { id: 'google', label: 'Google', authorizationPath: '/oauth2/authorization/google' },
      { id: 'github', label: 'GitHub', authorizationPath: '/oauth2/authorization/github' },
    ])

    render(<SocialLoginButtons />)

    const google = await screen.findByTestId('social-google')
    expect(google).toHaveAttribute('href', 'http://localhost:8080/oauth2/authorization/google')
    expect(google).toHaveTextContent('Continue with Google')
    expect(screen.getByTestId('social-github')).toHaveAttribute(
      'href',
      'http://localhost:8080/oauth2/authorization/github',
    )
  })

  it('renders nothing when no providers are configured', async () => {
    stubProviders([])

    render(<SocialLoginButtons />)

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(screen.queryByTestId('social-login')).not.toBeInTheDocument()
  })

  it('renders nothing and does not throw when the providers call fails', async () => {
    stubProviders({ status: 500, message: 'boom' }, { ok: false })

    render(<SocialLoginButtons />)

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(screen.queryByTestId('social-login')).not.toBeInTheDocument()
  })

  it('ignores a non-array payload', async () => {
    stubProviders({ unexpected: true })

    render(<SocialLoginButtons />)

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    expect(screen.queryByTestId('social-login')).not.toBeInTheDocument()
  })
})
