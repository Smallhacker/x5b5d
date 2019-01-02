package com.smallhacker.x5b5d

typealias X5Visitor = Visitor<X5ExpressionNode>

class X5DefineNode(val name: String, val body: X5ExpressionNode)

interface X5ExpressionNode : Visitable<X5ExpressionNode> {
    fun isContextual(program: X5Program): Boolean
    fun evaluate(context: X5Context): X5Array
    override fun toString(): String
    fun toRawString(): String
    fun expand(program: X5Program): X5ExpressionNode
}

class X5ArrayNode(private val elements: List<X5ExpressionNode>) : X5ExpressionNode {
    override fun isContextual(program: X5Program) = elements.any { it.isContextual(program) }

    override fun evaluate(context: X5Context) = if (elements.isEmpty()) X5EmptyArray else X5FiniteArray(elements.map { it.evaluate(context) })

    override fun toString() = elements.asSequence()
            .map { it.toString() }
            .joinToString(", ", "[", "]")

    override fun toRawString() = elements.asSequence()
            .map { it.toRawString() }
            .joinToString(", ", "[", "]")

    override fun visit(visitor: X5Visitor) {
        visitor.addNow(this)
        visitor.addLater(elements.asSequence())
    }

    override fun expand(program: X5Program) = X5ArrayNode(elements.map { it.expand(program) })
}

class X5IndexingNode(private val array: X5ExpressionNode, private val index: X5ExpressionNode) : X5ExpressionNode {
    override fun isContextual(program: X5Program) = array.isContextual(program) || index.isContextual(program)

    override fun evaluate(context: X5Context) = X5LazilyIndexedArray(array.evaluate(context), index.evaluate(context))

    override fun toString() = "$array[$index]"

    override fun toRawString() = "${array.toRawString()}[${index.toRawString()}]"

    override fun visit(visitor: X5Visitor) {
        visitor.addNow(this)
        visitor.addLater(array, index)
    }

    override fun expand(program: X5Program) = X5IndexingNode(array.expand(program), index.expand(program))
}

class X5MacroNode(val name: String) : X5ExpressionNode {
    override fun isContextual(program: X5Program) = program.defines[name]?.isContextual(program) ?: true

    private fun getNode(program: X5Program): X5ExpressionNode = program.defines[name]
            ?: throw IllegalStateException("Unknown define: $name")

    override fun evaluate(context: X5Context) = getNode(context.program)
            .evaluate(context)
            .let {
                if (isContextual(context.program)) {
                    it
                } else {
                    X5NamedArray(name, it)
                }
            }

    override fun toString() = name

    override fun toRawString() = name

    override fun visit(visitor: X5Visitor) {
        visitor.addNow(this)
    }

    override fun expand(program: X5Program) = getNode(program).expand(program)
}

class X5FunctionNode(private val body: X5ExpressionNode) : X5ExpressionNode {
    override fun isContextual(program: X5Program) = false

    override fun evaluate(context: X5Context) = X5FunctionArray(body, context)

    override fun toString() = "{$body}"

    override fun toRawString() = "{${body.toRawString()}}"

    override fun visit(visitor: X5Visitor) {
        visitor.addNow(this)
        visitor.addLater(body)
    }

    override fun expand(program: X5Program) = X5FunctionNode(body.expand(program))
}

object X5ArgumentsNode : X5ExpressionNode {
    override fun isContextual(program: X5Program) = true

    override fun evaluate(context: X5Context) = context.arguments

    override fun toString() = "@"

    override fun toRawString() = toString()

    override fun visit(visitor: X5Visitor) {
        visitor.addNow(this)
    }

    override fun expand(program: X5Program) = this
}

class X5Program(val defines: Map<String, X5ExpressionNode>, val code: X5ExpressionNode)