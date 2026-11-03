import { TICKET_STATUSES, type TicketStatus } from '../../api/types'
import { STATUS_LABELS } from './labels'

interface StatusFilterProps {
  value: TicketStatus | undefined
  onChange: (status: TicketStatus | undefined) => void
}

export function StatusFilter({ value, onChange }: StatusFilterProps) {
  return (
    <div role="group" aria-label="Filter by status" className="flex flex-wrap gap-2">
      {[undefined, ...TICKET_STATUSES].map((status) => (
        <button
          key={status ?? 'ALL'}
          type="button"
          aria-pressed={value === status}
          onClick={() => onChange(status)}
          className="rounded-full border border-rule bg-white px-4 py-2 text-sm font-bold shadow-sm hover:border-sky hover:bg-[#f0faff] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky aria-pressed:border-sky aria-pressed:bg-sky aria-pressed:text-mill"
        >
          {status ? STATUS_LABELS[status] : 'Everything'}
        </button>
      ))}
    </div>
  )
}
