import { env } from '../../../config/env'
import type { AuthStrategy, Credentials } from './authStrategy'

const STORAGE_KEY = 'tss.dev.token'

/** LOCAL ONLY. The token sits in sessionStorage for convenience; the Entra strategy never does this. */
export const createDevStrategy = (baseUrl: string = env.apiBaseUrl): AuthStrategy => ({
  kind: 'dev',

  initialise: async () => sessionStorage.getItem(STORAGE_KEY) !== null,

  signIn: async (credentials?: Credentials) => {
    if (!credentials) throw new Error('Enter your email and password.')
    const response = await fetch(`${baseUrl}/api/auth/dev-login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    })
    if (response.status === 401) throw new Error('That email and password do not match.')
    if (!response.ok) throw new Error('Sign-in failed. Is the backend running on port 8080?')
    const { accessToken } = (await response.json()) as { accessToken: string }
    sessionStorage.setItem(STORAGE_KEY, accessToken)
  },

  signOut: async () => sessionStorage.removeItem(STORAGE_KEY),

  getAccessToken: async () => sessionStorage.getItem(STORAGE_KEY),
})
