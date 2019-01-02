package com.smallhacker.x5b5d

private val STDLIB = """
<ZERO, []>
<PARAM, @[ZERO]>
<INC, {
    [PARAM]
}>
<ONE, [ZERO]>
<TWO, [ONE]>
<THREE, [TWO]>
<DEC, {
    PARAM[ZERO]
}>
<IFEMPTY, {
    [PARAM[ONE], PARAM[TWO]][PARAM[ZERO]]
}>
<Y, {{@[ONE][[@[ONE], @[ZERO]]]}}>
<YTHIS, Y[PARAM[ZERO]]>
<YPARAM, PARAM[ONE]>
""".trimIndent()

private fun test(code: String, expected: String, std: Boolean = true) {
    val c = if (std) {
        STDLIB + System.lineSeparator() + code
    } else {
        code
    }
    val result = X5Runner.run(c)
    val toString = result.toRawString()
    if (toString != expected) {
        throw IllegalStateException("""Expected "$expected", found "$toString". Code:
    $code""")
    }
}

fun main() {
    test("[]", "[]")
    test("[[]]", "[[]]")
    test("[[[]]]", "[[[]]]")
    test("[ZERO]", "[[]]")
    test("[ZERO, ONE, [TWO]]", "[[], [[]], [[[[]]]]]")
    test("ZERO", "[]")
    test("ONE", "[[]]")
    test("INC[ZERO]", "[[]]")
    test("INC[ONE]", "[[[]]]")
    test("INC[[]]", "[[]]")
    test("DEC[ZERO]", "[]")
    test("DEC[ONE]", "[]")
    test("DEC[TWO]", "[[]]")
    test("[THREE, TWO, ONE, ZERO][ZERO]", "[[[[]]]]")
    test("[THREE, TWO, ONE, ZERO][ONE]", "[[[]]]")
    test("[THREE, TWO, ONE, ZERO][TWO]", "[[]]")
    test("[THREE, TWO, ONE, ZERO][THREE]", "[]")
    test("[ZERO, ONE, THREE][ZERO]", "[]")
    test("[ONE, TWO][[ZERO][ZERO]]", "[[]]")
    test("[ONE, TWO][ZERO]", "[[]]")
    test("IFEMPTY[[ZERO, ONE, TWO]]", "[[]]")
    test("IFEMPTY[[ONE, ONE, TWO]]", "[[[]]]")
    test("IFEMPTY[[TWO, ONE, TWO]]", "[[[]]]")
    test("IFEMPTY[[THREE, ONE, TWO]]", "[[[]]]")
    println("Tests passed")
}