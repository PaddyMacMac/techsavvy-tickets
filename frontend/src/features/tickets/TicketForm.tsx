import { useActionState } from 'react'
import { ApiError } from '../../api/httpClient'
import { TICKET_CATEGORIES, TICKET_PRIORITIES, type LogTicketRequest, type Ticket } from '../../api/types'
import { Button } from '../../components/Button'
import { FeedbackMessage } from '../../components/FeedbackMessage'
import { SelectField, TextAreaField, TextField } from '../../components/Field'
import { CATEGORY_LABELS, PRIORITY_LABELS } from './labels'
import { toLogTicketRequest, validateRequest } from './ticketFormMapper'

type FormState =
  | { phase: 'idle' }
  | { phase: 'invalid'; errors: Record<string, string>; values: LogTicketRequest; message?: string }
  | { phase: 'logged'; reference: string }

interface TicketFormProps {
  onSubmit: (request: LogTicketRequest) => Promise<Ticket>
  onLogged?: (ticket: Ticket) => void
}

const failureMessage = (error: unknown): string =>
  error instanceof ApiError ? error.message : 'We could not log that request. Please try again.'

export const TicketForm = ({ onSubmit, onLogged }: TicketFormProps) => {
  const [state, formAction, pending] = useActionState(
    async (_previous: FormState, data: FormData): Promise<FormState> => {
      const request = toLogTicketRequest(data)
      const errors = validateRequest(request)
      if (Object.keys(errors).length > 0) return { phase: 'invalid', errors, values: request }

      try {
        const ticket = await onSubmit(request)
        onLogged?.(ticket)
        return { phase: 'logged', reference: ticket.reference }
      } catch (error) {
        const serverErrors = error instanceof ApiError ? (error.problem?.errors ?? {}) : {}
        return { phase: 'invalid', errors: serverErrors, values: request, message: failureMessage(error) }
      }
    },
    { phase: 'idle' },
  )

  const values = state.phase === 'invalid' ? state.values : undefined
  const errors = state.phase === 'invalid' ? state.errors : {}

  return (
    <form action={formAction} noValidate className="flex flex-col gap-4" aria-labelledby="new-request-heading">
      <div className="mb-1 border-b border-rule pb-4">
        <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-river">Need a hand?</p>
        <h2 id="new-request-heading" className="mt-1 font-display text-3xl leading-tight text-mill">Tell us what’s up.</h2>
        <p className="mt-2 text-sm leading-6 text-ink-muted">Share a few details and our team will pick it up.</p>
      </div>

      {state.phase === 'logged' && (
        <FeedbackMessage tone="success">
          Thanks - {state.reference} is logged. An engineer will reply within the time shown against it.
        </FeedbackMessage>
      )}
      {state.phase === 'invalid' && state.message && (
        <FeedbackMessage>
          {state.message}
        </FeedbackMessage>
      )}

      <TextField label="What do you need?" name="title" maxLength={150} defaultValue={values?.title} error={errors.title} placeholder="e.g. Laptop for a new starter" />
      <SelectField label="Type of job" name="category" defaultValue={values?.category ?? 'NEW_STARTER'}>
        {TICKET_CATEGORIES.map((category) => (
          <option key={category} value={category}>
            {CATEGORY_LABELS[category]}
          </option>
        ))}
      </SelectField>
      <SelectField label="How urgent is it?" name="priority" defaultValue={values?.priority ?? 'NORMAL'}>
        {TICKET_PRIORITIES.map((priority) => (
          <option key={priority} value={priority}>
            {PRIORITY_LABELS[priority]}
          </option>
        ))}
      </SelectField>
      <TextField label="Company" name="companyName" maxLength={120} defaultValue={values?.companyName} error={errors.companyName} />
      <TextAreaField label="Details" name="description" maxLength={4000} defaultValue={values?.description} error={errors.description} hint="Who is it for, where, and by when? Plain English is perfect." />
      <TextField label="Needed by (optional)" name="requiredBy" type="date" defaultValue={values?.requiredBy ?? undefined} error={errors.requiredBy} />

      <Button type="submit" disabled={pending}>
        {pending ? 'Logging…' : 'Log this request'}
      </Button>
    </form>
  )
}
