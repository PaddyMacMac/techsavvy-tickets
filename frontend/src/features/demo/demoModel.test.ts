import { describe, expect, it } from 'vitest'
import { createSampleTickets, getAllowedTransitions, summarizeTickets } from './demoModel'

describe('demoModel', () => {
  it('creates sample tickets with unique IDs and valid status transitions', () => {
    const tickets = createSampleTickets()
    const ticketIds = tickets.map(({ id }) => id)

    expect(tickets).toHaveLength(5)
    expect(new Set(ticketIds).size).toBe(ticketIds.length)
    expect(tickets[0].allowedTransitions).toEqual(['IN_PROGRESS', 'CLOSED'])
    expect(getAllowedTransitions('CLOSED')).toEqual([])
  })

  it('summarizes ticket statuses and response deadlines', () => {
    const summary = summarizeTickets(createSampleTickets())

    expect(summary.byStatus).toEqual({
      OPEN: 2,
      IN_PROGRESS: 1,
      WAITING_ON_CLIENT: 1,
      RESOLVED: 1,
      CLOSED: 0,
    })
    expect(summary.awaitingResponseOverdue).toBe(1)
    expect(summary.nextResponseDue?.reference).toBe('TSS-001003')
  })
})
