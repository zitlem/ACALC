package com.acalc.domain

class ExpressionEvaluator {

    fun evaluate(input: String): Double? {
        val cleaned = input.trim().replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return null
        return try {
            val parser = Parser(cleaned)
            val result = parser.parseExpression()
            if (parser.pos != cleaned.length) return null
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val input: String) {
        var pos = 0

        // expression = term (('+' | '-') term)*
        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
                val op = input[pos++]
                val right = parseTerm()
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        // term = power (('*' | '/') power)*
        private fun parseTerm(): Double {
            var result = parsePower()
            while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
                val op = input[pos++]
                val right = parsePower()
                if (op == '/' && right == 0.0) throw ArithmeticException("Division by zero")
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        // power = factor ('^' factor)?  right-associative via recursion
        private fun parsePower(): Double {
            val base = parseFactor()
            return if (pos < input.length && input[pos] == '^') {
                pos++
                Math.pow(base, parseFactor())
            } else base
        }

        // factor = '-' factor | '√' factor | constant | function'('expr')' | '('expr')' | NUMBER
        private fun parseFactor(): Double {
            // Unary minus
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseFactor()
            }
            // Square root prefix
            if (pos < input.length && input[pos] == '√') {
                pos++
                val operand = parseFactor()
                if (operand < 0) throw ArithmeticException("√ of negative number")
                return Math.sqrt(operand)
            }
            // Single-char constants
            if (pos < input.length && input[pos] == 'π') { pos++; return Math.PI }
            if (pos < input.length && input[pos] == 'φ') { pos++; return (1.0 + Math.sqrt(5.0)) / 2.0 }
            // Named functions and constants (e, sin, cos, …)
            if (pos < input.length && input[pos].isLetter()) {
                val nameStart = pos
                while (pos < input.length && input[pos].isLetter()) pos++
                val name = input.substring(nameStart, pos)
                if (pos < input.length && input[pos] == '(') {
                    pos++ // consume '('
                    val arg = parseExpression()
                    if (pos < input.length && input[pos] == ')') pos++
                    else throw IllegalArgumentException("Missing ) after $name(")
                    return applyFunction(name, arg)
                }
                return when (name) {
                    "e"  -> Math.E
                    else -> throw IllegalArgumentException("Unknown constant: $name")
                }
            }
            // Parenthesized expression
            if (pos < input.length && input[pos] == '(') {
                pos++
                val result = parseExpression()
                if (pos < input.length && input[pos] == ')') pos++
                else throw IllegalArgumentException("Missing closing parenthesis")
                return result
            }
            return parseNumber()
        }

        private fun applyFunction(name: String, arg: Double): Double = when (name) {
            "sin"   -> Math.sin(Math.toRadians(arg))
            "cos"   -> Math.cos(Math.toRadians(arg))
            "tan"   -> Math.tan(Math.toRadians(arg))
            "asin"  -> Math.toDegrees(Math.asin(arg))
            "acos"  -> Math.toDegrees(Math.acos(arg))
            "atan"  -> Math.toDegrees(Math.atan(arg))
            "sinh"  -> Math.sinh(arg)
            "cosh"  -> Math.cosh(arg)
            "tanh"  -> Math.tanh(arg)
            "asinh" -> Math.log(arg + Math.sqrt(arg * arg + 1.0))
            "acosh" -> Math.log(arg + Math.sqrt(arg * arg - 1.0))
            "atanh" -> 0.5 * Math.log((1.0 + arg) / (1.0 - arg))
            "log"   -> Math.log10(arg)
            "ln"    -> Math.log(arg)
            "log2"  -> Math.log(arg) / Math.log(2.0)
            "abs"   -> Math.abs(arg)
            "cbrt"  -> Math.cbrt(arg)
            else    -> throw IllegalArgumentException("Unknown function: $name")
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (pos == start) throw IllegalArgumentException("Expected number at position $pos")
            return input.substring(start, pos).toDouble()
        }
    }
}
