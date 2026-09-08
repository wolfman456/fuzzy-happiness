export function createTtlCache(now = Date.now) {
  const store = new Map();

  return {
    get(key) {
      return store.get(key)?.value;
    },
    set(key, value) {
      store.set(key, { value, insertedAt: now() });
    },
    delete(key) {
      store.delete(key);
    },
    clear() {
      store.clear();
    },
    size() {
      return store.size;
    },
    isFresh(key, ttlMs) {
      const entry = store.get(key);
      if (!entry) return false;
      return now() - entry.insertedAt < ttlMs;
    },
    hasExpired(key, ttlMs) {
      const entry = store.get(key);
      if (!entry) return false;
      return now() - entry.insertedAt >= ttlMs;
    },
  };
}