import type { TicketCategory, TicketPriority, TicketStatus } from './types'

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Waiting for us',
  IN_PROGRESS: 'In progress',
  WAITING_ON_CLIENT: 'Waiting for you',
  RESOLVED: 'Done - please check',
  CLOSED: 'Closed',
}

export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Low - whenever suits',
  NORMAL: 'Normal',
  HIGH: 'High - it is slowing someone down',
  URGENT: 'Urgent - work has stopped',
}

export const CATEGORY_LABELS: Record<TicketCategory, string> = {
  NEW_STARTER: 'New starter set-up',
  HARDWARE: 'Computer or hardware',
  SOFTWARE: 'Software install or fix',
  MICROSOFT_365: 'Microsoft 365',
  SECURITY: 'Security',
  NETWORK: 'Wi-Fi or network',
  OFFICE_MOVE: 'Office move',
  OTHER: 'Something else',
}
