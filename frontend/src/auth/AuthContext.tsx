import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, createHttpClient } from '../api/httpClient'
import { createTicketApi } from '../api/ticketApi'
import type { Me } from '../api/types'
import type { AuthStrategy, Credentials } from './authStrategy'
import { createAuthStrategy } from './createAuthStrategy'

type AuthState =
  | { status: 'loading'; user: null; error: null }
  | { status: 'anonymous'; user: null; error: string | null }
  | { status: 'authenticated'; user: Me; error: null }

export interface AuthContextValue {
  status: AuthState['status']
  kind: AuthStrategy['kind']
  user: Me | null
  error: string | null
  signIn: (credentials?: Credentials) => Promise<void>
  signOut: () => Promise<void>
  getAccessToken: () => Promise<string | null>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

const NO_ACCESS = 'You signed in, but your account has not been given access yet. Ask Tech Savvy to assign you a role.'

const describe = (error: unknown): string =>
  error instanceof ApiError && error.status === 403 ? NO_ACCESS : error instanceof Error ? error.message : 'Something went wrong.'

export const AuthProvider = ({ children, strategy: injected }: { children: ReactNode; strategy?: AuthStrategy }) => {
  const [strategy] = useState<AuthStrategy>(() => injected ?? createAuthStrategy())
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null, error: null })

  const loadProfile = useCallback(async () => {
    try {
      const user = await createTicketApi(createHttpClient(strategy.getAccessToken)).me()
      setState({ status: 'authenticated', user, error: null })
    } catch (error) {
      setState({ status: 'anonymous', user: null, error: describe(error) })
    }
  }, [strategy])

  useEffect(() => {
    let cancelled = false
    strategy
      .initialise()
      .then(async (hasSession) => {
        if (cancelled) return
        if (hasSession) await loadProfile()
        else setState({ status: 'anonymous', user: null, error: null })
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ status: 'anonymous', user: null, error: describe(error) })
      })
    return () => {
      cancelled = true
    }
  }, [strategy, loadProfile])

  const signIn = useCallback(
    async (credentials?: Credentials) => {
      await strategy.signIn(credentials)
      await loadProfile()
    },
    [strategy, loadProfile],
  )

  const signOut = useCallback(async () => {
    await strategy.signOut()
    setState({ status: 'anonymous', user: null, error: null })
  }, [strategy])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, kind: strategy.kind, signIn, signOut, getAccessToken: strategy.getAccessToken }),
    [state, strategy, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
