import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

const control =
  'w-full rounded-xl border border-rule bg-white px-3.5 py-3 text-base text-mill placeholder:text-ink-muted/70 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sky aria-[invalid=true]:border-brick'

interface ShellProps {
  label: string
  error?: string
  hint?: string
  render: (props: { id: string; 'aria-invalid': boolean; 'aria-describedby': string | undefined; className: string }) => ReactNode
}

const FieldShell = ({ label, error, hint, render }: ShellProps) => {
  const id = useId()
  const messageId = `${id}-message`
  const message = error ?? hint
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-base font-bold">
        {label}
      </label>
      {render({ id, 'aria-invalid': Boolean(error), 'aria-describedby': message ? messageId : undefined, className: control })}
      {message && (
        <p id={messageId} role={error ? 'alert' : undefined} className={error ? 'text-sm font-medium text-brick' : 'text-sm text-ink-muted'}>
          {message}
        </p>
      )}
    </div>
  )
}

type Common = { label: string; error?: string; hint?: string }

export const TextField = ({ label, error, hint, ...input }: Common & InputHTMLAttributes<HTMLInputElement>) => (
  <FieldShell label={label} error={error} hint={hint} render={(shared) => <input {...input} {...shared} />} />
)

export const TextAreaField = ({ label, error, hint, ...area }: Common & TextareaHTMLAttributes<HTMLTextAreaElement>) => (
  <FieldShell label={label} error={error} hint={hint} render={(shared) => <textarea rows={5} {...area} {...shared} />} />
)

export const SelectField = ({ label, error, hint, children, ...select }: Common & SelectHTMLAttributes<HTMLSelectElement>) => (
  <FieldShell
    label={label}
    error={error}
    hint={hint}
    render={(shared) => (
      <select {...select} {...shared}>
        {children}
      </select>
    )}
  />
)
