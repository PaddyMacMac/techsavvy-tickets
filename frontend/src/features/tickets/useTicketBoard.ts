import { useCallback, useEffect, useState } from 'react'
import type { TicketApi } from '../../api/ticketApi'
import type { TicketPage, TicketStatus, TicketSummary } from '../../api/types'

export type BoardState =
  | { phase: 'loading' }
  | { phase: 'error'; message: string }
  | { phase: 'ready'; page: TicketPage; summary: TicketSummary }

const messageOf = (error: unknown): string => (error instanceof Error ? error.message : 'Could not load requests.')

export const useTicketBoard = (api: TicketApi, filter: TicketStatus | undefined, pageNumber: number) => {
  const [state, setState] = useState<BoardState>({ phase: 'loading' })
  const [refreshCount, setRefreshCount] = useState(0)

  useEffect(() => {
    let cancelled = false
    Promise.all([api.list({ status: filter, page: pageNumber }), api.summary()])
      .then(([page, summary]) => {
        if (!cancelled) setState({ phase: 'ready', page, summary })
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ phase: 'error', message: messageOf(error) })
      })
    return () => {
      cancelled = true
    }
  }, [api, filter, pageNumber, refreshCount])

  const reload = useCallback(() => setRefreshCount((count) => count + 1), [])
  return { state, reload }
}
