import { env } from '../config/env'
import type { ProblemDetail } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, message: string, problem: ProblemDetail | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export type TokenProvider = () => Promise<string | null>

export interface HttpClient {
  get: <T>(path: string) => Promise<T>
  post: <T>(path: string, body: unknown) => Promise<T>
  patch: <T>(path: string, body: unknown) => Promise<T>
}

const readProblem = async (response: Response): Promise<ProblemDetail | null> => {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}

export const createHttpClient = (getToken: TokenProvider, baseUrl: string = env.apiBaseUrl): HttpClient => {
  const send = async <T>(method: string, path: string, body?: unknown): Promise<T> => {
    const token = await getToken()
    const headers: Record<string, string> = { Accept: 'application/json' }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    if (token) headers.Authorization = `Bearer ${token}`

    const response = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })

    if (!response.ok) {
      const problem = await readProblem(response)
      throw new ApiError(response.status, problem?.detail ?? problem?.title ?? `Request failed (${response.status})`, problem)
    }
    return (await response.json()) as T
  }

  return {
    get: <T>(path: string) => send<T>('GET', path),
    post: <T>(path: string, body: unknown) => send<T>('POST', path, body),
    patch: <T>(path: string, body: unknown) => send<T>('PATCH', path, body),
  }
}
