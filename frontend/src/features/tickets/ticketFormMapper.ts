import type { LogTicketRequest, TicketCategory, TicketPriority } from '../../api/types'

const text = (data: FormData, key: string): string => String(data.get(key) ?? '').trim()

export const toLogTicketRequest = (data: FormData): LogTicketRequest => ({
  title: text(data, 'title'),
  description: text(data, 'description'),
  category: text(data, 'category') as TicketCategory,
  priority: text(data, 'priority') as TicketPriority,
  companyName: text(data, 'companyName'),
  requiredBy: text(data, 'requiredBy') || null,
})

export const validateRequest = (request: LogTicketRequest): Record<string, string> => {
  const errors: Record<string, string> = {}
  if (!request.title) errors.title = 'Give the request a short title.'
  if (!request.companyName) errors.companyName = 'Tell us which company this is for.'
  if (request.description.length < 10) errors.description = 'Add a few details so the engineer knows what to do.'
  return errors
}
