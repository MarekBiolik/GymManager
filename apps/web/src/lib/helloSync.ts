/**
 * Thin supabase-js glue for hello-sync smoke (profiles + measurements).
 * Mirrors docs/specs/hello-sync-read-cache.md — no TS domain layer.
 */
import {
  cacheClearForUser,
  cacheGet,
  cacheKeys,
  cachePut,
  cacheRemove,
  type CacheEntry,
} from "./readCache";
import { supabase } from "./supabaseClient";

export type ProfileRow = {
  id: string;
  preferred_units: "kg" | "lb";
  created_at: string;
  updated_at: string;
};

export type MeasurementRow = {
  id: string;
  user_id: string;
  metric: string;
  value: number;
  measured_at: string;
  created_at: string;
  updated_at: string;
};

export type ReadOutcome<T> =
  | { kind: "fresh"; data: T }
  | { kind: "stale"; data: T; fetchedAt: number }
  | { kind: "empty"; reason: "offline" | "error" | "not_found" };

export type WriteOutcome<T> =
  | { kind: "ok"; data: T }
  | { kind: "offline" }
  | { kind: "error"; message: string };

function isOnline(): boolean {
  return typeof navigator === "undefined" ? true : navigator.onLine;
}

function lwwMeasurement(a: MeasurementRow, b: MeasurementRow): MeasurementRow {
  return a.updated_at >= b.updated_at ? a : b;
}

export function wipeHelloSyncCache(userId: string): void {
  cacheClearForUser(userId);
}

export async function readProfile(userId: string): Promise<ReadOutcome<ProfileRow>> {
  const key = cacheKeys.profile(userId);
  if (!isOnline() || !supabase) {
    const hit = cacheGet<ProfileRow>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: !supabase ? "error" : "offline" };
  }
  try {
    const { data, error } = await supabase.from("profiles").select("*").eq("id", userId).maybeSingle();
    if (error) {
      throw error;
    }
    if (!data) {
      cacheRemove(key);
      return { kind: "empty", reason: "not_found" };
    }
    const row = data as ProfileRow;
    cachePut(key, { payload: row, fetchedAt: Date.now(), userId });
    return { kind: "fresh", data: row };
  } catch {
    const hit = cacheGet<ProfileRow>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: "error" };
  }
}

export async function readMeasurements(userId: string): Promise<ReadOutcome<MeasurementRow[]>> {
  const key = cacheKeys.measurementsList(userId);
  if (!isOnline() || !supabase) {
    const hit = cacheGet<MeasurementRow[]>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: !supabase ? "error" : "offline" };
  }
  try {
    const { data, error } = await supabase
      .from("measurements")
      .select("*")
      .eq("user_id", userId)
      .order("measured_at", { ascending: false });
    if (error) {
      throw error;
    }
    const rows = (data ?? []) as MeasurementRow[];
    const now = Date.now();
    cachePut(key, { payload: rows, fetchedAt: now, userId });
    rows.forEach((row) => {
      cachePut(cacheKeys.measurementItem(row.id), { payload: row, fetchedAt: now, userId });
    });
    return { kind: "fresh", data: rows };
  } catch {
    const hit = cacheGet<MeasurementRow[]>(key);
    if (hit && hit.userId === userId) {
      return { kind: "stale", data: hit.payload, fetchedAt: hit.fetchedAt };
    }
    return { kind: "empty", reason: "error" };
  }
}

export async function insertMeasurement(
  userId: string,
  input: { metric: string; value: number; measuredAtIso: string },
): Promise<WriteOutcome<MeasurementRow>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { data, error } = await supabase
      .from("measurements")
      .insert({
        user_id: userId,
        metric: input.metric,
        value: input.value,
        measured_at: input.measuredAtIso,
      })
      .select()
      .single();
    if (error) {
      throw error;
    }
    const row = data as MeasurementRow;
    upsertMeasurementCaches(row);
    return { kind: "ok", data: row };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Insert failed" };
  }
}

export async function deleteMeasurement(userId: string, id: string): Promise<WriteOutcome<null>> {
  if (!isOnline()) {
    return { kind: "offline" };
  }
  if (!supabase) {
    return { kind: "error", message: "Supabase is not configured" };
  }
  try {
    const { error } = await supabase.from("measurements").delete().eq("id", id);
    if (error) {
      throw error;
    }
    cacheRemove(cacheKeys.measurementItem(id));
    const listKey = cacheKeys.measurementsList(userId);
    const list = cacheGet<MeasurementRow[]>(listKey);
    if (list && list.userId === userId) {
      cachePut(listKey, {
        payload: list.payload.filter((m) => m.id !== id),
        fetchedAt: Date.now(),
        userId,
      });
    }
    return { kind: "ok", data: null };
  } catch (err: unknown) {
    return { kind: "error", message: err instanceof Error ? err.message : "Delete failed" };
  }
}

function upsertMeasurementCaches(row: MeasurementRow): void {
  const now = Date.now();
  const itemKey = cacheKeys.measurementItem(row.id);
  const previous = cacheGet<MeasurementRow>(itemKey)?.payload;
  const winner = previous ? lwwMeasurement(previous, row) : row;
  cachePut(itemKey, { payload: winner, fetchedAt: now, userId: row.user_id });
  const listKey = cacheKeys.measurementsList(row.user_id);
  const existing = cacheGet<MeasurementRow[]>(listKey);
  let next: MeasurementRow[];
  if (existing && existing.userId === row.user_id) {
    const base = existing.payload.filter((m) => m.id !== row.id);
    next = [...base, winner].sort((a, b) => (a.measured_at < b.measured_at ? 1 : -1));
  } else {
    next = [winner];
  }
  cachePut(listKey, { payload: next, fetchedAt: now, userId: row.user_id } satisfies CacheEntry<MeasurementRow[]>);
}
