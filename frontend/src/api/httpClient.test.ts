import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, createHttpClient } from './httpClient'

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

afterEach(() => vi.unstubAllGlobals())

describe('createHttpClient', () => {
  it('sends the bearer token and JSON body', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }))
    vi.stubGlobal('fetch', fetchMock)

    await createHttpClient(async () => 'abc123', 'https://api.example.test').post('/api/tickets', { title: 'x' })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('https://api.example.test/api/tickets')
    expect(init.method).toBe('POST')
    expect(init.headers).toMatchObject({ Authorization: 'Bearer abc123', 'Content-Type': 'application/json' })
    expect(init.body).toBe('{"title":"x"}')
  })

  it('turns problem details into an ApiError with field errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ title: 'Validation failed', detail: 'Some details need fixing.', errors: { title: 'must not be blank' } }, 400)),
    )

    const failure = await createHttpClient(async () => null, '').post('/api/tickets', {}).catch((error: unknown) => error)

    expect(failure).toBeInstanceOf(ApiError)
    expect(failure).toMatchObject({ status: 400, message: 'Some details need fixing.', problem: { errors: { title: 'must not be blank' } } })
  })
})
