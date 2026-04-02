package com.acalc.ui.viewmodel

import com.acalc.domain.ExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import kotlin.math.floor

data class CalculatorState(
    val expression: String = "",
    val result: String = "",
    val isError: Boolean = false
)

class CalculatorViewModel {

    private val evaluator = ExpressionEvaluator()

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state

    // Raw expression string (no formatting). Kept in sync with _state.value.expression.
    private var expression = ""

    // True after a successful onEquals() or onPercent() — tracks that the result is currently shown.
    private var resultShown = false

    // MARK: — Input handlers

    fun onDigit(digit: String) {
        if (resultShown) {
            // Start fresh after a result
            expression = ""
            resultShown = false
        }
        expression += digit
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onOperator(op: String) {
        if (expression.isEmpty()) {
            // Only allow unary minus on empty expression
            if (op == "-") {
                expression = "-"
                _state.value = _state.value.copy(expression = expression, result = "", isError = false)
            }
            return
        }

        if (resultShown) {
            // Continue from result — use unformatted expression (already set by onEquals)
            resultShown = false
        }

        val lastChar = expression.last()
        if (lastChar in "+-x/") {
            // Replace the trailing operator
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onDecimal() {
        val currentToken = currentToken()
        if (currentToken.contains(".")) {
            // Guard: already a decimal in this token — do nothing
            return
        }
        if (expression.isEmpty() || expression.last() in "+-x/") {
            // Start a new token with "0."
            expression += "0."
        } else {
            expression += "."
        }
        resultShown = false
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onClear() {
        expression = ""
        resultShown = false
        _state.value = CalculatorState()
    }

    fun onBackspace() {
        if (expression.isEmpty()) return
        expression = expression.dropLast(1)
        resultShown = false
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onPercent() {
        val token = currentToken()
        if (token.isEmpty()) return
        // Append "/100" to the expression and evaluate
        expression += "/100"
        val result = compute()
        if (result != null) {
            val formatted = formatResult(result)
            // Store unformatted result back as expression for continued ops
            expression = result.toBigDecimal().stripTrailingZeros().toPlainString()
            _state.value = _state.value.copy(
                expression = expression,
                result = formatted,
                isError = false
            )
            resultShown = true
        } else {
            _state.value = _state.value.copy(result = "Error", isError = true)
            resultShown = false
        }
    }

    fun onEquals() {
        if (expression.isEmpty()) return
        val result = compute()
        if (result != null) {
            val formatted = formatResult(result)
            // Store unformatted result back as expression for continued ops
            expression = result.toBigDecimal().stripTrailingZeros().toPlainString()
            _state.value = _state.value.copy(
                expression = expression,
                result = formatted,
                isError = false
            )
            resultShown = true
        } else {
            _state.value = _state.value.copy(result = "Error", isError = true)
            resultShown = false
        }
    }

    // MARK: — Helpers

    /**
     * Returns the current (last) number token in the expression.
     * Scans backwards from the end to find the last operator boundary.
     */
    private fun currentToken(): String {
        val lastOpIndex = expression.indexOfLast { it in "+-x/" }
        return if (lastOpIndex == -1) expression else expression.substring(lastOpIndex + 1)
    }

    /**
     * Sanitizes the expression and evaluates it. Returns null on error.
     */
    private fun compute(): Double? {
        // Trim trailing operators
        val trimmed = expression.trimEnd { it in "+-x/" }
        if (trimmed.isEmpty()) return null
        // Substitute display 'x' with evaluator '*'
        val sanitized = trimmed.replace("x", "*")
        return evaluator.evaluate(sanitized)
    }

    /**
     * Formats a Double result for display:
     * - Whole numbers: integer format with thousands separators (e.g. 3,000)
     * - Decimals: up to 10 fraction digits with grouping (e.g. 1,234.5678)
     */
    private fun formatResult(value: Double): String {
        return if (value == floor(value) && !value.isInfinite() && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
            NumberFormat.getIntegerInstance().format(value.toLong())
        } else {
            val fmt = NumberFormat.getNumberInstance()
            fmt.maximumFractionDigits = 10
            fmt.isGroupingUsed = true
            fmt.format(value)
        }
    }
}
