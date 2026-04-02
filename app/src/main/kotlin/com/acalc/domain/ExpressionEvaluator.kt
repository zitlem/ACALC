package com.acalc.domain

class ExpressionEvaluator {

    fun evaluate(input: String): Double? {
        val cleaned = input.trim().replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return null
        return try {
            val parser = Parser(cleaned)
            val result = parser.parseExpression()
            // Verify all input was consumed — leftover chars means malformed input
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

        // term = factor (('*' | '/') factor)*
        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
                val op = input[pos++]
                val right = parseFactor()
                if (op == '/' && right == 0.0) {
                    throw ArithmeticException("Division by zero")
                }
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        // factor = '-' factor | '(' expression ')' | NUMBER
        private fun parseFactor(): Double {
            // Unary minus
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseFactor()
            }
            // Parenthesized expression
            if (pos < input.length && input[pos] == '(') {
                pos++ // consume '('
                val result = parseExpression()
                if (pos < input.length && input[pos] == ')') {
                    pos++ // consume ')'
                } else {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                return result
            }
            // Number
            return parseNumber()
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            if (pos == start) {
                throw IllegalArgumentException("Expected number at position $pos")
            }
            return input.substring(start, pos).toDouble()
        }
    }
}
