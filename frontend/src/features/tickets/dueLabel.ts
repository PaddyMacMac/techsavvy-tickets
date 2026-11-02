const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

export interface DueLabel {
  text: string
  overdue: boolean
}

const humanise = (milliseconds: number): string => {
  const size = Math.abs(milliseconds)
  if (size < HOUR) return `${Math.max(1, Math.round(size / MINUTE))} min`
  if (size < DAY) return `${Math.round(size / HOUR)} h`
  const days = Math.round(size / DAY)
  return days === 1 ? '1 day' : `${days} days`
}

export const describeResponseDue = (respondBy: string, now: Date = new Date()): DueLabel => {
  const difference = new Date(respondBy).getTime() - now.getTime()
  return difference >= 0
    ? { text: `Reply due in ${humanise(difference)}`, overdue: false }
    : { text: `Reply overdue by ${humanise(difference)}`, overdue: true }
}
