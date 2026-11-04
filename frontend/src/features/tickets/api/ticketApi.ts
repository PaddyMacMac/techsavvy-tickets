import type { HttpClient } from '../../../api/httpClient'
import type { LogTicketRequest, Ticket, TicketPage, TicketStatus, TicketSummary } from '../types'

export interface ListParams {
  status?: TicketStatus
  page?: number
  size?: number
}

export const createTicketApi = (http: HttpClient) => ({
  list: ({ status, page = 0, size = 20 }: ListParams = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) query.set('status', status)
    return http.get<TicketPage>(`/api/tickets?${query.toString()}`)
  },

  summary: () => http.get<TicketSummary>('/api/tickets/summary'),

  log: (request: LogTicketRequest) => http.post<Ticket>('/api/tickets', request),

  changeStatus: (id: string, status: TicketStatus) => http.patch<Ticket>(`/api/tickets/${id}/status`, { status }),
})

export type TicketApi = ReturnType<typeof createTicketApi>
