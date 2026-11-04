import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import type { Role } from '../identity/types'
import type { LogTicketRequest, Ticket, TicketStatus } from '../tickets/types'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { TextField } from '../../components/Field'
import { TicketForm } from '../tickets/TicketForm'
import { TicketTable } from '../tickets/TicketTable'
import { StatusFilter } from '../tickets/StatusFilter'
import { SummaryStrip } from '../tickets/SummaryStrip'
import {
  CLIENT_EMAIL,
  createSampleTickets,
  dateFromNow,
  getAllowedTransitions,
  summarizeTickets,
} from './demoModel'

const ENGINEER_EMAIL = 'engineer@tss.local'

const DEMO_ACCOUNTS: { email: string; password: string; role: Role }[] = [
  { email: ENGINEER_EMAIL, password: 'Engineer123!', role: 'ENGINEER' },
  { email: CLIENT_EMAIL, password: 'Client123!', role: 'CLIENT' },
]

export const DemoPage = () => {
  const [tickets, setTickets] = useState<Ticket[]>(createSampleTickets)
  const [role, setRole] = useState<Role | null>(null)
  const [demoError, setDemoError] = useState<string | null>(null)
  const [filter, setFilter] = useState<TicketStatus | undefined>()
  const [nextDemoNumber, setNextDemoNumber] = useState(1)

  const isEngineer = role === 'ENGINEER'
  const currentEmail = isEngineer ? ENGINEER_EMAIL : CLIENT_EMAIL
  const currentName = isEngineer ? 'Demo Engineer' : 'Demo Client'
  const ticketsForCurrentRole = useMemo(
    () =>
      isEngineer
        ? tickets
        : tickets.filter((ticket) => ticket.requesterEmail === CLIENT_EMAIL),
    [isEngineer, tickets],
  )
  const visibleTickets = useMemo(
    () =>
      filter
        ? ticketsForCurrentRole.filter((ticket) => ticket.status === filter)
        : ticketsForCurrentRole,
    [filter, ticketsForCurrentRole],
  )
  const summary = useMemo(() => summarizeTickets(ticketsForCurrentRole), [ticketsForCurrentRole])

  const signInToDemo = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const email = String(form.get('email') ?? '').trim().toLowerCase()
    const password = String(form.get('password') ?? '')
    const account = DEMO_ACCOUNTS.find((candidate) => candidate.email === email && candidate.password === password)
    if (!account) {
      setDemoError('That demo email and password do not match. Use one of the sample accounts below.')
      return
    }
    setRole(account.role)
    setDemoError(null)
  }

  const signOutOfDemo = () => {
    setRole(null)
    setDemoError(null)
    setFilter(undefined)
    setTickets(createSampleTickets())
    setNextDemoNumber(1)
  }

  const createTicket = async (request: LogTicketRequest): Promise<Ticket> => {
    const now = new Date().toISOString()
    const ticket: Ticket = {
      id: `demo-new-${nextDemoNumber}`,
      reference: `DEMO-${String(nextDemoNumber).padStart(3, '0')}`,
      ...request,
      status: 'OPEN',
      requesterName: currentName,
      requesterEmail: currentEmail,
      respondBy: dateFromNow(request.priority === 'URGENT' ? 1 : request.priority === 'HIGH' ? 4 : request.priority === 'LOW' ? 24 : 8),
      createdAt: now,
      updatedAt: now,
      allowedTransitions: getAllowedTransitions('OPEN'),
    }
    setNextDemoNumber((number) => number + 1)
    return ticket
  }

  const addTicket = (ticket: Ticket) => setTickets((current) => [ticket, ...current])
  const changeStatus = (ticket: Ticket, status: TicketStatus) => {
    setTickets((current) => current.map((item) => item.id === ticket.id
      ? { ...item, status, updatedAt: new Date().toISOString(), allowedTransitions: getAllowedTransitions(status) }
      : item))
  }

  if (!role) {
    return (
      <div className="login-screen grid min-h-[100dvh] bg-white lg:grid-cols-[1.05fr_0.95fr]">
        <section className="login-hero-panel relative flex min-h-[420px] flex-col justify-start gap-6 overflow-hidden p-6 text-white sm:p-8 lg:min-h-0 lg:p-8 xl:p-9">
          <Link to="/login" aria-label="Tech Savvy Solutions home" className="relative flex w-fit items-center rounded-lg focus-visible:outline-2 focus-visible:outline-sky">
            <img src="/images/tech-savvy-logo-colour.webp" alt="Tech Savvy Solutions" className="h-14 w-auto object-contain sm:h-16" />
          </Link>
          <div className="relative flex flex-col gap-5">
            <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-sky">Interactive portfolio demo</p>
            <h1 className="max-w-xl font-display text-5xl leading-[1.04] sm:text-6xl xl:text-7xl">See support from both sides.</h1>
            <p className="max-w-lg text-lg leading-8 text-white/75">Sign in as a client to log a request, or as an engineer to work through the team queue.</p>
          </div>
          <p className="relative mt-auto pt-2 text-sm text-white/60">All demo tickets are fictional and stay in this browser.</p>
        </section>

        <main className="login-form-panel flex items-start justify-center px-5 py-6 sm:px-8 lg:items-center lg:justify-end lg:overflow-y-auto lg:py-4 lg:pr-10 xl:pr-8">
          <div className="flex w-full max-w-md flex-col gap-4 rounded-[2rem] border border-rule/70 bg-white/95 p-5 shadow-[0_24px_70px_rgba(11,38,51,0.15)] sm:p-6 lg:gap-3">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-river">Demo account</p>
              <h2 className="font-display text-4xl leading-tight">Welcome.</h2>
              <p className="mt-1 text-ink-muted">Choose either sample account to explore the portal.</p>
            </div>
            {demoError && <FeedbackMessage>{demoError}</FeedbackMessage>}
            <form onSubmit={signInToDemo} className="flex flex-col gap-3">
              <TextField label="Email" name="email" type="email" autoComplete="username" required />
              <TextField label="Password" name="password" type="password" autoComplete="current-password" required />
              <Button type="submit">Sign in to demo</Button>
            </form>
            <aside aria-label="Demo account credentials" className="rounded-2xl border border-sky/50 bg-[#eef9fe] p-4 shadow-[0_8px_24px_rgba(11,38,51,0.06)]">
              <p className="text-sm font-extrabold text-mill">Sample accounts</p>
              <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
                <div className="grid gap-1">
                  <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-river">Engineer</dt>
                  <dd className="break-all font-mono text-[13px] leading-5 text-mill">{ENGINEER_EMAIL}<br /><span className="font-bold">Engineer123!</span></dd>
                </div>
                <div className="grid gap-1 border-t border-sky/25 pt-2 sm:border-t-0 sm:border-l sm:pt-0 sm:pl-3">
                  <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-river">Client</dt>
                  <dd className="break-all font-mono text-[13px] leading-5 text-mill">{CLIENT_EMAIL}<br /><span className="font-bold">Client123!</span></dd>
                </div>
              </dl>
            </aside>
            <p className="text-xs leading-5 text-ink-muted">This is a portfolio demo login only. It uses fictional browser data and does not sign in to Azure or access the live API.</p>
            <Link to="/login" className="text-center text-sm font-bold text-river underline decoration-sky underline-offset-4">Full application: Microsoft sign-in</Link>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="dashboard-app min-h-screen">
      <div className="mx-auto max-w-[1500px] px-4 pt-4 sm:px-8">
        <aside className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-sky/40 bg-[#eef9fe] p-4">
          <div>
            <p className="text-sm font-extrabold text-mill">Signed in as demo {isEngineer ? 'Engineer' : 'Client'}</p>
            <p className="text-sm text-ink-muted">Fictional sample data. Changes stay in this browser and reset when you leave or refresh.</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button type="button" onClick={signOutOfDemo} className="rounded-full border border-rule bg-white px-4 py-2 text-sm font-bold text-mill hover:border-sky hover:bg-[#f0faff]">Sign out of demo</button>
            <Link to="/login" className="rounded-full border border-rule bg-white px-4 py-2 text-sm font-bold text-mill hover:border-sky hover:bg-[#f0faff]">
              Full app: Microsoft sign-in
            </Link>
          </div>
        </aside>
      </div>

      <main className="dashboard-main mx-auto grid max-w-[1500px] gap-8 px-4 py-5 sm:px-8 sm:py-7 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="dashboard-left flex min-w-0 flex-col gap-6">
          <section className="portal-hero-splash dashboard-hero relative min-h-64 overflow-hidden rounded-[2rem] px-6 py-8 text-white shadow-[0_24px_55px_rgba(11,38,51,0.2)] sm:px-9 sm:py-10">
            <div className="dashboard-hero-brand relative mb-3 flex flex-wrap items-center justify-between gap-4">
              <Link to="/login" aria-label="Tech Savvy Solutions home" className="flex items-center rounded-lg focus-visible:outline-2 focus-visible:outline-sky">
                <img src="/images/tech-savvy-logo-colour.webp" alt="Tech Savvy Solutions" className="h-10 w-auto object-contain sm:h-11" />
              </Link>
              <span className="rounded-full border border-white/20 bg-mill/40 px-4 py-2 text-sm font-bold text-white">{isEngineer ? 'Engineer view' : 'Client view'}</span>
            </div>
            <p className="relative mb-3 text-xs font-extrabold uppercase tracking-[0.22em] text-sky">Tech Savvy · Support portal</p>
            <h1 className="relative max-w-3xl font-display text-4xl leading-[1.04] sm:text-5xl lg:text-6xl">
              {isEngineer ? 'Every request,' : 'Your support,'}<br /><span className="italic text-sky">moving forward.</span>
            </h1>
            <p className="relative mt-4 max-w-2xl text-base leading-7 text-white/75 sm:text-lg">
              {isEngineer ? 'Keep track of the team queue, priorities and response times in one clear view.' : 'Log a request, check its progress, and let our local team take it from here.'}
            </p>
          </section>

          <section aria-label="Sample request list" className="dashboard-requests flex flex-col gap-4">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-river">{isEngineer ? 'Team queue preview' : 'Client preview'}</p>
              <h2 className="mt-1 text-2xl font-extrabold sm:text-3xl">{isEngineer ? 'All sample requests' : 'Sample job requests'}</h2>
            </div>
            <SummaryStrip summary={summary} />
            <StatusFilter value={filter} onChange={setFilter} />
            <div className="ticket-results">
              <TicketTable tickets={visibleTickets} canChangeStatus={isEngineer} onChangeStatus={changeStatus} />
            </div>
          </section>
        </div>

        <aside className="dashboard-form-panel h-fit rounded-3xl border border-rule/80 bg-white p-5 shadow-[0_16px_45px_rgba(11,38,51,0.07)] sm:p-6 xl:sticky xl:top-6">
          <TicketForm onSubmit={createTicket} onLogged={addTicket} />
        </aside>
      </main>
    </div>
  )
}
