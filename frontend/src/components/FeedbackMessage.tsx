import type { ReactNode } from 'react'

type Tone = 'error' | 'success'

const styles: Record<Tone, string> = {
  error: 'border-brick/30 bg-brick/10 text-brick',
  success: 'border-moss/30 bg-moss/10 text-moss',
}

export const FeedbackMessage = ({
  tone = 'error',
  children,
}: {
  tone?: Tone
  children: ReactNode
}) => (
  <p role={tone === 'error' ? 'alert' : 'status'} className={`rounded-2xl border p-3 font-medium ${styles[tone]}`}>
    {children}
  </p>
)
