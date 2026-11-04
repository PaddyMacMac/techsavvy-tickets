import { useMemo } from 'react'
import { useAuth } from '../../identity/auth/useAuth'
import { createHttpClient } from '../../../api/httpClient'
import { createTicketApi, type TicketApi } from './ticketApi'

export const useTicketApi = (): TicketApi => {
  const { getAccessToken } = useAuth()
  return useMemo(() => createTicketApi(createHttpClient(getAccessToken)), [getAccessToken])
}
