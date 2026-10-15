import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import CharacterFace from './CharacterFace'
import { eyeCss } from '../lib/face'
import { hueCss } from '../lib/avatars'

describe('CharacterFace', () => {
  it('renders a face at the requested size with the given test id', () => {
    render(<CharacterFace raceIndex="human" sex="MALE" appearance={null} size={80} testId="character-avatar" />)
    const face = screen.getByTestId('character-avatar')
    expect(face).toBeInTheDocument()
    expect(face).toHaveStyle({ width: '80px', height: '80px' })
    expect(face.querySelector('svg')).toBeTruthy()
  })

  it('applies the resolved skin, hair and eye colours', () => {
    render(<CharacterFace raceIndex="human" sex={null} appearance={{ skinTone: 10, hairTone: 200, eyeColor: 90, hairStyle: 'short' }} />)
    expect(screen.getByTestId('face-skin').getAttribute('fill')).toBe(hueCss(10))
    expect(screen.getByTestId('face-hair').getAttribute('fill')).toBe(hueCss(200))
    const pupils = screen.getByTestId('face-eyes').querySelectorAll('circle')
    expect(pupils[0].getAttribute('fill')).toBe(eyeCss(90))
  })

  it('omits hair when no style is chosen', () => {
    render(<CharacterFace raceIndex="human" sex={null} appearance={{ hairStyle: '' }} />)
    expect(screen.queryByTestId('face-hair')).not.toBeInTheDocument()
    expect(screen.queryByTestId('face-hair-sides')).not.toBeInTheDocument()
    expect(screen.queryByTestId('face-ponytail')).not.toBeInTheDocument()
  })

  it('draws long hair side panels and a ponytail for those styles', () => {
    const { rerender } = render(<CharacterFace raceIndex="human" sex={null} appearance={{ hairStyle: 'long' }} />)
    expect(screen.getByTestId('face-hair-sides')).toBeInTheDocument()
    rerender(<CharacterFace raceIndex="human" sex={null} appearance={{ hairStyle: 'ponytail' }} />)
    expect(screen.getByTestId('face-ponytail')).toBeInTheDocument()
  })

  it('draws race features: pointed ears, horns and tusks', () => {
    render(<CharacterFace raceIndex="elf" sex={null} appearance={null} />)
    expect(screen.getByTestId('face-ears').querySelectorAll('path').length).toBe(2)

    const { rerender } = render(<CharacterFace raceIndex="tiefling" sex={null} appearance={null} />)
    expect(screen.getByTestId('face-horns')).toBeInTheDocument()
    rerender(<CharacterFace raceIndex="half-orc" sex={null} appearance={null} />)
    expect(screen.getByTestId('face-tusks')).toBeInTheDocument()
    rerender(<CharacterFace raceIndex="human" sex={null} appearance={null} />)
    expect(screen.queryByTestId('face-horns')).not.toBeInTheDocument()
    expect(screen.queryByTestId('face-tusks')).not.toBeInTheDocument()
  })
})
