package com.acalc.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.acalc.data.CalculationEntity
import com.acalc.domain.ExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import kotlin.math.floor

data class CalculatorState(
    val expression: String = "",
    val result: String = "",
    val isError: Boolean = false
)

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
    private val evaluator = ExpressionEvaluator()

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state

    private val _history = MutableStateFlow(loadHistory())
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
        val live = tryComputeFormatted()
        _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
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
        if (lastChar in "+-×÷") {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
        _state.value = _state.value.copy(expression = expression, result = "", isError = false)
    }

    fun onDecimal() {
        val currentToken = currentToken()
        if (currentToken.contains(".")) return
        if (expression.isEmpty() || expression.last() in "+-×÷") {
            expression += "0."
        } else {
            expression += "."
        }
        resultShown = false
        val live = tryComputeFormatted()
        _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
    }

    fun onClear() {
        if (expression.isNotEmpty() && !resultShown) {
            tryComputeFormatted()?.let { saveHistory(expression, it) }
        }
        expression = ""
        resultShown = false
        _state.value = CalculatorState()
    }

    fun onBackspace() {
        if (expression.isEmpty()) return
        expression = expression.dropLast(1)
        resultShown = false
        val live = tryComputeFormatted()
        _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
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

    fun onAdvanced(key: String) {
        if (resultShown) {
            // Function calls, √, (, constants (π φ e) → start fresh; ^ continues from result
            val startsFresh = key == "√" || key == "(" || key.first().isLetter() ||
                              key == "π" || key == "φ"
            if (startsFresh) {
                expression = key
                resultShown = false
                val live = tryComputeFormatted()
                _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
                return
            }
            resultShown = false
        }
        expression += key
        val live = tryComputeFormatted()
        _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
    }

    // Smart ( ) — adds ( if parens are balanced, ) if there's an unclosed one
    fun onParen() {
        if (resultShown) {
            expression = "("
            resultShown = false
            _state.value = _state.value.copy(expression = expression, result = "", isError = false)
            return
        }
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        val toAdd = if (open > close) ")" else "("
        expression += toAdd
        val live = tryComputeFormatted()
        _state.value = _state.value.copy(expression = expression, result = live ?: "", isError = false)
    }

    fun onTabLeave() {
        if (expression.isNotEmpty() && !resultShown) {
            tryComputeFormatted()?.let { saveHistory(expression, it) }
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
        prefs.edit().remove("calculator_history").apply()
    }

    // MARK: — Helpers

    private fun saveHistory(expr: String, result: String) {
        val updated = listOf(CalculationEntity(expression = expr, result = result)) + _history.value
        _history.value = updated
        prefs.edit().putString("calculator_history", Json.encodeToString(updated)).apply()
    }

    private fun loadHistory(): List<CalculationEntity> {
        val json = prefs.getString("calculator_history", null) ?: return emptyList()
        return try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
    }

    private fun currentToken(): String {
        val lastOpIndex = expression.indexOfLast { it in "+-×÷" }
        return if (lastOpIndex == -1) expression else expression.substring(lastOpIndex + 1)
    }

    private fun tryComputeFormatted(): String? {
        val result = compute() ?: return null
        return formatResult(result)
    }

    private fun compute(): Double? {
        val trimmed = expression.trimEnd { it in "+-×÷" }
        if (trimmed.isEmpty()) return null
        val sanitized = trimmed.replace("×", "*").replace("÷", "/")
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
