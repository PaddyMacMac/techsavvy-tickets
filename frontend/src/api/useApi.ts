import { useMemo } from 'react'
import { useAuth } from '../auth/useAuth'
import { createHttpClient } from './httpClient'
import { createTicketApi, type TicketApi } from './ticketApi'

export const useApi = (): TicketApi => {
  const { getAccessToken } = useAuth()
  return useMemo(() => createTicketApi(createHttpClient(getAccessToken)), [getAccessToken])
}
