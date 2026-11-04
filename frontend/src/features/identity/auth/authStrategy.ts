export interface Credentials {
  email: string
  password: string
}

/**
 * Strategy pattern: Identity only knows this interface. "entra" signs in with Microsoft in production,
 * "dev" signs in against the local backend so you can work without an Azure tenant.
 */
export interface AuthStrategy {
  readonly kind: 'dev' | 'entra'
  /** Finishes any redirect sign-in and reports whether a session already exists. */
  initialise: () => Promise<boolean>
  signIn: (credentials?: Credentials) => Promise<void>
  signOut: () => Promise<void>
  getAccessToken: () => Promise<string | null>
}
