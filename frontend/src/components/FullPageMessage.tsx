import type { ReactNode } from 'react'

export const FullPageMessage = ({ children }: { children: ReactNode }) => (
  <main className="grid min-h-screen place-items-center p-6 text-lg text-ink-muted" role="status">
    {children}
  </main>
)
