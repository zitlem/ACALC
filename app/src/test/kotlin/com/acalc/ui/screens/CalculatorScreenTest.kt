package com.acalc.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.assertShowsContaining
import com.acalc.ui.clearAppPreferences
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** End-to-end UI tests for the calculator screen: keypad, live display, history and sheets. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class CalculatorScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { CalculatorScreen() }
        }
    }

    private fun tapAll(vararg labels: String) = labels.forEach { compose.tap(it) }

    // ── Keypad and display ──

    @Test
    fun `the keypad renders every key`() {
        listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "=", "C", "( )", "%", "÷", "×", "-", "+")
            .forEach { compose.assertShows(it) }
    }

    @Test
    fun `digits build up the expression`() {
        tapAll("1", "2", "3")
        compose.assertShows("123")
    }

    @Test
    fun `the result updates live while typing`() {
        tapAll("1", "2", "+", "3", "4")
        compose.assertShows("12+34")
        compose.assertShows("46")
    }

    @Test
    fun `equals collapses the expression to the result`() {
        tapAll("1", "2", "+", "3", "4", "=")
        compose.assertDoesNotShow("12+34")
        compose.assertShows("46")
    }

    @Test
    fun `multiplication takes precedence over addition`() {
        tapAll("1", "0", "+", "2", "×", "3", "=")
        compose.assertShows("16")
    }

    @Test
    fun `the decimal key inserts a decimal point`() {
        tapAll("1", ".", "5", "×", "2", "=")
        compose.assertShows("3")
    }

    @Test
    fun `clear empties the display`() {
        tapAll("1", "2", "3")
        compose.assertShows("123")
        compose.tap("C")
        compose.assertDoesNotShow("123")
    }

    @Test
    fun `backspace removes the last character`() {
        tapAll("1", "2", "3")
        compose.tap("⌫")
        compose.assertShows("12")
        compose.assertDoesNotShow("123")
    }

    /**
     * KNOWN DEFECT, seen from the UI: after `=` the answer is on screen twice — once in the
     * expression field, once on the result line — and for a sum that is not exactly representable
     * in binary the two disagree. See CalculatorViewModelHistoryTest for the cause and the fix.
     */
    @Test
    fun `the expression and result lines disagree on a repeating decimal`() {
        tapAll("1", "2", "3", "+", "1", "2", "3", ".", "4", "5", "6", "=")
        compose.assertShows("246.456")               // result line — correct
        compose.assertShows("246.45600000000002")    // expression field — float noise
    }

    @Test
    fun `percent divides by one hundred`() {
        tapAll("5", "0", "%")
        compose.assertShows("0.5")
    }

    @Test
    fun `dividing by zero shows an error`() {
        tapAll("5", "÷", "0", "=")
        compose.assertShows("Error")
    }

    @Test
    fun `large results are grouped with thousands separators`() {
        tapAll("9", "9", "9", "×", "9", "9", "9", "=")
        compose.assertShows("998,001")
    }

    // ── Smart parentheses ──

    @Test
    fun `the paren key opens then closes`() {
        compose.tap("( )")
        compose.assertShows("(")
        tapAll("2", "+", "3")
        compose.tap("( )")
        compose.assertShows("(2+3)")
    }

    @Test
    fun `parentheses change the evaluation order`() {
        compose.tap("( )")
        tapAll("2", "+", "3")
        compose.tap("( )")
        tapAll("×", "4", "=")
        compose.assertShows("20")
    }

    // ── Scientific functions sheet ──

    @Test
    fun `the advanced sheet lists the scientific functions`() {
        compose.tap("•••")
        compose.assertShows("Scientific functions")
        compose.assertShows("sin")
        compose.assertShows("cos")
        compose.assertShows("log")
        compose.assertShows("√")
        compose.assertShows("π")
    }

    @Test
    fun `picking a function inserts it and closes the sheet`() {
        compose.tap("•••")
        compose.tap("√")
        compose.assertDoesNotShow("Scientific functions")
        tapAll("8", "1")
        compose.assertShows("√81")
        compose.assertShows("9")
    }

    @Test
    fun `a trigonometric function evaluates in degrees`() {
        compose.tap("•••")
        compose.tap("sin")
        tapAll("3", "0")
        compose.tap("( )")
        compose.assertShows("0.5")
    }

    @Test
    fun `the pi constant evaluates`() {
        compose.tap("•••")
        compose.tap("π")
        compose.assertShowsContaining("3.14")
    }

    // ── History ──

    @Test
    fun `history starts empty`() {
        compose.onNodeWithContentDescription("History").performClick()
        compose.assertShows("History")
        compose.assertShows("No history yet")
    }

    @Test
    fun `a completed calculation is added to history`() {
        tapAll("1", "2", "+", "3", "4", "=")
        compose.onNodeWithContentDescription("History").performClick()
        compose.assertShows("12+34")
        compose.assertShows("= 46")
    }

    @Test
    fun `clear all empties the history`() {
        tapAll("1", "2", "+", "3", "4", "=")
        compose.onNodeWithContentDescription("History").performClick()
        compose.assertShows("12+34")
        compose.tap("Clear all")
        compose.assertShows("No history yet")
    }

    @Test
    fun `a failed calculation is not recorded`() {
        tapAll("5", "÷", "0", "=")
        compose.onNodeWithContentDescription("History").performClick()
        compose.assertShows("No history yet")
    }
}
