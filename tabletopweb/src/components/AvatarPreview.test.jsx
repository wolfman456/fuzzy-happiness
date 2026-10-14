import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AvatarPreview, { AvatarErrorBoundary, AvatarSwatch } from './AvatarPreview'
import { AVATAR_MODELS } from '../lib/avatars'

vi.mock('./AvatarCanvas', () => ({
  default: () => <div data-testid="avatar-canvas" />,
}))

describe('AvatarPreview', () => {
  afterEach(() => {
    delete AVATAR_MODELS['dwarf-M']
  })

  it('falls back to the 2D swatch when no model is registered', () => {
    render(
      <AvatarPreview raceIndex="dwarf" sex="MALE" appearance={{ skinTone: 10, hairTone: 20 }} />,
    )
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
    expect(screen.queryByTestId('avatar-canvas')).not.toBeInTheDocument()
  })

  it('renders the code-split canvas once a model is registered', async () => {
    AVATAR_MODELS['dwarf-M'] = '/models/avatars/dwarf-M.glb'
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

  it('falls back when race or sex is unset', () => {
    render(<AvatarPreview raceIndex="" sex="" appearance={null} />)
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
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
