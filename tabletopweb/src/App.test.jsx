import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import App from './App.jsx'

test('renders the get started heading', () => {
  render(<App />)
  expect(
    screen.getByRole('heading', { name: /get started/i }),
  ).toBeInTheDocument()
})