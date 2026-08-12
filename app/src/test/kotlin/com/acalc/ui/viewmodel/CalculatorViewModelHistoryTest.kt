package com.acalc.ui.viewmodel

import com.acalc.data.CalculationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory [HistoryStorage] so persistence behaviour can be asserted without Android. */
private class FakeHistoryStorage(
    var items: List<CalculationEntity> = emptyList(),
    var expression: String = ""
) : HistoryStorage {
    var saveCount = 0
    override fun load(): List<CalculationEntity> = items
    override fun save(items: List<CalculationEntity>) {
        this.items = items
        saveCount++
    }
    override fun loadExpression(): String = expression
    override fun saveExpression(expr: String) { expression = expr }
}

class CalculatorViewModelHistoryTest {

    private lateinit var storage: FakeHistoryStorage
    private lateinit var vm: CalculatorViewModel

    @Before
    fun setUp() {
        storage = FakeHistoryStorage()
        vm = CalculatorViewModel(storage)
    }

    // ── History recording ──

    @Test
    fun `equals records the original expression and formatted result`() {
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onEquals()

        assertEquals(1, vm.history.value.size)
        assertEquals("2+3", vm.history.value.first().expression)
        assertEquals("5", vm.history.value.first().result)
    }

    @Test
    fun `newest history entry comes first`() {
        vm.onDigit("1")
        vm.onEquals()
        vm.onClear()
        vm.onDigit("2")
        vm.onEquals()

        assertEquals(listOf("2", "1"), vm.history.value.map { it.expression })
    }

    @Test
    fun `history is persisted through storage`() {
        vm.onDigit("7")
        vm.onEquals()
        assertEquals(1, storage.items.size)
        assertEquals("7", storage.items.first().expression)
    }

    @Test
    fun `history is restored from storage on construction`() {
        val seeded = FakeHistoryStorage(
            items = listOf(CalculationEntity(expression = "9*9", result = "81"))
        )
        val restored = CalculatorViewModel(seeded)
        assertEquals(1, restored.history.value.size)
        assertEquals("81", restored.history.value.first().result)
    }

    @Test
    fun `clearHistory empties both state and storage`() {
        vm.onDigit("4")
        vm.onEquals()
        vm.clearHistory()
        assertTrue(vm.history.value.isEmpty())
        assertTrue(storage.items.isEmpty())
    }

    @Test
    fun `failed evaluation is not recorded in history`() {
        vm.onDigit("5")
        vm.onOperator("/")
        vm.onDigit("0")
        vm.onEquals()
        assertTrue(vm.history.value.isEmpty())
    }

    @Test
    fun `clear records the in-progress calculation before wiping it`() {
        vm.onDigit("6")
        vm.onOperator("+")
        vm.onDigit("1")
        vm.onClear()
        assertEquals(1, vm.history.value.size)
        assertEquals("6+1", vm.history.value.first().expression)
        assertEquals("7", vm.history.value.first().result)
    }

    @Test
    fun `clear right after equals does not double-record`() {
        vm.onDigit("6")
        vm.onOperator("+")
        vm.onDigit("1")
        vm.onEquals()
        vm.onClear()
        assertEquals(1, vm.history.value.size)
    }

    @Test
    fun `onTabLeave records an unfinished calculation`() {
        vm.onDigit("8")
        vm.onOperator("×")
        vm.onDigit("2")
        vm.onTabLeave()
        assertEquals("8×2", vm.history.value.first().expression)
        assertEquals("16", vm.history.value.first().result)
    }

    @Test
    fun `onTabLeave after equals does not record again`() {
        vm.onDigit("8")
        vm.onEquals()
        vm.onTabLeave()
        assertEquals(1, vm.history.value.size)
    }

    @Test
    fun `percent records history`() {
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onPercent()
        assertEquals("50", vm.history.value.first().expression)
        assertEquals("0.5", vm.history.value.first().result)
    }

    // ── Expression persistence ──

    @Test
    fun `expression is persisted on every edit`() {
        vm.onDigit("1")
        vm.onDigit("2")
        assertEquals("12", storage.expression)
    }

    @Test
    fun `expression is restored on construction with a live result`() {
        val seeded = FakeHistoryStorage(expression = "3+4")
        val restored = CalculatorViewModel(seeded)
        assertEquals("3+4", restored.state.value.expression)
        assertEquals("7", restored.state.value.result)
    }

    @Test
    fun `restored empty expression yields an empty result`() {
        val restored = CalculatorViewModel(FakeHistoryStorage(expression = ""))
        assertEquals("", restored.state.value.expression)
        assertEquals("", restored.state.value.result)
    }

    // ── Cursor handling ──

    @Test
    fun `cursor advances as digits are typed`() {
        vm.onDigit("1")
        vm.onDigit("2")
        assertEquals(2, vm.state.value.cursorPos)
    }

    @Test
    fun `onCursorMoved is clamped to the expression bounds`() {
        vm.onDigit("1")
        vm.onCursorMoved(99)
        assertEquals(1, vm.state.value.cursorPos)
        vm.onCursorMoved(-5)
        assertEquals(0, vm.state.value.cursorPos)
    }

    @Test
    fun `digit is inserted at the cursor, not appended`() {
        vm.onDigit("1")
        vm.onDigit("3")
        vm.onCursorMoved(1)
        vm.onDigit("2")
        assertEquals("123", vm.state.value.expression)
        assertEquals(2, vm.state.value.cursorPos)
    }

    @Test
    fun `backspace deletes the character before the cursor`() {
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        vm.onCursorMoved(2)
        vm.onBackspace()
        assertEquals("13", vm.state.value.expression)
        assertEquals(1, vm.state.value.cursorPos)
    }

