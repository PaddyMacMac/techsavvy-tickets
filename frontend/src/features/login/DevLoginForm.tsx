import { useActionState } from 'react'
import type { Credentials } from '../../auth/authStrategy'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { TextField } from '../../components/Field'

interface DevLoginFormProps {
  onSignIn: (credentials: Credentials) => Promise<void>
}

type FormState = { error: string | null; email: string }

/** Local development sign-in only. In Azure the Microsoft button is shown instead. */
export const DevLoginForm = ({ onSignIn }: DevLoginFormProps) => {
  const [state, formAction, pending] = useActionState(
    async (_previous: FormState, data: FormData): Promise<FormState> => {
      const email = String(data.get('email') ?? '').trim()
      const password = String(data.get('password') ?? '')
      if (!email || !password) return { error: 'Enter your email and password.', email }
      try {
        await onSignIn({ email, password })
        return { error: null, email }
      } catch (error) {
        return { error: error instanceof Error ? error.message : 'Sign-in failed.', email }
      }
    },
    { error: null, email: '' },
  )

  return (
    <form action={formAction} noValidate className="flex flex-col gap-3">
      {state.error && (
        <FeedbackMessage>
          {state.error}
        </FeedbackMessage>
      )}
      <TextField label="Email" name="email" type="email" autoComplete="username" defaultValue={state.email} />
      <TextField label="Password" name="password" type="password" autoComplete="current-password" />
      <Button type="submit" disabled={pending}>
        {pending ? 'Signing in…' : 'Sign in'}
      </Button>
      <aside aria-label="Local demo accounts" className="rounded-2xl border border-sky/50 bg-[#eef9fe] p-4 shadow-[0_8px_24px_rgba(11,38,51,0.06)]">
        <p className="flex items-center gap-2 text-sm font-extrabold text-mill"><span aria-hidden="true" className="grid h-6 w-6 place-items-center rounded-full bg-sky text-xs">i</span>Local demo accounts</p>
        <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
          <div className="grid gap-1">
            <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-river">Engineer</dt>
            <dd className="break-all font-mono text-[13px] leading-5 text-mill">engineer@tss.local<br /><span className="font-bold">Engineer123!</span></dd>
          </div>
          <div className="grid gap-1 border-t border-sky/25 pt-2 sm:border-t-0 sm:border-l sm:pt-0 sm:pl-3">
            <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-river">Client</dt>
            <dd className="break-all font-mono text-[13px] leading-5 text-mill">client@acme.local<br /><span className="font-bold">Client123!</span></dd>
          </div>
        </dl>
      </aside>
    </form>
  )
}
