import { Suspense, useEffect, useMemo } from 'react'
import { Canvas } from '@react-three/fiber'
import { OrbitControls, useGLTF } from '@react-three/drei'
import { applyAvatarTints, attachToSocket } from '../lib/avatars'

/** Loads one part GLB and parents it at its socket on the (cloned) base. */
function AvatarPart({ url, socketName, parent }) {
  const { scene } = useGLTF(url)

  useEffect(() => {
    if (!parent) return undefined
    const part = scene.clone(true)
    if (!attachToSocket(parent, socketName, part)) return undefined
    return () => {
      parent.getObjectByName(socketName)?.remove(part)
    }
  }, [scene, socketName, parent])

  return null
}

function AvatarModel({ avatar, appearance }) {
  const { scene } = useGLTF(avatar.base)
  const model = useMemo(() => scene.clone(true), [scene])

  useEffect(() => {
    applyAvatarTints(model, appearance)
  }, [model, appearance])

  return (
    <group scale={avatar.scale ?? 1}>
      <primitive object={model} />
      {(avatar.parts ?? []).map((part) => (
        <AvatarPart key={part.url} url={part.url} socketName={part.socket} parent={model} />
      ))}
    </group>
  )
}

/**
 * The lazily-loaded 3D half of `AvatarPreview`. Loads the shared race×sex base
 * GLB, attaches the race's parts at their sockets, applies the skin/hair tints
 * and lets the player orbit. Kept in its own module so three.js/R3F stay out of
 * the base bundle until a base asset is available and WebGL is present.
 */
export default function AvatarCanvas({
  avatar,
  appearance,
  size = 64,
  testId = 'appearance-swatch',
  className = '',
}) {
  return (
    <div
      data-testid={testId}
      aria-hidden="true"
      className={`shrink-0 overflow-hidden rounded-full border border-zinc-300 bg-zinc-100 ${className}`}
      style={{ width: size, height: size }}
    >
      <Canvas
        camera={{ position: [0, 1.5, 3.4], fov: 32 }}
        dpr={[1, 2]}
        gl={{ preserveDrawingBuffer: true }}
      >
        <ambientLight intensity={0.9} />
        <directionalLight position={[3, 6, 4]} intensity={1.15} />
        <Suspense fallback={null}>
          <AvatarModel avatar={avatar} appearance={appearance} />
        </Suspense>
        <OrbitControls enablePan={false} enableZoom={false} target={[0, 1, 0]} />
      </Canvas>
    </div>
  )
}
