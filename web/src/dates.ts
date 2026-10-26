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
