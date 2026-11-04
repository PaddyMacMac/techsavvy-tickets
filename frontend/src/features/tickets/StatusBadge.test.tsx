import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from './StatusBadge'

describe('ticket StatusBadge', () => {
  it('shows plain-English wording, not the raw status code', () => {
    render(<StatusBadge status="WAITING_ON_CLIENT" />)
    expect(screen.getByText('Waiting for you')).toBeInTheDocument()
  })
})
