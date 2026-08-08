package com.todo.server.auth

import kotlin.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Sliding-window in-memory rate limiter keyed by an arbitrary string
 * (e.g. client IP). Intended for authentication endpoints. In a multi-node
 * deployment this should be replaced by a shared store (e.g. Redis).
 */
class RateLimiter(
    private val maxRequests: Int,
    private val window: Duration,
) {
    private val hits = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        val deque = hits.computeIfAbsent(key) { ArrayDeque() }
        synchronized(deque) {
            while (deque.isNotEmpty() && now - deque.first() > window.inWholeMilliseconds) {
                deque.removeFirst()
            }
            if (deque.size >= maxRequests) return false
            deque.addLast(now)
            return true
        }
    }

    fun clear(key: String) {
        hits.remove(key)
    }
}
