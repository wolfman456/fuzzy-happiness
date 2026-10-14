import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AvatarPreview, { AvatarErrorBoundary, AvatarSwatch } from './AvatarPreview'
import { supportsWebGL } from '../lib/avatars'

vi.mock('./AvatarCanvas', () => ({
  default: () => <div data-testid="avatar-canvas" />,
}))

vi.mock('../lib/avatars', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, supportsWebGL: vi.fn(() => true) }
})

afterEach(() => {
  supportsWebGL.mockReturnValue(true)
})

describe('AvatarPreview', () => {
  it('falls back to the 2D swatch when sex is unset', () => {
    render(<AvatarPreview raceIndex="dwarf" sex="" appearance={{ skinTone: 10, hairTone: 20 }} />)
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
    expect(screen.queryByTestId('avatar-canvas')).not.toBeInTheDocument()
  })

  it('renders the code-split canvas for a valid sex', async () => {
    render(
      <AvatarPreview
        raceIndex="dwarf"
        sex="MALE"
        appearance={{ skinTone: 10, hairTone: 20 }}
        testId="character-avatar"
      />,
    )
    expect(await screen.findByTestId('avatar-canvas')).toBeInTheDocument()
  })

  it('falls back to the swatch when WebGL is unavailable', () => {
    supportsWebGL.mockReturnValue(false)
    render(<AvatarPreview raceIndex="dwarf" sex="MALE" appearance={null} />)
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
    expect(screen.queryByTestId('avatar-canvas')).not.toBeInTheDocument()
  })
})

describe('AvatarSwatch', () => {
  it('renders with default tints when appearance is missing', () => {
    render(<AvatarSwatch appearance={null} size={40} testId="character-avatar" />)
    expect(screen.getByTestId('character-avatar')).toBeInTheDocument()
  })
})

describe('AvatarErrorBoundary', () => {
  it('renders the fallback when a child throws', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    function Boom() {
      throw new Error('WebGL unavailable')
    }

    render(
      <AvatarErrorBoundary fallback={<span>swatch fallback</span>}>
        <Boom />
      </AvatarErrorBoundary>,
    )

    expect(screen.getByText('swatch fallback')).toBeInTheDocument()
    consoleError.mockRestore()
  })
})
