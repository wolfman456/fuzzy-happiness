import { Suspense, useEffect, useMemo } from 'react'
import { Canvas } from '@react-three/fiber'
import { OrbitControls, useGLTF } from '@react-three/drei'
import { applyAvatarTints } from '../lib/avatars'

function AvatarModel({ url, appearance }) {
  const { scene } = useGLTF(url)
  const model = useMemo(() => scene.clone(true), [scene])

  useEffect(() => {
    applyAvatarTints(model, appearance)
  }, [model, appearance])

  return <primitive object={model} />
}

/**
 * The lazily-loaded 3D half of `AvatarPreview`. Loads the race×sex GLB, applies
 * the skin/hair tints and lets the player orbit. Kept in its own module so
 * three.js/R3F stay out of the base bundle until an asset is curated.
 */
export default function AvatarCanvas({
  modelUrl,
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
      <Canvas camera={{ position: [0, 1.5, 3.4], fov: 32 }} dpr={[1, 2]}>
        <ambientLight intensity={0.9} />
        <directionalLight position={[3, 6, 4]} intensity={1.15} />
        <Suspense fallback={null}>
          <AvatarModel url={modelUrl} appearance={appearance} />
        </Suspense>
        <OrbitControls enablePan={false} enableZoom={false} target={[0, 1, 0]} />
      </Canvas>
    </div>
  )
}
