import { useCallback, useState } from 'react'
import { useApi } from '../../api/useApi'
import { TICKET_STATUSES, type Ticket, type TicketStatus } from '../../api/types'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { STATUS_LABELS } from './labels'
import { SummaryStrip } from './SummaryStrip'
import { TicketForm } from './TicketForm'
import { TicketTable } from './TicketTable'
import { useTicketBoard } from './useTicketBoard'

export const TicketsPage = () => {
  const { user, signOut } = useAuth()
  const api = useApi()
  const [filter, setFilter] = useState<TicketStatus | undefined>(undefined)
  const [pageNumber, setPageNumber] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const { state, reload } = useTicketBoard(api, filter, pageNumber)
  const isEngineer = user?.roles.includes('ENGINEER') ?? false

  const chooseFilter = (next: TicketStatus | undefined) => {
    setFilter(next)
    setPageNumber(0)
  }

  const changeStatus = useCallback(
    async (ticket: Ticket, status: TicketStatus) => {
      setActionError(null)
      try {
        await api.changeStatus(ticket.id, status)
        reload()
      } catch (error) {
        setActionError(error instanceof Error ? error.message : 'Could not update that request.')
      }
    },
    [api, reload],
  )

  return (
    <div className="dashboard-app min-h-screen">
      <main className="dashboard-main mx-auto grid max-w-[1500px] gap-8 px-4 py-7 sm:px-8 sm:py-10 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="dashboard-left flex min-w-0 flex-col gap-6">
          <section className="portal-hero-splash dashboard-hero relative min-h-64 overflow-hidden rounded-[2rem] px-6 py-8 text-white shadow-[0_24px_55px_rgba(11,38,51,0.2)] sm:px-9 sm:py-10">
            <div className="dashboard-hero-brand relative mb-3 flex items-center justify-between gap-3">
              <a href="/" aria-label="Tech Savvy Solutions home" className="flex items-center rounded-lg focus-visible:outline-2 focus-visible:outline-sky">
                <img src="/images/tech-savvy-logo-colour.webp" alt="Tech Savvy Solutions" className="h-10 w-auto object-contain sm:h-11" />
              </a>
              <div className="flex items-center gap-2">
                <p className="hidden text-right text-sm text-white/80 sm:block">{user?.name}{isEngineer ? ' · Engineer' : ''}</p>
                <Button variant="secondary" className="px-4 py-2 text-sm" onClick={() => void signOut()}>Sign out</Button>
              </div>
            </div>
            <p className="relative mb-3 text-xs font-extrabold uppercase tracking-[0.22em] text-sky">Tech Savvy · Support portal</p>
            <h1 className="relative max-w-3xl font-display text-4xl leading-[1.04] sm:text-5xl lg:text-6xl">{isEngineer ? 'Every request,' : 'Your support,'}<br /><span className="italic text-sky">moving forward.</span></h1>
            <p className="relative mt-4 max-w-2xl text-base leading-7 text-white/75 sm:text-lg">{isEngineer ? 'Keep track of the team queue, priorities and response times in one clear view.' : 'Log a request, check its progress, and let our local team take it from here.'}</p>
          </section>

          <section aria-label="Request list" className="dashboard-requests flex flex-col gap-4">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div><p className="text-xs font-extrabold uppercase tracking-[0.18em] text-river">{isEngineer ? 'Team queue' : 'Your account'}</p><h2 className="mt-1 text-2xl font-extrabold sm:text-3xl">{isEngineer ? 'All job requests' : 'Your job requests'}</h2></div>
            </div>
            {state.phase === 'ready' && <SummaryStrip summary={state.summary} />}

          <div role="group" aria-label="Filter by status" className="flex flex-wrap gap-2">
            {[undefined, ...TICKET_STATUSES].map((status) => (
              <button
                key={status ?? 'ALL'}
                type="button"
                aria-pressed={filter === status}
                onClick={() => chooseFilter(status)}
                className="rounded-full border border-rule bg-white px-4 py-2 text-sm font-bold shadow-sm hover:border-sky hover:bg-[#f0faff] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky aria-pressed:border-sky aria-pressed:bg-sky aria-pressed:text-mill"
              >
                {status ? STATUS_LABELS[status] : 'Everything'}
              </button>
            ))}
          </div>

          {actionError && (
            <FeedbackMessage>
              {actionError}
            </FeedbackMessage>
          )}
          {state.phase === 'loading' && <p role="status" className="text-ink-muted">Loading requests…</p>}
          {state.phase === 'error' && (
            <FeedbackMessage>
              {state.message}
            </FeedbackMessage>
          )}
          {state.phase === 'ready' && (
            <>
              <div className="ticket-results"><TicketTable tickets={state.page.items} canChangeStatus={isEngineer} onChangeStatus={(t, s) => void changeStatus(t, s)} /></div>
              {state.page.totalPages > 1 && (
                <nav aria-label="Pages" className="flex items-center gap-3">
                  <Button variant="secondary" disabled={pageNumber === 0} onClick={() => setPageNumber((p) => p - 1)}>Newer</Button>
                  <p className="text-sm text-ink-muted">Page {state.page.page + 1} of {state.page.totalPages}</p>
                  <Button variant="secondary" disabled={pageNumber + 1 >= state.page.totalPages} onClick={() => setPageNumber((p) => p + 1)}>Older</Button>
                </nav>
              )}
            </>
          )}
          </section>
        </div>

        <aside className="dashboard-form-panel h-fit rounded-3xl border border-rule/80 bg-white p-5 shadow-[0_16px_45px_rgba(11,38,51,0.07)] sm:p-6 xl:sticky xl:top-6">
          <TicketForm onSubmit={api.log} onLogged={reload} />
        </aside>
      </main>
    </div>
  )
}
