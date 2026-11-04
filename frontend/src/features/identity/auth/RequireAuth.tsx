import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { FullPageMessage } from '../../../components/FullPageMessage'
import { useAuth } from './useAuth'

export const RequireAuth = ({ children }: { children: ReactNode }) => {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageMessage>Checking your sign-in…</FullPageMessage>
  if (status === 'anonymous') return <Navigate to="/login" replace />
  return <>{children}</>
}
