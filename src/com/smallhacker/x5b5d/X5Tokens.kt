package com.smallhacker.x5b5d

sealed class X5Token {
    data class Constant(val name: String): X5Token()
    object ArrayOpen: X5Token()
    object ArrayClose: X5Token()
    object Comma: X5Token()
    object Arguments: X5Token()
    object FunctionOpen: X5Token()
    object FunctionClose: X5Token()
    object DefineOpen: X5Token()
    object DefineClose: X5Token()
    object ParenOpen: X5Token()
    object ParenClose: X5Token()
}