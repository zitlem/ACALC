package com.acalc.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
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
    val isError: Boolean = false,
    val cursorPos: Int = 0
)

/** Persistence abstraction so CalculatorViewModel can be unit-tested without an Application. */
interface HistoryStorage {
    fun load(): List<CalculationEntity>
    fun save(items: List<CalculationEntity>)
    fun loadExpression(): String
    fun saveExpression(expr: String)
}

private class SharedPrefsHistoryStorage(prefs: SharedPreferences) : HistoryStorage {
    private val sp = prefs
    override fun load(): List<CalculationEntity> {
        val json = sp.getString("calculator_history", null) ?: return emptyList()
        return try { Json.decodeFromString(json) } catch (_: Exception) { emptyList() }
    }
    override fun save(items: List<CalculationEntity>) {
        sp.edit().putString("calculator_history", Json.encodeToString(items)).apply()
    }
    override fun loadExpression(): String =
        sp.getString("calculator_expression", null) ?: ""
    override fun saveExpression(expr: String) {
        sp.edit().putString("calculator_expression", expr).apply()
    }
}

private object NoOpHistoryStorage : HistoryStorage {
    override fun load(): List<CalculationEntity> = emptyList()
    override fun save(items: List<CalculationEntity>) {}
    override fun loadExpression(): String = ""
    override fun saveExpression(expr: String) {}
}

