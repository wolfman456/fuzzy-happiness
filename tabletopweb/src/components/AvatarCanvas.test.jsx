import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import AvatarCanvas from './AvatarCanvas'

const { scene, colorSet } = vi.hoisted(() => {
  const colorSet = vi.fn()
  const scene = {
    clone: () => scene,
    traverse: (fn) => fn({ isMesh: true, material: { name: 'Skin', color: { set: colorSet } } }),
  }
  return { scene, colorSet }
})

vi.mock('@react-three/fiber', () => ({
  Canvas: ({ children }) => <div data-testid="r3f-canvas">{children}</div>,
}))

vi.mock('@react-three/drei', () => ({
  useGLTF: () => ({ scene }),
  OrbitControls: () => null,
}))

describe('AvatarCanvas', () => {
  it('mounts the canvas and tints the loaded model', async () => {
    render(
      <AvatarCanvas modelUrl="/models/avatars/dwarf-M.glb" appearance={{ skinTone: 10, hairTone: 200 }} />,
    )
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
    expect(screen.getByTestId('r3f-canvas')).toBeInTheDocument()
    await waitFor(() => expect(colorSet).toHaveBeenCalledWith('hsl(10 45% 60%)'))
  })

  it('honours a custom testId and size', () => {
    render(
      <AvatarCanvas modelUrl="/m.glb" appearance={null} size={80} testId="character-avatar" />,
    )
    expect(screen.getByTestId('character-avatar')).toHaveStyle({ width: '80px', height: '80px' })
  })
})
