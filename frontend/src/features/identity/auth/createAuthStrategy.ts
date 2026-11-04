import { env } from '../../../config/env'
import type { AuthStrategy } from './authStrategy'
import { createDevStrategy } from './devStrategy'
import { createMsalStrategy } from './msalStrategy'

export const createAuthStrategy = (): AuthStrategy => {
  switch (env.authMode) {
    case 'entra':
      if (!env.entraTenantId || !env.entraClientId) {
        throw new Error('VITE_ENTRA_TENANT_ID and VITE_ENTRA_CLIENT_ID must be set when VITE_AUTH_MODE=entra')
      }
      return createMsalStrategy({ tenantId: env.entraTenantId, clientId: env.entraClientId })
    case 'dev':
      return createDevStrategy()
  }
}
