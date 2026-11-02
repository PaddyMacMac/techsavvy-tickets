import { Link, Navigate } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { DevLoginForm } from './DevLoginForm'

const PROMISES = [
  'Established, locally owned and based in Bury',
  'Big-firm capability with small-business care',
  'Plain-English advice, no confusing tech-speak',
  'Local engineers who genuinely know your setup',
]

export const LoginPage = () => {
  const { status, kind, error, signIn } = useAuth()
  if (status === 'authenticated') return <Navigate to="/" replace />

  return (
    <div className="login-screen grid min-h-[100dvh] bg-white lg:grid-cols-[1.05fr_0.95fr]">
      <section className="login-hero-panel relative flex min-h-[420px] flex-col justify-start gap-6 overflow-hidden p-6 text-white sm:p-8 lg:min-h-0 lg:p-8 xl:p-9">
        <a href="/" aria-label="Tech Savvy Solutions home" className="relative flex w-fit items-center rounded-lg focus-visible:outline-2 focus-visible:outline-sky">
          <img src="/images/tech-savvy-logo-colour.webp" alt="Tech Savvy Solutions" className="h-14 w-auto object-contain sm:h-16" />
        </a>
        <div className="relative flex flex-col gap-5">
          <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-sky">Local people. Reliable support.</p>
          <h1 className="max-w-xl font-display text-5xl leading-[1.04] sm:text-6xl xl:text-7xl">
            Good IT keeps your business <span className="italic text-sky">moving.</span>
          </h1>
          <p className="max-w-lg text-lg leading-8 text-white/75">Your local team for practical support, clear answers and technology that simply works.</p>
          <ul className="grid max-w-lg gap-3 text-base text-white/85 sm:grid-cols-2">
            {PROMISES.map((promise) => (
              <li key={promise} className="flex items-start gap-2"><span className="mt-0.5 text-sky">✓</span><span>{promise}</span></li>
            ))}
          </ul>
        </div>
        <p className="relative mt-auto pt-2 text-sm text-white/60">Serving Greater Manchester and Lancashire.</p>
      </section>

      <main className="login-form-panel flex items-start justify-center px-5 pt-4 pb-6 sm:px-8 lg:items-center lg:justify-end lg:overflow-y-auto lg:py-4 lg:pr-10 xl:pr-8">
        <div className="flex w-full max-w-md flex-col gap-4 rounded-[2rem] border border-rule/70 bg-white/95 p-5 shadow-[0_24px_70px_rgba(11,38,51,0.15)] backdrop-blur-md sm:p-6 lg:gap-3">
          <div><p className="text-xs font-extrabold uppercase tracking-[0.18em] text-river">Support portal</p><h2 className="font-display text-4xl leading-tight">Welcome back.</h2><p className="mt-1 text-ink-muted">Sign in to log a job or check on a request.</p></div>
          {error && status === 'anonymous' && (
            <FeedbackMessage>
              {error}
            </FeedbackMessage>
          )}
          {kind === 'dev' ? (
            <>
              <DevLoginForm onSignIn={signIn} />
              <Link to="/demo" className="rounded-full border border-rule px-5 py-3 text-center font-bold text-mill hover:border-sky hover:bg-[#f0faff]">
                Try the demo account login
              </Link>
            </>
          ) : (
            <>
              <Button onClick={() => void signIn()}>Sign in with Microsoft</Button>
              <p className="text-sm text-ink-muted">Use the work account Tech Savvy set up for you.</p>
              <Link to="/demo" className="rounded-full border border-rule px-5 py-3 text-center font-bold text-mill hover:border-sky hover:bg-[#f0faff]">
                Try the demo account login
              </Link>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