    @Test
    fun `operator replaces the operator immediately before the cursor`() {
        vm.onDigit("5")
        vm.onOperator("+")
        vm.onDigit("2")
        vm.onCursorMoved(2)
        vm.onOperator("×")
        assertEquals("5×2", vm.state.value.expression)
    }

    // ── Live result ──

    @Test
    fun `live result updates while typing without pressing equals`() {
        vm.onDigit("4")
        vm.onOperator("+")
        vm.onDigit("4")
        assertEquals("8", vm.state.value.result)
    }

    @Test
    fun `live result is blank right after an operator`() {
        vm.onDigit("4")
        vm.onOperator("+")
        assertEquals("", vm.state.value.result)
    }

    // ── Advanced keys ──

    @Test
    fun `advanced key inserts a function prefix`() {
        vm.onAdvanced("sin(")
        vm.onDigit("3")
        vm.onDigit("0")
        vm.onParen()
        assertEquals("sin(30)", vm.state.value.expression)
        assertEquals("0.5", vm.state.value.result)
    }

    @Test
    fun `pi constant evaluates`() {
        vm.onAdvanced("π")
        assertTrue(vm.state.value.result.startsWith("3.14"))
    }

    @Test
    fun `square root of a typed number`() {
        vm.onAdvanced("√")
        vm.onDigit("8")
        vm.onDigit("1")
        assertEquals("9", vm.state.value.result)
    }

    @Test
    fun `power operator continues from a shown result`() {
        vm.onDigit("2")
        vm.onEquals()
        vm.onAdvanced("^")
        vm.onDigit("3")
        vm.onEquals()
        assertEquals("8", vm.state.value.result)
    }

    @Test
    fun `function key after a result starts a fresh expression`() {
        vm.onDigit("9")
        vm.onEquals()
        vm.onAdvanced("√")
        assertEquals("√", vm.state.value.expression)
    }

    // ── Smart parentheses ──

    @Test
    fun `paren opens when balanced and closes when open`() {
        vm.onParen()
        assertEquals("(", vm.state.value.expression)
        vm.onDigit("2")
        vm.onParen()
        assertEquals("(2)", vm.state.value.expression)
    }

    @Test
    fun `parenthesised expression evaluates with correct precedence`() {
        vm.onParen()
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onParen()
        vm.onOperator("×")
        vm.onDigit("4")
        vm.onEquals()
        assertEquals("20", vm.state.value.result)
    }

    @Test
    fun `paren after a result starts a fresh expression`() {
        vm.onDigit("9")
        vm.onEquals()
        vm.onParen()
        assertEquals("(", vm.state.value.expression)
    }

    // ── Error recovery ──

    @Test
    fun `typing after an error clears the error flag`() {
        vm.onDigit("5")
        vm.onOperator("/")
        vm.onDigit("0")
        vm.onEquals()
        assertTrue(vm.state.value.isError)

        vm.onDigit("1")
        assertEquals(false, vm.state.value.isError)
    }

    @Test
    fun `equals on an empty expression is a no-op`() {
        vm.onEquals()
        assertEquals("", vm.state.value.expression)
        assertEquals("", vm.state.value.result)
        assertTrue(vm.history.value.isEmpty())
    }

    @Test
    fun `percent on an empty expression is a no-op`() {
        vm.onPercent()
        assertEquals("", vm.state.value.expression)
        assertEquals(0, storage.saveCount)
    }

    // ── Float noise after equals ──

    /**
     * KNOWN DEFECT — the answer is shown in two places and they disagree.
     *
     * `onEquals` formats the result line through [java.text.NumberFormat] capped at 10 fraction
     * digits, but rewrites the expression line from the raw `Double`. When the sum is not exactly
     * representable in binary (123 + 123.456 lands one ulp high at 246.45600000000002), the
     * result line reads "246.456" while the expression line reads "246.45600000000002".
     *
     * It is not only cosmetic: the noisy value is what the next operation continues from, so the
     * error is carried forward into every subsequent calculation.
     *
     * Fix: round the expression the same way the result is, e.g.
     * `BigDecimal.valueOf(result).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
     *  .toPlainString()` — the same `formatConverted` treatment ConverterViewModel already uses.
     * Applies to both `onEquals` and `onPercent`.
     */
    @Test
    fun `equals leaves float noise in the expression while the result is rounded`() {
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onDecimal()
        vm.onDigit("4"); vm.onDigit("5"); vm.onDigit("6")
        vm.onEquals()

        assertEquals("246.456", vm.state.value.result)
        // Should be "246.456" too, but the raw double leaks through:
        assertEquals("246.45600000000002", vm.state.value.expression)
    }

    /** The noisy expression is what the next operation builds on. */
    @Test
    fun `float noise is carried into the next calculation`() {
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onDecimal()
        vm.onDigit("4"); vm.onDigit("5"); vm.onDigit("6")
        vm.onEquals()
        vm.onOperator("+")
        vm.onDigit("1")

        assertEquals("246.45600000000002+1", vm.state.value.expression)
    }

    /** History records the clean, formatted result, so only the live display is affected. */
    @Test
    fun `history keeps the rounded result`() {
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onOperator("+")
        vm.onDigit("1"); vm.onDigit("2"); vm.onDigit("3")
        vm.onDecimal()
        vm.onDigit("4"); vm.onDigit("5"); vm.onDigit("6")
        vm.onEquals()

        assertEquals("123+123.456", vm.history.value.first().expression)
        assertEquals("246.456", vm.history.value.first().result)
    }

    @Test
    fun `equals collapses the expression to the numeric result`() {
        vm.onDigit("6")
        vm.onOperator("×")
        vm.onDigit("7")
        vm.onEquals()
        assertEquals("42", vm.state.value.expression)
        assertNotEquals("6×7", vm.state.value.expression)
    }
}
