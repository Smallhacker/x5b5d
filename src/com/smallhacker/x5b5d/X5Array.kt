package com.smallhacker.x5b5d

abstract class X5Array {
    val first: X5Array get() = this[X5EmptyArray]
    open val rank: UInt get() = first.rank + 1u
    abstract val knownFinite: Boolean
    abstract operator fun get(index: X5Array): X5Array
    abstract fun asSequence(): Sequence<X5Array>
    override fun toString(): String = toString { it.toString() }
    open fun toRawString(): String = toString { it.toRawString() }
    open fun evaluateEagerly() = this

    private fun toString(stringifier: (X5Array) -> String): String {
        return asSequence()
                .map(stringifier)
                .let {
                    if (knownFinite) {
                        it
                    } else {
                        it.take(10)
                    }
                }
                .joinToString(", ", "[", "]")
    }
}

class X5LazilyIndexedArray(private val array: X5Array, private val index: X5Array) : X5Array() {
    //private val value = lazy {
    //    array[index]
    //}

    private val value get() = array[index]

    override val rank get() = value.rank

    override val knownFinite = false

    override fun get(index: X5Array) = value[index]

    override fun asSequence() = value.asSequence()

    override fun evaluateEagerly() = value
}

class X5NamedArray(private val name: String, private val array: X5Array) : X5Array() {
    override val knownFinite get() = array.knownFinite
    override val rank get() = array.rank

    override fun get(index: X5Array) = array[index]
    override fun asSequence() = array.asSequence()
    override fun toString() = name
    override fun toRawString() = array.toRawString()
}

object X5EmptyArray : X5Array() {
    override val knownFinite = true
    override val rank = 0u
    override fun get(index: X5Array) = X5EmptyArray
    override fun asSequence() = emptySequence<X5Array>()
}

class X5FiniteArray(private val entries: List<X5Array>) : X5Array() {
    override val knownFinite = true

    init {
        if (entries.isEmpty()) {
            throw IllegalArgumentException("X5FiniteArray cannot be empty")
        }
    }

    override fun get(index: X5Array): X5Array {
        val i = index.rank.toInt()
        return if (i < entries.size) entries[i] else entries.last()
    }

    override fun asSequence() = entries.asSequence()
}

class X5FunctionArray(private val body: X5ExpressionNode, private val context: X5Context) : X5Array() {
    override val knownFinite = false
    override fun get(index: X5Array) = body.evaluate(context.withArgument(index))

    override fun toString() = "{" + body.toString() + "}"
    override fun toRawString() = "{" + body.toRawString() + "}"

    override fun asSequence(): Sequence<X5Array> {
        return generateSequence<X5Array>(X5EmptyArray) {
            X5FiniteArray(listOf(it))
        }
                .map { get(it) }
    }
}

class X5LinkedArray(private val value: X5Array, private val parent: X5Array) : X5Array() {
    override val knownFinite = parent.knownFinite

    override fun get(index: X5Array) =
            if (index.rank == 0u) {
                value
            } else {
                parent[index.first]
            }

    override fun asSequence() = sequenceOf(value).plus(parent.asSequence())

}