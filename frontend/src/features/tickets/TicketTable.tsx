import type { Ticket, TicketStatus } from './types'
import { StatusBadge } from './StatusBadge'
import { describeResponseDue } from './dueLabel'
import { CATEGORY_LABELS, PRIORITY_LABELS, STATUS_LABELS } from './labels'

interface TicketTableProps {
  tickets: Ticket[]
  canChangeStatus: boolean
  onChangeStatus: (ticket: Ticket, status: TicketStatus) => void
}

const dateFormat = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

export const TicketTable = ({ tickets, canChangeStatus, onChangeStatus }: TicketTableProps) => {
  if (tickets.length === 0) {
    return (
      <p className="rounded-3xl border border-dashed border-rule bg-white px-6 py-14 text-center text-ink-muted shadow-sm">
        <span className="mb-2 block font-display text-2xl text-mill">Nothing in the queue yet.</span>
        Use the form to log your first job request.
      </p>
    )
  }

  return (
    <div className="overflow-x-auto rounded-3xl border border-rule/80 bg-white shadow-[0_12px_36px_rgba(11,38,51,0.05)]">
      <table className="w-full min-w-176 border-collapse text-left">
        <caption className="sr-only">Job requests</caption>
        <thead className="border-b border-rule bg-[#f7fafb] text-xs uppercase tracking-[0.12em] text-ink-muted">
          <tr>
            <th scope="col" className="px-5 py-4 font-extrabold">Request</th>
            <th scope="col" className="px-5 py-4 font-extrabold">Priority</th>
            <th scope="col" className="px-5 py-4 font-extrabold">Status</th>
            <th scope="col" className="px-5 py-4 font-extrabold">Logged</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((ticket) => {
            const due = ticket.status === 'OPEN' ? describeResponseDue(ticket.respondBy) : null
            return (
              <tr key={ticket.id} className="border-b border-rule/70 align-top last:border-b-0">
                <td className="px-5 py-4">
                  <p className="font-bold text-mill">{ticket.title}</p>
                  <p className="mt-1 text-sm leading-5 text-ink-muted">
                    <span className="font-bold tabular-nums text-river">{ticket.reference}</span> <span aria-hidden="true">·</span> {ticket.companyName} <span aria-hidden="true">·</span> {CATEGORY_LABELS[ticket.category]}
                  </p>
                </td>
                <td className="px-5 py-4 text-sm font-semibold">{PRIORITY_LABELS[ticket.priority]}</td>
                <td className="px-5 py-4">
                  <StatusBadge status={ticket.status} />
                  {due && <p className={`mt-1 text-sm ${due.overdue ? 'font-bold text-brick' : 'text-ink-muted'}`}>{due.text}</p>}
                  {canChangeStatus && ticket.allowedTransitions.length > 0 && (
                    <select
                      aria-label={`Move ${ticket.reference} to`}
                      className="mt-2 block rounded-full border border-rule bg-white px-3 py-1.5 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-sky"
                      value=""
                      onChange={(event) => onChangeStatus(ticket, event.target.value as TicketStatus)}
                    >
                      <option value="" disabled>Move to…</option>
                      {ticket.allowedTransitions.map((next) => (
                        <option key={next} value={next}>{STATUS_LABELS[next]}</option>
                      ))}
                    </select>
                  )}
                </td>
                <td className="whitespace-nowrap px-5 py-4 text-sm text-ink-muted">{dateFormat.format(new Date(ticket.createdAt))}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
