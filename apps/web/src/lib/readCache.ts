/**
 * Thin hello-sync read cache (ADR 0006 / #19). Memory + sessionStorage.
 * Not a domain sync engine — key/invalidation rules only.
 */

export type CacheEntry<T> = {
  payload: T;
  fetchedAt: number;
  userId: string;
};

const MEMORY = new Map<string, CacheEntry<unknown>>();
const STORAGE_PREFIX = "gm.hello-sync.";

function storageKey(key: string): string {
  return STORAGE_PREFIX + key;
}

export const cacheKeys = {
  profile: (userId: string) => `profile:${userId}`,
  measurementsList: (userId: string) => `measurements:list:${userId}`,
  measurementItem: (id: string) => `measurements:item:${id}`,
  nutritionSettings: (userId: string) => `nutrition:settings:${userId}`,
  mealTypesList: (userId: string) => `nutrition:mealTypes:${userId}`,
  foodsList: (userId: string) => `nutrition:foods:${userId}`,
  foodEntriesDay: (userId: string, eatenOn: string) => `nutrition:entries:${userId}:${eatenOn}`,
};

export function cacheGet<T>(key: string): CacheEntry<T> | null {
  const mem = MEMORY.get(key) as CacheEntry<T> | undefined;
  if (mem) {
    return mem;
  }
  if (typeof sessionStorage === "undefined") {
    return null;
  }
  try {
    const raw = sessionStorage.getItem(storageKey(key));
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as CacheEntry<T>;
    MEMORY.set(key, parsed);
    return parsed;
  } catch {
    return null;
  }
}

export function cachePut<T>(key: string, entry: CacheEntry<T>): void {
  MEMORY.set(key, entry);
  if (typeof sessionStorage === "undefined") {
    return;
  }
  try {
    sessionStorage.setItem(storageKey(key), JSON.stringify(entry));
  } catch {
    // Quota / private mode — memory still works.
  }
}

export function cacheRemove(key: string): void {
  MEMORY.delete(key);
  if (typeof sessionStorage === "undefined") {
    return;
  }
  try {
    sessionStorage.removeItem(storageKey(key));
  } catch {
    // ignore
  }
}

/** Wipe all hello-sync entries for a user (logout / user change). */
export function cacheClearForUser(userId: string): void {
  for (const [key, entry] of [...MEMORY.entries()]) {
    if (entry.userId === userId) {
      cacheRemove(key);
    }
  }
  if (typeof sessionStorage === "undefined") {
    return;
  }
  try {
    const toRemove: string[] = [];
    for (let i = 0; i < sessionStorage.length; i += 1) {
      const full = sessionStorage.key(i);
      if (!full?.startsWith(STORAGE_PREFIX)) {
        continue;
      }
      const raw = sessionStorage.getItem(full);
      if (!raw) {
        continue;
      }
      try {
        const parsed = JSON.parse(raw) as CacheEntry<unknown>;
        if (parsed.userId === userId) {
          toRemove.push(full);
        }
      } catch {
        // ignore bad entries
      }
    }
    toRemove.forEach((k) => sessionStorage.removeItem(k));
  } catch {
    // ignore
  }
}

export function cacheClearAll(): void {
  MEMORY.clear();
  if (typeof sessionStorage === "undefined") {
    return;
  }
  try {
    const keys: string[] = [];
    for (let i = 0; i < sessionStorage.length; i += 1) {
      const k = sessionStorage.key(i);
      if (k?.startsWith(STORAGE_PREFIX)) {
        keys.push(k);
      }
    }
    keys.forEach((k) => sessionStorage.removeItem(k));
  } catch {
    // ignore
  }
}
