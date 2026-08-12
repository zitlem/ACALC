package com.acalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the scientific half of [ExpressionEvaluator]: constants, powers, roots and the
 * named function table. Plain arithmetic lives in [ExpressionEvaluatorTest].
 */
class ExpressionEvaluatorScientificTest {

    private val evaluator = ExpressionEvaluator()
    private val delta = 1e-9

    // --- Constants ---

    @Test
    fun `pi constant`() {
        assertEquals(Math.PI, evaluator.evaluate("π")!!, delta)
    }

    @Test
    fun `e constant`() {
        assertEquals(Math.E, evaluator.evaluate("e")!!, delta)
    }

    @Test
    fun `golden ratio constant`() {
        assertEquals((1.0 + Math.sqrt(5.0)) / 2.0, evaluator.evaluate("φ")!!, delta)
    }

    @Test
    fun `constant participates in arithmetic`() {
        assertEquals(2 * Math.PI, evaluator.evaluate("2*π")!!, delta)
    }

    @Test
    fun `unknown constant returns null`() {
        assertNull(evaluator.evaluate("foo"))
    }

    // --- Powers ---

    @Test
    fun `power operator`() {
        assertEquals(1024.0, evaluator.evaluate("2^10")!!, delta)
    }

    @Test
    fun `power binds tighter than multiplication`() {
        assertEquals(18.0, evaluator.evaluate("2*3^2")!!, delta)
    }

    @Test
    fun `power of negative exponent`() {
        assertEquals(0.25, evaluator.evaluate("2^-2")!!, delta)
    }

    /** Documents a current limitation: `a^b^c` is not accepted (no chained exponent). */
    @Test
    fun `chained exponent is unsupported and returns null`() {
        assertNull(evaluator.evaluate("2^3^2"))
    }

    // --- Roots ---

    @Test
    fun `square root prefix`() {
        assertEquals(3.0, evaluator.evaluate("√9")!!, delta)
    }

    @Test
    fun `square root of parenthesised expression`() {
        assertEquals(5.0, evaluator.evaluate("√(9+16)")!!, delta)
    }

    @Test
    fun `square root of negative returns null`() {
        assertNull(evaluator.evaluate("√-4"))
    }

    @Test
    fun `cube root function`() {
        assertEquals(3.0, evaluator.evaluate("cbrt(27)")!!, delta)
    }

    // --- Trigonometry (arguments in degrees) ---

    @Test
    fun `sin of 30 degrees is one half`() {
        assertEquals(0.5, evaluator.evaluate("sin(30)")!!, delta)
    }

    @Test
    fun `cos of 60 degrees is one half`() {
        assertEquals(0.5, evaluator.evaluate("cos(60)")!!, delta)
    }

    @Test
    fun `tan of 45 degrees is one`() {
        assertEquals(1.0, evaluator.evaluate("tan(45)")!!, delta)
    }

    @Test
    fun `asin returns degrees`() {
        assertEquals(30.0, evaluator.evaluate("asin(0.5)")!!, 1e-9)
    }

    @Test
    fun `acos returns degrees`() {
        assertEquals(60.0, evaluator.evaluate("acos(0.5)")!!, 1e-9)
    }

    @Test
    fun `atan returns degrees`() {
        assertEquals(45.0, evaluator.evaluate("atan(1)")!!, 1e-9)
    }

    // --- Hyperbolic ---

    @Test
    fun `sinh of zero is zero`() {
        assertEquals(0.0, evaluator.evaluate("sinh(0)")!!, delta)
    }

    @Test
    fun `cosh of zero is one`() {
        assertEquals(1.0, evaluator.evaluate("cosh(0)")!!, delta)
    }

    @Test
    fun `tanh of zero is zero`() {
        assertEquals(0.0, evaluator.evaluate("tanh(0)")!!, delta)
    }

    @Test
    fun `asinh is the inverse of sinh`() {
        assertEquals(1.0, evaluator.evaluate("asinh(${Math.sinh(1.0)})")!!, 1e-9)
    }

    @Test
    fun `acosh of one is zero`() {
        assertEquals(0.0, evaluator.evaluate("acosh(1)")!!, delta)
    }

    @Test
    fun `atanh of zero is zero`() {
        assertEquals(0.0, evaluator.evaluate("atanh(0)")!!, delta)
    }

    // --- Logarithms ---

    @Test
    fun `log is base ten`() {
        assertEquals(2.0, evaluator.evaluate("log(100)")!!, delta)
    }

    @Test
    fun `ln is natural log`() {
        assertEquals(1.0, evaluator.evaluate("ln(e)")!!, delta)
    }

    /** Regression: function names may contain digits, so the log₂ key can evaluate. */
    @Test
    fun `log2 of eight is three`() {
        assertEquals(3.0, evaluator.evaluate("log2(8)")!!, delta)
    }

    @Test
    fun `log2 of one is zero`() {
        assertEquals(0.0, evaluator.evaluate("log2(1)")!!, delta)
    }

    @Test
    fun `log2 composes with arithmetic`() {
        assertEquals(20.0, evaluator.evaluate("log2(1024)*2")!!, delta)
    }

    /** Digits in a name must not turn an unknown name into a silent success. */
    @Test
    fun `an unknown name containing digits still returns null`() {
        assertNull(evaluator.evaluate("log3(8)"))
        assertNull(evaluator.evaluate("e2"))
    }

    @Test
    fun `log of zero is infinite and returns null`() {
        assertNull(evaluator.evaluate("log(0)"))
    }

    @Test
    fun `ln of negative is NaN and returns null`() {
        assertNull(evaluator.evaluate("ln(-1)"))
    }

    // --- Misc functions ---

    @Test
    fun `abs of negative`() {
        assertEquals(5.0, evaluator.evaluate("abs(-5)")!!, delta)
    }

    @Test
    fun `unknown function returns null`() {
        assertNull(evaluator.evaluate("frobnicate(2)"))
    }

    @Test
    fun `missing closing parenthesis after function returns null`() {
        assertNull(evaluator.evaluate("sin(30"))
    }

    // --- Composition ---

    @Test
    fun `nested functions`() {
        assertEquals(1.0, evaluator.evaluate("abs(sin(90))")!!, delta)
    }

    @Test
    fun `functions combine with arithmetic and constants`() {
        // √16 + 2^3 - log(10) = 4 + 8 - 1
        assertEquals(11.0, evaluator.evaluate("√16+2^3-log(10)")!!, delta)
    }
}
