import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import AvatarCanvas from './AvatarCanvas'

const { scene, colorSet, socketAdd } = vi.hoisted(() => {
  const colorSet = vi.fn()
  const socketAdd = vi.fn()
  const socket = { add: socketAdd, remove: vi.fn() }
  const scene = {
    clone: () => scene,
    traverse: (fn) => fn({ isMesh: true, material: { name: 'Skin', color: { set: colorSet } } }),
    getObjectByName: (name) => (name === 'socket_horns' ? socket : null),
  }
  return { scene, colorSet, socketAdd }
})

vi.mock('@react-three/fiber', () => ({
  Canvas: ({ children }) => <div data-testid="r3f-canvas">{children}</div>,
}))

vi.mock('@react-three/drei', () => ({
  useGLTF: () => ({ scene }),
  OrbitControls: () => null,
}))

const BASE = { base: '/models/avatars/base-M.glb', parts: [], scale: 1 }

describe('AvatarCanvas', () => {
  it('mounts the canvas and tints the loaded model', async () => {
    render(
      <AvatarCanvas avatar={BASE} appearance={{ skinTone: 10, hairTone: 200 }} />,
    )
    expect(screen.getByTestId('appearance-swatch')).toBeInTheDocument()
    expect(screen.getByTestId('r3f-canvas')).toBeInTheDocument()
    await waitFor(() => expect(colorSet).toHaveBeenCalledWith('hsl(10 45% 60%)'))
  })

  it('attaches registered parts at their socket', async () => {
    render(
      <AvatarCanvas
        avatar={{
          ...BASE,
          parts: [{ socket: 'socket_horns', url: '/models/avatars/parts/horns/tiefling.glb' }],
        }}
        appearance={null}
      />,
    )
    await waitFor(() => expect(socketAdd).toHaveBeenCalled())
  })

  it('honours a custom testId and size', () => {
    render(
      <AvatarCanvas avatar={BASE} appearance={null} size={80} testId="character-avatar" />,
    )
    expect(screen.getByTestId('character-avatar')).toHaveStyle({ width: '80px', height: '80px' })
  })
})
