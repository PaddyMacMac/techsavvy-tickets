export const TICKET_STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_ON_CLIENT', 'RESOLVED', 'CLOSED'] as const
export const TICKET_PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT'] as const
export const TICKET_CATEGORIES = [
  'NEW_STARTER',
  'HARDWARE',
  'SOFTWARE',
  'MICROSOFT_365',
  'SECURITY',
  'NETWORK',
  'OFFICE_MOVE',
  'OTHER',
] as const

export type TicketStatus = (typeof TICKET_STATUSES)[number]
export type TicketPriority = (typeof TICKET_PRIORITIES)[number]
export type TicketCategory = (typeof TICKET_CATEGORIES)[number]
export type Role = 'ENGINEER' | 'CLIENT'

export interface Me {
  subject: string
  name: string
  email: string
  roles: Role[]
}

export interface Ticket {
  id: string
  reference: string
  title: string
  description: string
  category: TicketCategory
  priority: TicketPriority
  status: TicketStatus
  companyName: string
  requesterName: string
  requesterEmail: string
  requiredBy: string | null
  respondBy: string
  createdAt: string
  updatedAt: string
  allowedTransitions: TicketStatus[]
}

export interface TicketPage {
  items: Ticket[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface TicketSummary {
  byStatus: Record<TicketStatus, number>
  awaitingResponseOverdue: number
  nextResponseDue: Ticket | null
}

export interface LogTicketRequest {
  title: string
  description: string
  category: TicketCategory
  priority: TicketPriority
  companyName: string
  requiredBy: string | null
}

export interface ProblemDetail {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}
