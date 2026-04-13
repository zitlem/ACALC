package com.acalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {

    private val evaluator = ExpressionEvaluator()
    private val delta = 1e-10

    // --- Basic arithmetic ---

    @Test
    fun addition() {
        assertEquals(7.0, evaluator.evaluate("3 + 4")!!, delta)
    }

    @Test
    fun subtraction() {
        assertEquals(7.0, evaluator.evaluate("10 - 3")!!, delta)
    }

    @Test
    fun multiplication() {
        assertEquals(42.0, evaluator.evaluate("6 * 7")!!, delta)
    }

    @Test
    fun division() {
        assertEquals(0.25, evaluator.evaluate("1/4")!!, delta)
    }

    // --- Operator precedence ---

    @Test
    fun `multiplication before addition`() {
        assertEquals(14.0, evaluator.evaluate("2 + 3 * 4")!!, delta)
    }

    @Test
    fun `multiplication before subtraction`() {
        assertEquals(4.0, evaluator.evaluate("10 - 2 * 3")!!, delta)
    }

    // --- Parentheses ---

    @Test
    fun `parentheses override precedence`() {
        assertEquals(20.0, evaluator.evaluate("(2 + 3) * 4")!!, delta)
    }

    @Test
    fun `nested parentheses`() {
        assertEquals(20.0, evaluator.evaluate("((2 + 3)) * 4")!!, delta)
    }

    // --- Unary minus ---

    @Test
    fun `unary minus`() {
        assertEquals(-3.0, evaluator.evaluate("-3")!!, delta)
    }

    @Test
    fun `unary minus in multiplication`() {
        assertEquals(-12.0, evaluator.evaluate("-3 * 4")!!, delta)
    }

    @Test
    fun `unary minus after operator`() {
        assertEquals(3.0, evaluator.evaluate("5 + -2")!!, delta)
    }

    // --- Decimal numbers ---

    @Test
    fun `decimal multiplication`() {
        assertEquals(6.28, evaluator.evaluate("3.14 * 2")!!, delta)
    }

    @Test
    fun `decimal addition`() {
        val result = evaluator.evaluate("0.1 + 0.2")
        assertNotNull(result)
        assertEquals(0.3, result!!, delta)
    }

    // --- Error handling ---

    @Test
    fun `division by zero returns null`() {
        assertNull(evaluator.evaluate("5 / 0"))
    }

    @Test
    fun `empty input returns null`() {
        assertNull(evaluator.evaluate(""))
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(evaluator.evaluate("abc"))
    }

    @Test
    fun `trailing operator returns null`() {
        assertNull(evaluator.evaluate("2 + "))
    }

    @Test
    fun `leading binary operator returns null`() {
        assertNull(evaluator.evaluate("* 3"))
    }

    @Test
    fun `consecutive operators returns null`() {
        assertNull(evaluator.evaluate("2 + + 3"))
    }

    // --- Leading-dot decimals ---

    @Test
    fun `leading dot decimal`() {
        assertEquals(0.2, evaluator.evaluate(".2")!!, delta)
    }

    @Test
    fun `leading dot decimal subtraction`() {
        assertEquals(0.08, evaluator.evaluate(".2-.12")!!, delta)
    }

    @Test
    fun `leading dot decimal addition`() {
        assertEquals(0.8, evaluator.evaluate(".5+.3")!!, delta)
    }

    @Test
    fun `integer minus leading dot decimal`() {
        assertEquals(0.93, evaluator.evaluate("1-.07")!!, delta)
    }

    // --- Complex expressions ---

    @Test
    fun `complex expression with multiple parentheses`() {
        assertEquals(21.0, evaluator.evaluate("(1 + 2) * (3 + 4)")!!, delta)
    }

    @Test
    fun `division with parenthesized denominator`() {
        assertEquals(10.0, evaluator.evaluate("100 / (2 * 5)")!!, delta)
    }
}
