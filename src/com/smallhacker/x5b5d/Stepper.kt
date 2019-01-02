package com.smallhacker.x5b5d

internal abstract class Stepper<I : Any>(private val iterable: Iterator<I>) {
    private var nextVal: I? = null

    val over get() = nextVal == null && !iterable.hasNext()

    val peek: I
        get() {
            nextVal?.let { return it }
            if (over) {
                throw NoSuchElementException("Source depleted")
            }
            val v = iterable.next()
            nextVal = v
            return v
        }

    fun step() {
        nextVal ?: peek
        nextVal = null
    }

    val next get() = peek.also { step() }

}

internal class StringStepper(s: String): Stepper<Char>(s.iterator()) {
    fun readWhile(condition: (Char) -> Boolean): String {
        val b = StringBuilder()
        while (!over) {
            val i = peek
            if (!condition(i)) {
                break
            }
            b.append(i)
            step()
        }
        return b.toString()
    }
}
