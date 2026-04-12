package com.acalc.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acalc.data.CalculationEntity
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

class CalculatorViewModel : ViewModel() {

    private val evaluator = ExpressionEvaluator()

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state

    private val _history = MutableStateFlow<List<CalculationEntity>>(emptyList())
    val history: StateFlow<List<CalculationEntity>> = _history

    // Raw expression string (no formatting). Kept in sync with _state.value.expression.
    private var expression = ""

    // True after a successful onEquals() or onPercent() — tracks that the result is currently shown.
    private var resultShown = false

    // MARK: — Input handlers

    fun onDigit(digit: String) {
        if (resultShown) {
            expression = ""
            resultShown = false
        }
        expression += digit
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onOperator(op: String) {
        if (expression.isEmpty()) {
            if (op == "-") {
                expression = "-"
                _state.value = _state.value.copy(expression = expression, result = "", isError = false)
            }
            return
        }

        if (resultShown) {
            resultShown = false
        }

        val lastChar = expression.last()
        if (lastChar in "+-x/") {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onDecimal() {
        val currentToken = currentToken()
        if (currentToken.contains(".")) return
        if (expression.isEmpty() || expression.last() in "+-x/") {
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
        val originalExpression = expression
        expression += "/100"
        val result = compute()
        if (result != null) {
            val formatted = formatResult(result)
            expression = result.toBigDecimal().stripTrailingZeros().toPlainString()
            _state.value = _state.value.copy(expression = expression, result = formatted, isError = false)
            resultShown = true
            saveHistory(originalExpression, formatted)
        } else {
            _state.value = _state.value.copy(result = "Error", isError = true)
            resultShown = false
        }
    }

    fun onEquals() {
        if (expression.isEmpty()) return
        val originalExpression = expression
        val result = compute()
        if (result != null) {
            val formatted = formatResult(result)
            expression = result.toBigDecimal().stripTrailingZeros().toPlainString()
            _state.value = _state.value.copy(expression = expression, result = formatted, isError = false)
            resultShown = true
            saveHistory(originalExpression, formatted)
        } else {
            _state.value = _state.value.copy(result = "Error", isError = true)
            resultShown = false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    // MARK: — Helpers

    private fun saveHistory(expr: String, result: String) {
        _history.value = listOf(CalculationEntity(expression = expr, result = result)) + _history.value
    }

    private fun currentToken(): String {
        val lastOpIndex = expression.indexOfLast { it in "+-x/" }
        return if (lastOpIndex == -1) expression else expression.substring(lastOpIndex + 1)
    }

    private fun compute(): Double? {
        val trimmed = expression.trimEnd { it in "+-x/" }
        if (trimmed.isEmpty()) return null
        val sanitized = trimmed.replace("x", "*")
        return evaluator.evaluate(sanitized)
    }

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
