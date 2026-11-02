import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import type { LogTicketRequest, Role, Ticket, TicketStatus, TicketSummary } from '../../api/types'
import { TICKET_STATUSES } from '../../api/types'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { TextField } from '../../components/Field'
import { TicketForm } from '../tickets/TicketForm'
import { TicketTable } from '../tickets/TicketTable'
import { STATUS_LABELS } from '../tickets/labels'
import { SummaryStrip } from '../tickets/SummaryStrip'

const CLIENT_EMAIL = 'client@acme.local'
const ENGINEER_EMAIL = 'engineer@tss.local'
const HOUR = 60 * 60 * 1000

const DEMO_ACCOUNTS: { email: string; password: string; role: Role }[] = [
  { email: ENGINEER_EMAIL, password: 'Engineer123!', role: 'ENGINEER' },
  { email: CLIENT_EMAIL, password: 'Client123!', role: 'CLIENT' },
]

const dateFromNow = (hours: number): string => new Date(Date.now() + hours * HOUR).toISOString()
const dateFromDays = (days: number): string => {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

const nextStatuses: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['OPEN', 'WAITING_ON_CLIENT', 'RESOLVED'],
  WAITING_ON_CLIENT: ['IN_PROGRESS', 'CLOSED'],
  RESOLVED: ['IN_PROGRESS', 'CLOSED'],
  CLOSED: [],
}

const createSampleTickets = (): Ticket[] => {
  const makeTicket = (
    id: string,
    reference: string,
    title: string,
    description: string,
    category: Ticket['category'],
    priority: Ticket['priority'],
    status: TicketStatus,
    companyName: string,
    requesterName: string,
    requesterEmail: string,
    ageHours: number,
    replyHours: number,
    requiredBy: string | null = null,
  ): Ticket => ({
    id,
    reference,
    title,
    description,
    category,
    priority,
    status,
    companyName,
    requesterName,
    requesterEmail,
    requiredBy,
    respondBy: dateFromNow(replyHours),
    createdAt: dateFromNow(-ageHours),
    updatedAt: dateFromNow(-ageHours),
    allowedTransitions: nextStatuses[status],
  })

  return [
    makeTicket(
      'demo-1004', 'TSS-001004', 'New starter laptop for Alex',
      'Alex starts Monday. Needs a laptop with Microsoft 365 and the accounts package.',
      'NEW_STARTER', 'HIGH', 'OPEN', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 1, 3, dateFromDays(5),
    ),
    makeTicket(
      'demo-1003', 'TSS-001003', 'Front office printer offline',
      'The printer next to reception has not worked since this morning.',
      'HARDWARE', 'URGENT', 'OPEN', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 2, -0.5,
    ),
    makeTicket(
      'demo-1002', 'TSS-001002', 'Shared mailbox for orders',
      'Please create orders@ and give three people access.',
      'MICROSOFT_365', 'NORMAL', 'IN_PROGRESS', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 6, 18,
    ),
    makeTicket(
      'demo-1001', 'TSS-001001', 'Office move: Bury to Ramsbottom',
      'We move in six weeks. Need a plan for the network and phones.',
      'OFFICE_MOVE', 'LOW', 'WAITING_ON_CLIENT', 'Lancashire Joinery', 'Demo Client', 'owner@demo.example', 20, 28, dateFromDays(42),
    ),
    makeTicket(
      'demo-1000', 'TSS-001000', 'Review mailbox security settings',
      'Check the shared mailbox permissions after the recent staff changes.',
      'SECURITY', 'NORMAL', 'RESOLVED', 'Acme Bury Ltd', 'Demo Client', CLIENT_EMAIL, 48, 72,
    ),
  ]
}

const summarize = (tickets: Ticket[]): TicketSummary => {
  const byStatus = Object.fromEntries(TICKET_STATUSES.map((status) => [
    status,
    tickets.filter((ticket) => ticket.status === status).length,
  ])) as Record<TicketStatus, number>
  const open = tickets.filter((ticket) => ticket.status === 'OPEN')
  const now = Date.now()
  const nextResponseDue = [...open].sort((left, right) =>
    new Date(left.respondBy).getTime() - new Date(right.respondBy).getTime(),
  )[0] ?? null

  return {
    byStatus,
    awaitingResponseOverdue: open.filter((ticket) => new Date(ticket.respondBy).getTime() < now).length,
    nextResponseDue,
  }
}

export const DemoPage = () => {
  const [tickets, setTickets] = useState<Ticket[]>(createSampleTickets)
  const [role, setRole] = useState<Role | null>(null)
  const [demoError, setDemoError] = useState<string | null>(null)
  const [filter, setFilter] = useState<TicketStatus | undefined>()
  const [nextDemoNumber, setNextDemoNumber] = useState(1)

  const isEngineer = role === 'ENGINEER'
  const currentEmail = isEngineer ? ENGINEER_EMAIL : CLIENT_EMAIL
  const currentName = isEngineer ? 'Demo Engineer' : 'Demo Client'
  const visibleTickets = useMemo(() => {
    const roleTickets = isEngineer
      ? tickets
      : tickets.filter((ticket) => ticket.requesterEmail === CLIENT_EMAIL)
    return filter ? roleTickets.filter((ticket) => ticket.status === filter) : roleTickets
  }, [filter, isEngineer, tickets])
  const roleTickets = useMemo(() => isEngineer
    ? tickets
    : tickets.filter((ticket) => ticket.requesterEmail === CLIENT_EMAIL), [isEngineer, tickets])
  const summary = useMemo(() => summarize(roleTickets), [roleTickets])

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
      allowedTransitions: nextStatuses.OPEN,
    }
    setNextDemoNumber((number) => number + 1)
    return ticket
  }

  const addTicket = (ticket: Ticket) => setTickets((current) => [ticket, ...current])
  const changeStatus = (ticket: Ticket, status: TicketStatus) => {
    setTickets((current) => current.map((item) => item.id === ticket.id
      ? { ...item, status, updatedAt: new Date().toISOString(), allowedTransitions: nextStatuses[status] }
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
            <div role="group" aria-label="Filter by status" className="flex flex-wrap gap-2">
              {[undefined, ...TICKET_STATUSES].map((status) => (
                <button
                  key={status ?? 'ALL'}
                  type="button"
                  aria-pressed={filter === status}
                  onClick={() => setFilter(status)}
                  className="rounded-full border border-rule bg-white px-4 py-2 text-sm font-bold shadow-sm hover:border-sky hover:bg-[#f0faff] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky aria-pressed:border-sky aria-pressed:bg-sky aria-pressed:text-mill"
                >
                  {status ? STATUS_LABELS[status] : 'Everything'}
                </button>
              ))}
            </div>
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
