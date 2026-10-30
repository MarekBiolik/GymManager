/** Local calendar helpers (YYYY-MM-DD). */

export function formatIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function parseIsoDate(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, (m ?? 1) - 1, d ?? 1);
}

export function addDays(iso: string, delta: number): string {
  const d = parseIsoDate(iso);
  d.setDate(d.getDate() + delta);
  return formatIsoDate(d);
}

export function weekStrip(centerIso: string): string[] {
  const center = parseIsoDate(centerIso);
  const day = center.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  const monday = new Date(center);
  monday.setDate(center.getDate() + mondayOffset);
  const days: string[] = [];
  for (let i = 0; i < 7; i += 1) {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    days.push(formatIsoDate(d));
  }
  return days;
}

export function shortWeekday(iso: string): string {
  return parseIsoDate(iso).toLocaleDateString(undefined, { weekday: "short" });
}

export function shortDayMonth(iso: string): string {
  return parseIsoDate(iso).toLocaleDateString(undefined, { day: "numeric", month: "short" });
}
