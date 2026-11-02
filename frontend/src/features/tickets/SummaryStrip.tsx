import { TICKET_STATUSES, type TicketSummary } from '../../api/types'
import { describeResponseDue } from './dueLabel'
import { STATUS_LABELS } from './labels'

export const SummaryStrip = ({ summary }: { summary: TicketSummary }) => {
  const next = summary.nextResponseDue
  return (
    <section aria-label="Summary" className="relative flex flex-col gap-4 overflow-hidden rounded-3xl bg-mill p-6 text-white shadow-[0_18px_45px_rgba(11,38,51,0.14)] sm:p-7">
      <div aria-hidden="true" className="pointer-events-none absolute -right-12 -top-20 h-64 w-64 rounded-full border-[32px] border-sky/10" />
      <p className="relative text-lg font-bold sm:text-xl">
        {summary.awaitingResponseOverdue === 0
          ? 'Nothing is waiting past its reply time.'
          : `${summary.awaitingResponseOverdue} ${summary.awaitingResponseOverdue === 1 ? 'request is' : 'requests are'} past the reply time.`}
      </p>
      {next && (
        <p className="relative text-base text-white/75">
          Next reply due: {next.reference}, {describeResponseDue(next.respondBy).text.replace('Reply ', '').toLowerCase()}.
        </p>
      )}
      <dl className="relative flex flex-wrap gap-x-8 gap-y-3 border-t border-white/15 pt-4">
        {TICKET_STATUSES.map((status) => (
          <div key={status} className="flex items-baseline gap-2">
            <dt className="text-sm text-white/70">{STATUS_LABELS[status]}</dt>
            <dd className="text-2xl font-extrabold tabular-nums text-sky">{summary.byStatus[status]}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
