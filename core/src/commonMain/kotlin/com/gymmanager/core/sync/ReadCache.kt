package com.gymmanager.core.sync

/**
 * In-memory read cache for hello-sync. Not a write queue.
 */
interface ReadCache {
    fun <T> get(key: String): CacheEntry<T>?

    fun <T> put(key: String, entry: CacheEntry<T>)

    fun remove(key: String)

    /** Drop every entry whose userId matches (logout / user change). */
    fun clearForUser(userId: String)

    fun clearAll()
}

class InMemoryReadCache : ReadCache {
    private val store = mutableMapOf<String, CacheEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String): CacheEntry<T>? = store[key] as CacheEntry<T>?

    override fun <T> put(key: String, entry: CacheEntry<T>) {
        store[key] = entry
    }

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clearForUser(userId: String) {
        val keys =
            store
                .filterValues { it.userId == userId }
                .keys
                .toList()
        keys.forEach { store.remove(it) }
    }

    override fun clearAll() {
        store.clear()
    }
}
