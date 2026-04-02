package com.acalc.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculatorViewModelTest {

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel()
    }

    // CALC-01 — Basic arithmetic

    @Test
    fun `CALC-01 simple addition`() {
        viewModel.onDigit("2")
        viewModel.onOperator("+")
        viewModel.onDigit("3")
        viewModel.onEquals()
        assertEquals("5", viewModel.state.value.result)
    }

    @Test
    fun `CALC-01 operator precedence multiplication over addition`() {
        viewModel.onDigit("2")
        viewModel.onOperator("+")
        viewModel.onDigit("3")
        viewModel.onOperator("x")
        viewModel.onDigit("4")
        viewModel.onEquals()
        assertEquals("14", viewModel.state.value.result)
    }

    @Test
    fun `CALC-01 division`() {
        viewModel.onDigit("1")
        viewModel.onDigit("0")
        viewModel.onOperator("/")
        viewModel.onDigit("2")
        viewModel.onEquals()
        assertEquals("5", viewModel.state.value.result)
    }

    // CALC-02 — Decimal input

    @Test
    fun `CALC-02 decimal input builds expression`() {
        viewModel.onDigit("1")
        viewModel.onDecimal()
        viewModel.onDigit("5")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `CALC-02 double decimal guard prevents second decimal in same token`() {
        viewModel.onDigit("1")
        viewModel.onDecimal()
        viewModel.onDecimal()
        assertEquals("1.", viewModel.state.value.expression)
    }

    @Test
    fun `CALC-02 decimal allowed in new token after operator`() {
        viewModel.onDigit("1")
        viewModel.onDecimal()
        viewModel.onDigit("5")
        viewModel.onOperator("+")
        viewModel.onDigit("3")
        viewModel.onDecimal()
        viewModel.onDigit("2")
        assertEquals("1.5+3.2", viewModel.state.value.expression)
    }

    // CALC-03 — Clear and backspace

    @Test
    fun `CALC-03 clear resets all state`() {
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onClear()
        assertEquals("", viewModel.state.value.expression)
        assertEquals("", viewModel.state.value.result)
    }

    @Test
    fun `CALC-03 backspace removes last character`() {
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onBackspace()
        assertEquals("1", viewModel.state.value.expression)
    }

    @Test
    fun `CALC-03 backspace on empty expression does not crash`() {
        viewModel.onBackspace()
        assertEquals("", viewModel.state.value.expression)
    }

    // CALC-04 — Percent

    @Test
    fun `CALC-04 percent evaluates as divided by 100`() {
        viewModel.onDigit("5")
        viewModel.onDigit("0")
        viewModel.onPercent()
        assertEquals("0.5", viewModel.state.value.result)
    }

    // CALC-05 — Expression display

    @Test
    fun `CALC-05 expression shows full in-progress input`() {
        viewModel.onDigit("1")
        viewModel.onOperator("+")
        viewModel.onDigit("2")
        assertEquals("1+2", viewModel.state.value.expression)
    }

    // CALC-06 — Division by zero error

    @Test
    fun `CALC-06 division by zero produces Error and isError flag`() {
        viewModel.onDigit("5")
        viewModel.onOperator("/")
        viewModel.onDigit("0")
        viewModel.onEquals()
        assertEquals("Error", viewModel.state.value.result)
        assertTrue(viewModel.state.value.isError)
    }

    // CALC-07 — Thousands separators

    @Test
    fun `CALC-07 large results have thousands separators`() {
        // Type "1000+2000" and evaluate
        viewModel.onDigit("1")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.onOperator("+")
        viewModel.onDigit("2")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.onDigit("0")
        viewModel.onEquals()
        assertTrue(
            "Expected thousands separator in result, got: ${viewModel.state.value.result}",
            viewModel.state.value.result.contains(",")
        )
    }

    // Additional edge cases

    @Test
    fun `operator replacement - new operator replaces trailing operator`() {
        viewModel.onDigit("5")
        viewModel.onOperator("+")
        viewModel.onOperator("-")
        assertEquals("5-", viewModel.state.value.expression)
    }

    @Test
    fun `trailing operator trimmed on equals`() {
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onOperator("+")
        viewModel.onEquals()
        assertEquals("12", viewModel.state.value.result)
    }

    @Test
    fun `result continuation - after equals digit starts fresh`() {
        viewModel.onDigit("3")
        viewModel.onOperator("+")
        viewModel.onDigit("2")
        viewModel.onEquals()
        assertEquals("5", viewModel.state.value.result)
        // After equals, digit starts fresh
        viewModel.onDigit("5")
        assertEquals("5", viewModel.state.value.expression)
    }

    @Test
    fun `result continuation - after equals operator continues from result`() {
        viewModel.onDigit("3")
        viewModel.onOperator("+")
        viewModel.onDigit("2")
        viewModel.onEquals()
        assertEquals("5", viewModel.state.value.result)
        // After equals, operator continues from result
        viewModel.onOperator("+")
        viewModel.onDigit("3")
        viewModel.onEquals()
        assertEquals("8", viewModel.state.value.result)
    }

    @Test
    fun `leading operator on empty expression is ignored except unary minus`() {
        viewModel.onOperator("+")
        assertEquals("", viewModel.state.value.expression)

        viewModel.onOperator("-")
        assertEquals("-", viewModel.state.value.expression)
    }

    @Test
    fun `backspace on single minus clears to empty`() {
        viewModel.onOperator("-")
        assertEquals("-", viewModel.state.value.expression)
        viewModel.onBackspace()
        assertEquals("", viewModel.state.value.expression)
    }

    @Test
    fun `whole number result displays without trailing zero`() {
        viewModel.onDigit("4")
        viewModel.onOperator("/")
        viewModel.onDigit("2")
        viewModel.onEquals()
        assertEquals("2", viewModel.state.value.result)
        // Should NOT be "2.0"
        assertFalse(viewModel.state.value.result.contains("."))
    }

    @Test
    fun `x operator is substituted to star for evaluator`() {
        // 3 x 4 = 12
        viewModel.onDigit("3")
        viewModel.onOperator("x")
        viewModel.onDigit("4")
        viewModel.onEquals()
        assertEquals("12", viewModel.state.value.result)
    }
}
