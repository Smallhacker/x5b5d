package com.smallhacker.x5b5d

import java.util.*

internal object CycleDetector {
    fun <T> detectCycles(connections: Map<T, List<T>>): Sequence<T> {
        return connections.keys.asSequence()
                .filter { reachableNodes(it, connections).contains(it) }
    }

    private fun <T> reachableNodes(start: T, connections: Map<T, List<T>>): Set<T> {
        val queue = ArrayDeque<T>()
        val enqueued = HashSet<T>()

        fun add(t: T) {
            if (enqueued.add(t))
                queue.add(t)
        }

        fun visit(t: T) {
            connections.getOrDefault(t, emptyList()).forEach { add(it) }
        }

        visit(start)

        while (queue.isNotEmpty()) {
            visit(queue.removeFirst())
        }

        return enqueued
    }
}