import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Alert, Card, EmptyState, LoadingState, PageHeading } from './ui'

describe('ui components', () => {
  it('renders an error Alert with an alert role', () => {
    render(<Alert tone="error">Something broke</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Something broke')
  })

  it('renders warning, status and success Alerts as status', () => {
    render(
      <>
        <Alert tone="warning">Careful</Alert>
        <Alert tone="status">Heads up</Alert>
        <Alert tone="success">All good</Alert>
      </>,
    )
    expect(screen.getAllByRole('status')).toHaveLength(3)
  })

  it('falls back to the error tone for an unknown tone', () => {
    render(<Alert tone="nonsense">Uh oh</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Uh oh')
  })

  it('renders a PageHeading with an optional subtitle', () => {
    render(<PageHeading title="My characters" subtitle="Build a hero" />)
    expect(screen.getByRole('heading', { name: 'My characters' })).toBeInTheDocument()
    expect(screen.getByText('Build a hero')).toBeInTheDocument()
  })

  it('renders a Card wrapper', () => {
    render(<Card>Panel body</Card>)
    expect(screen.getByText('Panel body')).toBeInTheDocument()
  })

  it('renders a LoadingState with a custom message', () => {
    render(<LoadingState message="Loading heroes…" />)
    expect(screen.getByRole('status')).toHaveTextContent('Loading heroes…')
  })

  it('renders an EmptyState with body and an action', () => {
    render(
      <EmptyState title="No characters yet" action={<button type="button">Build one</button>}>
        Start by building a hero.
      </EmptyState>,
    )
    const title = screen.getByText('No characters yet')
    expect(title).toBeInTheDocument()
    expect(title.closest('div')).toHaveClass('bg-white')
    expect(screen.getByText('Start by building a hero.')).toHaveClass('text-zinc-600')
    expect(screen.getByRole('button', { name: 'Build one' })).toBeInTheDocument()
  })
})
