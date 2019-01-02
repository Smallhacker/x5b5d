package com.smallhacker.x5b5d

import java.util.*

interface Visitable<T: Any> {
    fun visit(visitor: Visitor<T>)
}

class Visitor<T : Any> private constructor(start: Visitable<T>) {
    private val items = ArrayDeque<T>()
    private val upcoming = ArrayDeque<Visitable<T>>()

    init {
        addLater(start)
    }

    fun addNow(item: T) = items.addLast(item)

    fun addLater(vararg visitable: Visitable<T>) = visitable.forEach { upcoming.addLast(it) }

    fun addLater(visitables: Sequence<Visitable<T>>) = visitables.forEach { addLater(it) }

    private fun getNext(): T? {
        while (items.isEmpty() && upcoming.isNotEmpty()) {
            upcoming.removeFirst().visit(this)
        }
        if (items.isNotEmpty()) {
            return items.removeFirst()
        }
        return null
    }

    private fun toSequence() = generateSequence { getNext() }

    companion object {
        fun <T : Any> visit(start: Visitable<T>) = Visitor(start).toSequence()
    }
}