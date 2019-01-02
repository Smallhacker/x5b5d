package com.smallhacker.x5b5d

import java.util.*

object X5Parser {
    fun parse(code: String): X5Program = X5Reader(tokenize(code))
            .readProgram()
            .also { validateMacros(it) }

    private fun validateMacros(program: X5Program) {
        val explicitUsages = HashMap<String, List<String>>()
        program.defines.forEach { name, expression ->
            val macros = Visitor.visit(expression)
                    .filterIsInstance<X5MacroNode>()
                    .map { it.name }
                    .toList()
            explicitUsages[name] = macros
        }
        val cycles = CycleDetector.detectCycles(explicitUsages)
                .toList()

        if (cycles.isNotEmpty()) {
            throw IllegalStateException("Circle references detected in macros: " + cycles.joinToString(", "))
        }
    }

    private fun tokenize(code: String): List<X5Token> {
        val s = StringStepper(code)
        val tokens = ArrayList<X5Token>()
        while (!s.over) {
            val char = s.next
            when (char) {
                '[' -> X5Token.ArrayOpen
                ']' -> X5Token.ArrayClose
                '{' -> X5Token.FunctionOpen
                '}' -> X5Token.FunctionClose
                '@' -> X5Token.Arguments
                '<' -> X5Token.DefineOpen
                '>' -> X5Token.DefineClose
                '(' -> X5Token.ParenOpen
                ')' -> X5Token.ParenClose
                ',' -> X5Token.Comma
                ';' -> {
                    s.readWhile { it != '\r' && it != '\n' }
                    null
                }
                else -> when {
                    isConstantChar(char) -> {
                        X5Token.Constant(char + s.readWhile { isConstantChar(it) })
                    }
                    char.isWhitespace() -> null
                    else -> throw IllegalStateException("Unexpected character: $char")
                }
            }?.run { tokens.add(this) }
        }
        return tokens
    }

    private fun isConstantChar(c: Char) = ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || (c == '_')
}

private class X5Reader(l: List<X5Token>) : Stepper<X5Token>(l.iterator()) {
    fun readProgram(): X5Program {
        val defines = HashMap<String, X5ExpressionNode>()
        var code: X5ExpressionNode? = null
        while (!over) {
            val t = peek
            when (t) {
                is X5Token.DefineOpen -> readDefine().let { defines.put(it.name, it.body) }
                else -> {
                    if (code == null) {
                        code = readExpression()
                    } else {
                        throw IllegalStateException("Multiple code blocks")
                    }
                }
            }
        }

        return X5Program(defines, code ?: throw IllegalStateException("No code block"))
    }

    fun readExpression(): X5ExpressionNode {
        val expression = when (val t = peek) {
            is X5Token.ArrayOpen -> readArray()
            is X5Token.FunctionOpen -> readFunction()
            is X5Token.ParenOpen -> readParenthesis()
            is X5Token.Arguments -> X5ArgumentsNode.also { step() }
            is X5Token.Constant -> X5MacroNode(t.name).also { step() }
            else -> throw IllegalStateException("Unexpected $t")
        }
        return readIndexingIfPossible(expression)
    }

    private fun readDefine(): X5DefineNode {
        expect(X5Token.DefineOpen)
        val constant = readConstant()
        if (!constant.name.all { isMacroChar(it) }) {
            throw IllegalStateException("Unsupported macro name: ${constant.name}")
        }

        expect(X5Token.Comma)
        val value = readExpression()
        expect(X5Token.DefineClose)
        return X5DefineNode(constant.name, value)
    }

    private fun readParenthesis(): X5ExpressionNode {
        expect(X5Token.ParenOpen)
        val value = readExpression()
        expect(X5Token.ParenClose)
        return value
    }

    private fun readFunction(): X5ExpressionNode {
        expect(X5Token.FunctionOpen)
        val value = readExpression()
        expect(X5Token.FunctionClose)
        return X5FunctionNode(value)
    }

    private fun readArray(): X5ArrayNode {
        expect(X5Token.ArrayOpen)
        if (peek is X5Token.ArrayClose) {
            step()
            return X5ArrayNode(emptyList())
        }

        val elements = ArrayList<X5ExpressionNode>()
        loop@ while (true) {
            elements.add(readExpression())
            val n = next
            return when (n) {
                is X5Token.ArrayClose -> X5ArrayNode(elements)
                is X5Token.Comma -> continue@loop
                else -> throw IllegalStateException("Unexpected $n")
            }
        }
    }

    private fun readIndexingIfPossible(context: X5ExpressionNode): X5ExpressionNode {
        var outer = context
        while (nextIs(X5Token.ArrayOpen)) {
            outer = readIndexing(outer)
        }
        return outer
    }

    private fun readIndexing(context: X5ExpressionNode): X5ExpressionNode {
        expect(X5Token.ArrayOpen)
        val indexing = readExpression()
        expect(X5Token.ArrayClose)
        return X5IndexingNode(context, indexing)
    }

    private fun expect(token: X5Token) {
        if (next != token) {
            throw IllegalStateException("Expected $token")
        }
    }

    private fun readConstant() = (next as? X5Token.Constant)
            ?.name
            ?.let { X5MacroNode(it) }
            ?: throw IllegalStateException("Expected Constant")

    private fun nextIs(value: X5Token) = !over && (peek == value)

    private fun isMacroChar(c: Char) = ((c >= 'A') && (c <= 'Z')) || (c == '_')
}