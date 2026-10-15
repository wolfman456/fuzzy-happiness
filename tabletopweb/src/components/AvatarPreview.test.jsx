import { useEffect } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AvatarPreview, { AvatarErrorBoundary, AvatarSwatch } from './AvatarPreview'
import { supportsWebGL } from '../lib/avatars'
import {
  avatarSnapshotKey,
  clearAvatarSnapshots,
  getAvatarSnapshot,
  setAvatarSnapshot,
} from '../lib/avatarSnapshot'

vi.mock('./AvatarCanvas', () => {
  function MockAvatarCanvas({ capture, onCapture }) {
    useEffect(() => {
      if (capture && onCapture) onCapture('data:image/png;base64,ZZZ')
    }, [capture, onCapture])
    return <div data-testid="avatar-canvas" />
  }
  return { default: MockAvatarCanvas }
})

vi.mock('../lib/avatars', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, supportsWebGL: vi.fn(() => true) }
})

afterEach(() => {
  supportsWebGL.mockReturnValue(true)
  clearAvatarSnapshots()
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

describe('AvatarPreview portrait snapshot', () => {
  const appearance = { skinTone: 120, hairTone: 300 }
  const key = avatarSnapshotKey('dwarf', 'FEMALE', appearance)

  it('renders a cached snapshot as an image without mounting the canvas', () => {
    setAvatarSnapshot(key, 'data:image/png;base64,CACHED')
    const { container } = render(
      <AvatarPreview
        raceIndex="dwarf"
        sex="FEMALE"
        appearance={appearance}
        portrait
        testId="character-avatar"
      />,
    )

    const img = container.querySelector('img[data-testid="character-avatar"]')
    expect(img).toBeTruthy()
    expect(img.getAttribute('src')).toBe('data:image/png;base64,CACHED')
    expect(screen.queryByTestId('avatar-canvas')).not.toBeInTheDocument()
  })

  it('captures and caches a portrait on a cache miss', async () => {
    const { container } = render(
      <AvatarPreview
        raceIndex="dwarf"
        sex="FEMALE"
        appearance={appearance}
        portrait
        testId="character-avatar"
      />,
    )

    await waitFor(() =>
      expect(container.querySelector('img[data-testid="character-avatar"]')).toBeTruthy(),
    )
    expect(getAvatarSnapshot(key)).toBe('data:image/png;base64,ZZZ')
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
