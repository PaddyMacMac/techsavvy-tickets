export type AuthMode = 'dev' | 'entra'

const read = (key: string): string => (import.meta.env[key] as string | undefined) ?? ''

export const env = {
  authMode: (read('VITE_AUTH_MODE') || 'dev') as AuthMode,
  /** Empty means "same origin" - locally Vite proxies /api to the backend. */
  apiBaseUrl: read('VITE_API_BASE_URL').replace(/\/$/, ''),
  entraTenantId: read('VITE_ENTRA_TENANT_ID'),
  entraClientId: read('VITE_ENTRA_CLIENT_ID'),
} as const
