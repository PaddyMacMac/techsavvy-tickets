import { TICKET_STATUSES, type Ticket, type TicketStatus, type TicketSummary } from '../../api/types'

export const CLIENT_EMAIL = 'client@acme.local'

const HOUR = 60 * 60 * 1000

const ALLOWED_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['OPEN', 'WAITING_ON_CLIENT', 'RESOLVED'],
  WAITING_ON_CLIENT: ['IN_PROGRESS', 'CLOSED'],
  RESOLVED: ['IN_PROGRESS', 'CLOSED'],
  CLOSED: [],
}

export const dateFromNow = (hours: number): string => new Date(Date.now() + hours * HOUR).toISOString()

const dateFromDays = (days: number): string => {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

export const getAllowedTransitions = (status: TicketStatus): TicketStatus[] =>
  ALLOWED_TRANSITIONS[status]

export const createSampleTickets = (): Ticket[] => {
  const makeTicket = (
    id: string,
    reference: string,
    title: string,
    description: string,
    category: Ticket['category'],
    priority: Ticket['priority'],
    status: TicketStatus,
    companyName: string,
    requesterName: string,
    requesterEmail: string,
    ageHours: number,
    replyHours: number,
    requiredBy: string | null = null,
  ): Ticket => ({
    id,
    reference,
    title,
    description,
    category,
    priority,
    status,
    companyName,
    requesterName,
    requesterEmail,
    requiredBy,
    respondBy: dateFromNow(replyHours),
    createdAt: dateFromNow(-ageHours),
    updatedAt: dateFromNow(-ageHours),
    allowedTransitions: getAllowedTransitions(status),
  })

  return [
    makeTicket(
      'demo-1004', 'TSS-001004', 'New starter laptop for Alex',
      'Alex starts Monday. Needs a laptop with Microsoft 365 and the accounts package.',
      'NEW_STARTER', 'HIGH', 'OPEN', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 1, 3, dateFromDays(5),
    ),
    makeTicket(
      'demo-1003', 'TSS-001003', 'Front office printer offline',
      'The printer next to reception has not worked since this morning.',
      'HARDWARE', 'URGENT', 'OPEN', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 2, -0.5,
    ),
    makeTicket(
      'demo-1002', 'TSS-001002', 'Shared mailbox for orders',
      'Please create orders@ and give three people access.',
      'MICROSOFT_365', 'NORMAL', 'IN_PROGRESS', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 6, 18,
    ),
    makeTicket(
      'demo-1001', 'TSS-001001', 'Office move: Bury to Ramsbottom',
      'We move in six weeks. Need a plan for the network and phones.',
      'OFFICE_MOVE', 'LOW', 'WAITING_ON_CLIENT', 'Lancashire Joinery', 'Demo Client', 'owner@demo.example', 20, 28, dateFromDays(42),
    ),
    makeTicket(
      'demo-1000', 'TSS-001000', 'Review mailbox security settings',
      'Check the shared mailbox permissions after the recent staff changes.',
      'SECURITY', 'NORMAL', 'RESOLVED', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 48, 72,
    ),
  ]
}

export const summarizeTickets = (tickets: Ticket[]): TicketSummary => {
  const byStatus = Object.fromEntries(TICKET_STATUSES.map((status) => [
    status,
    tickets.filter((ticket) => ticket.status === status).length,
  ])) as Record<TicketStatus, number>
  const openTickets = tickets.filter((ticket) => ticket.status === 'OPEN')
  const now = Date.now()
  const nextResponseDue = [...openTickets].sort((left, right) =>
    new Date(left.respondBy).getTime() - new Date(right.respondBy).getTime(),
  )[0] ?? null

  return {
    byStatus,
    awaitingResponseOverdue: openTickets.filter((ticket) => new Date(ticket.respondBy).getTime() < now).length,
    nextResponseDue,
  }
}