class CalculatorViewModel(
    private val historyStorage: HistoryStorage = NoOpHistoryStorage
) : ViewModel() {

    companion object {
        /** Factory for use in the Activity/Composable with real SharedPreferences. */
        fun create(app: Application): CalculatorViewModel =
            CalculatorViewModel(
                SharedPrefsHistoryStorage(
                    app.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
                )
            )
    }

    private val evaluator = ExpressionEvaluator()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<CalculationEntity>> = _history

    // Raw expression string (no formatting). Kept in sync with _state.value.expression.
    private var expression = historyStorage.loadExpression()

    // Cursor position within expression (index of insertion point, 0..expression.length).
    private var cursorPos = expression.length

    // True after a successful onEquals() or onPercent() — tracks that the result is currently shown.
    private var resultShown = false

    private val _state = MutableStateFlow(run {
        val live = if (expression.isNotEmpty()) tryComputeFormatted() else null
        CalculatorState(expression = expression, result = live ?: "", cursorPos = cursorPos)
    })
    val state: StateFlow<CalculatorState> = _state

    /** Emits new state and persists the expression. */
    private fun emitState(s: CalculatorState) {
        _state.value = s
        historyStorage.saveExpression(s.expression)
    }

    /** Called by the UI when the user taps to reposition the cursor. */
    fun onCursorMoved(newPos: Int) {
        cursorPos = newPos.coerceIn(0, expression.length)
        // Only update cursorPos in state — no expression/result change.
        _state.value = _state.value.copy(cursorPos = cursorPos)
    }

    // MARK: — Input handlers

    fun onDigit(digit: String) {
        if (resultShown) {
            expression = ""
            cursorPos = 0
            resultShown = false
        }
        expression = expression.insert(cursorPos, digit)
        cursorPos += digit.length
        val live = tryComputeFormatted()
        emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
    }

    fun onOperator(op: String) {
        if (expression.isEmpty()) {
            if (op == "-") {
                expression = "-"
                cursorPos = 1
                emitState(_state.value.copy(expression = expression, result = "", isError = false, cursorPos = cursorPos))
            }
            return
        }

        if (resultShown) {
            resultShown = false
        }

        // If the character immediately before the cursor is an operator, replace it.
        val charBefore = if (cursorPos > 0) expression[cursorPos - 1] else null
        if (charBefore != null && charBefore in "+-×÷") {
            expression = expression.removeRange(cursorPos - 1, cursorPos).insert(cursorPos - 1, op)
            // cursorPos stays the same (replaced one char with one char)
        } else {
            expression = expression.insert(cursorPos, op)
            cursorPos += op.length
        }
        emitState(_state.value.copy(expression = expression, result = "", isError = false, cursorPos = cursorPos))
    }

    fun onDecimal() {
        val tokenAtCursor = currentTokenAt(cursorPos)
        if (tokenAtCursor.contains(".")) return
        val insertion: String
        val charBefore = if (cursorPos > 0) expression[cursorPos - 1] else null
        if (expression.isEmpty() || charBefore == null || charBefore in "+-×÷") {
            insertion = "0."
        } else {
            insertion = "."
        }
        expression = expression.insert(cursorPos, insertion)
        cursorPos += insertion.length
        resultShown = false
        val live = tryComputeFormatted()
        emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
    }

    fun onClear() {
        if (expression.isNotEmpty() && !resultShown) {
            tryComputeFormatted()?.let { saveHistory(expression, it) }
        }
        expression = ""
        cursorPos = 0
        resultShown = false
        emitState(CalculatorState())
    }

    fun onBackspace() {
        if (cursorPos == 0) return
        expression = expression.removeRange(cursorPos - 1, cursorPos)
        cursorPos -= 1
        resultShown = false
        val live = tryComputeFormatted()
        emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
    }

    fun onPercent() {
        val token = currentToken()
        if (token.isEmpty()) return
        val originalExpression = expression
        expression += "/100"
        cursorPos = expression.length
        val result = compute()
        if (result != null) {
            val formatted = formatResult(result)
            expression = result.toBigDecimal().stripTrailingZeros().toPlainString()
            cursorPos = expression.length
            emitState(_state.value.copy(expression = expression, result = formatted, isError = false, cursorPos = cursorPos))
            resultShown = true
            saveHistory(originalExpression, formatted)
        } else {
            emitState(_state.value.copy(result = "Error", isError = true))
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
                cursorPos = key.length
                resultShown = false
                val live = tryComputeFormatted()
                emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
                return
            }
            resultShown = false
        }
        expression = expression.insert(cursorPos, key)
        cursorPos += key.length
        val live = tryComputeFormatted()
        emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
    }

    // Smart ( ) — adds ( if parens are balanced, ) if there's an unclosed one
    fun onParen() {
        if (resultShown) {
            expression = "("
            cursorPos = 1
            resultShown = false
            emitState(_state.value.copy(expression = expression, result = "", isError = false, cursorPos = cursorPos))
            return
        }
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        val toAdd = if (open > close) ")" else "("
        expression = expression.insert(cursorPos, toAdd)
        cursorPos += 1
        val live = tryComputeFormatted()
        emitState(_state.value.copy(expression = expression, result = live ?: "", isError = false, cursorPos = cursorPos))
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
            cursorPos = expression.length
            emitState(_state.value.copy(expression = expression, result = formatted, isError = false, cursorPos = cursorPos))
            resultShown = true
            saveHistory(originalExpression, formatted)
        } else {
            emitState(_state.value.copy(result = "Error", isError = true))
            resultShown = false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        historyStorage.save(emptyList())
    }

    // MARK: — Helpers

    private fun saveHistory(expr: String, result: String) {
        val updated = listOf(CalculationEntity(expression = expr, result = result)) + _history.value
        _history.value = updated
        historyStorage.save(updated)
    }

    private fun loadHistory(): List<CalculationEntity> = historyStorage.load()

    private fun currentToken(): String {
        val lastOpIndex = expression.indexOfLast { it in "+-×÷" }
        return if (lastOpIndex == -1) expression else expression.substring(lastOpIndex + 1)
    }

    /** Returns the numeric token in [expr] that contains position [pos] (used for decimal guard). */
    private fun currentTokenAt(pos: Int): String {
        val sub = expression.substring(0, pos)
        val lastOpIndex = sub.indexOfLast { it in "+-×÷" }
        return if (lastOpIndex == -1) sub else sub.substring(lastOpIndex + 1)
    }

    private fun tryComputeFormatted(): String? {
        val result = compute() ?: return null
        return formatResult(result)
    }

    private fun compute(): Double? {
        val trimmed = expression.trimEnd { it in "+-×÷" }
        if (trimmed.isEmpty()) return null
        val sanitized = trimmed.replace("×", "*").replace("÷", "/").replace("x", "*")
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

/** Inserts [s] into this string at position [index]. */
private fun String.insert(index: Int, s: String): String =
    substring(0, index) + s + substring(index)
