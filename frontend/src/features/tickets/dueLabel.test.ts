import { describe, expect, it } from 'vitest'
import { describeResponseDue } from './dueLabel'

const now = new Date('2026-10-26T09:00:00Z')

describe('describeResponseDue', () => {
  it('counts down in minutes when the reply is due within the hour', () => {
    expect(describeResponseDue('2026-10-26T09:40:00Z', now)).toEqual({ text: 'Reply due in 40 min', overdue: false })
  })

  it('switches to hours and days as the gap grows', () => {
    expect(describeResponseDue('2026-10-26T13:00:00Z', now).text).toBe('Reply due in 4 h')
    expect(describeResponseDue('2026-10-28T09:00:00Z', now).text).toBe('Reply due in 2 days')
  })

  it('flags late replies', () => {
    expect(describeResponseDue('2026-10-26T07:00:00Z', now)).toEqual({ text: 'Reply overdue by 2 h', overdue: true })
  })
})
