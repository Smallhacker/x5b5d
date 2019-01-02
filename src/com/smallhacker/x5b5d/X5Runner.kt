package com.smallhacker.x5b5d

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object X5Runner {
    fun run(code: String): X5Array {
        return runLazily(code)
                .evaluateEagerly()
    }

    internal fun runLazily(code: String): X5Array {
        val program = X5Parser.parse(code)
        return program.code.evaluate(X5Context(program))
    }

    fun preProcess(code: String): X5ExpressionNode {
        val program = X5Parser.parse(code)
        return program.code.expand(program)
    }
}

fun main(args: Array<String>) {
    val params = X5RunnerParams.parse(args)

    if (params == null) {
        println("Usage:")
        println("    [-n] sourceFile.x5")
        println("    -p sourceFile.x5")
        println("Arguments:")
        println("    -n  Attempt to retain macro references in output")
        println("    -p  Only run preprocessor")
        return
    }

    val code = Paths.get(params.inFile)
            .toFile()
            .readText(StandardCharsets.UTF_8)

    if (params.preProcess) {
        val preProcessed = X5Runner.preProcess(code)
        println(preProcessed.toRawString())
    } else {
        val output = X5Runner.  run(code)
        val stringOutput = if (params.retainNames) {
            output.toString()
        } else {
            output.toRawString()
        }

        println(stringOutput)
    }
}

private data class X5RunnerParams(val inFile: String, val preProcess: Boolean = false, val retainNames: Boolean = false) {
    companion object {
        fun parse(args: Array<String>): X5RunnerParams? {
            val size = args.size
            if (size == 0) {
                return null
            }

            when (args[0]) {
                "-p" -> {
                    if (size != 2) {
                        return null
                    }
                    return X5RunnerParams(args[1], preProcess = true)
                }
                "-n" -> {
                    if (size != 2) {
                        return null
                    }
                    return X5RunnerParams(args[1], retainNames = true)
                }
            }

            if (size != 1) {
                return null
            }

            return X5RunnerParams(args.last())

        }
    }
}

class X5Context(val program: X5Program, val arguments: X5Array = X5EmptyArray) {
    fun withArgument(v: X5Array) = X5Context(program, X5LinkedArray(v, arguments))
}