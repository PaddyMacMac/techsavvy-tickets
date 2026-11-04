import type { HttpClient } from '../../../api/httpClient'
import type { Me } from '../types'

export const createIdentityApi = (http: HttpClient) => ({
  me: () => http.get<Me>('/api/me'),
})

export type IdentityApi = ReturnType<typeof createIdentityApi>
