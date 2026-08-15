# 📋 Cachex LLD Roadmap & TODO

- [ ] **Path 1: Eliminate $O(N)$ Removal with `TreeSet` (Time Complexity Optimization)**
    - **Problem:** `PriorityQueue.remove(node)` runs an $O(N)$ linear scan. Every `put()`, `remove()`, and `removeLast()` suffers from this.
    - **Fix:** Replace `PriorityQueue` with a `TreeSet<Node<K, V>>` using a compound comparator `(expiresAt, uniqueSequenceId)` to guarantee $O(\log N)$ insertions and deletions.

- [ ] **Path 2: Concurrency & Thread-Safety (Production Readiness)**
    - **Problem:** `HashMap`, `PriorityQueue` / `TreeSet`, and DLL pointers will corrupt under concurrent multi-threaded read/write access.
    - **Fix:**
        - *Simple:* `ReentrantReadWriteLock` or coarse synchronization.
        - *Advanced:* Segmented cache (like Guava's `ConcurrentLinkedHashMap` or `StripedLock`) to minimize lock contention.

- [ ] **Path 3: Background Expiration Daemon (Active Cleanup)**
    - **Problem:** If a key is inserted with a short TTL and never accessed again, it sits in memory indefinitely without `put()` or `get()` triggering eviction.
    - **Fix:** Introduce a `ScheduledExecutorService` that runs a periodic background task (e.g., every 500ms) to call `evictExpired()`.