import type { TicketStatus } from '../api/types'
import { STATUS_LABELS } from '../features/tickets/labels'

const tone: Record<TicketStatus, string> = {
  OPEN: 'border-sky/50 bg-sky/10 text-mill',
  IN_PROGRESS: 'border-river bg-river/10 text-river',
  WAITING_ON_CLIENT: 'border-amber-ink bg-amber-ink/10 text-amber-ink',
  RESOLVED: 'border-moss bg-moss/10 text-moss',
  CLOSED: 'border-rule bg-chalk text-ink-muted',
}

/** Colour is never the only signal: every status also has its own words. */
export const StatusBadge = ({ status }: { status: TicketStatus }) => (
  <span className={`inline-block whitespace-nowrap rounded-full border px-3 py-1 text-xs font-extrabold tracking-wide ${tone[status]}`}>
    {STATUS_LABELS[status]}
  </span>
)
