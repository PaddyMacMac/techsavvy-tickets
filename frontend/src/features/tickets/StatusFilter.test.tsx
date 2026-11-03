import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { StatusFilter } from './StatusFilter'

describe('StatusFilter', () => {
  it('shows all status choices and marks the selected choice', () => {
    const onChange = vi.fn()

    render(<StatusFilter value="OPEN" onChange={onChange} />)

    expect(screen.getByRole('group', { name: 'Filter by status' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Everything' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: 'Waiting for us' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Closed' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('reports the selected status or the Everything option', () => {
    const onChange = vi.fn()

    render(<StatusFilter value={undefined} onChange={onChange} />)

    fireEvent.click(screen.getByRole('button', { name: 'Waiting for us' }))
    fireEvent.click(screen.getByRole('button', { name: 'Everything' }))

    expect(onChange).toHaveBeenNthCalledWith(1, 'OPEN')
    expect(onChange).toHaveBeenNthCalledWith(2, undefined)
  })
})
