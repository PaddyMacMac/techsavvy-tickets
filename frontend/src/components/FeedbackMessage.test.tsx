import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { FeedbackMessage } from './FeedbackMessage'

describe('FeedbackMessage', () => {
  it('exposes errors as alerts', () => {
    render(<FeedbackMessage>Something failed.</FeedbackMessage>)
    expect(screen.getByRole('alert')).toHaveTextContent('Something failed.')
  })

  it('exposes successes as status messages', () => {
    render(<FeedbackMessage tone="success">Saved.</FeedbackMessage>)
    expect(screen.getByRole('status')).toHaveTextContent('Saved.')
  })
})
