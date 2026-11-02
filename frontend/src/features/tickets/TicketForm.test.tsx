import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Ticket } from '../../api/types'
import { TicketForm } from './TicketForm'

const loggedTicket = { id: '1', reference: 'TSS-001000' } as Ticket

describe('TicketForm', () => {
  it('explains what is missing and does not call the API', async () => {
    const onSubmit = vi.fn()
    render(<TicketForm onSubmit={onSubmit} />)

    await userEvent.click(screen.getByRole('button', { name: 'Log this request' }))

    expect(await screen.findByText('Give the request a short title.')).toBeInTheDocument()
    expect(screen.getByText('Tell us which company this is for.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('sends a clean request and confirms the reference', async () => {
    const onSubmit = vi.fn().mockResolvedValue(loggedTicket)
    render(<TicketForm onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText('What do you need?'), '  Laptop for Alex ')
    await userEvent.type(screen.getByLabelText('Company'), 'Acme Bury Ltd')
    await userEvent.type(screen.getByLabelText('Details'), 'Alex starts on Monday and needs a laptop.')
    await userEvent.selectOptions(screen.getByLabelText('How urgent is it?'), 'HIGH')
    await userEvent.click(screen.getByRole('button', { name: 'Log this request' }))

    expect(await screen.findByText(/TSS-001000 is logged/)).toBeInTheDocument()
    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Laptop for Alex',
      description: 'Alex starts on Monday and needs a laptop.',
      category: 'NEW_STARTER',
      priority: 'HIGH',
      companyName: 'Acme Bury Ltd',
      requiredBy: null,
    })
  })
})
