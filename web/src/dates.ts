export function todayLocal(): string {
  return formatIsoDate(new Date())
}

export function formatIsoDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function parseIsoDate(isoDate: string): Date {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function shiftIsoDate(isoDate: string, deltaDays: number): string {
  const date = parseIsoDate(isoDate)
  date.setDate(date.getDate() + deltaDays)
  return formatIsoDate(date)
}

export function formatDateLabel(isoDate: string): string {
  return new Intl.DateTimeFormat('pl-PL', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(parseIsoDate(isoDate))
}

export function isToday(isoDate: string): boolean {
  return isoDate === todayLocal()
}

/** Monday-first week containing the given date. */
export function startOfWeekMonday(isoDate: string): string {
  const date = parseIsoDate(isoDate)
  const day = date.getDay() // 0 Sun .. 6 Sat
  const offset = day === 0 ? -6 : 1 - day
  date.setDate(date.getDate() + offset)
  return formatIsoDate(date)
}

export function weekDates(isoDate: string): string[] {
  const start = startOfWeekMonday(isoDate)
  return Array.from({ length: 7 }, (_, i) => shiftIsoDate(start, i))
}

/** Polish weekday short labels, Monday-first. */
export const WEEKDAY_SHORT_PL = ['Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'So', 'Nd'] as const

export function formatWeekRangeLabel(isoDate: string): string {
  const days = weekDates(isoDate)
  const start = parseIsoDate(days[0])
  const end = parseIsoDate(days[6])
  const sameMonth = start.getMonth() === end.getMonth()
  const startLabel = new Intl.DateTimeFormat('pl-PL', {
    day: 'numeric',
    month: sameMonth ? undefined : 'short',
  }).format(start)
  const endLabel = new Intl.DateTimeFormat('pl-PL', {
    day: 'numeric',
    month: 'short',
  }).format(end)
  return `${startLabel} – ${endLabel}`
}
