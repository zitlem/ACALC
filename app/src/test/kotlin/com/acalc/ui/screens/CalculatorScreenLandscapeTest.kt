package com.acalc.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.acalc.ui.AcalcTheme
import com.acalc.ui.LANDSCAPE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.clearAppPreferences
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The calculator has a separate landscape layout (display beside the keypad instead of above it).
 * It is a whole second `if (isLandscape)` branch, so it needs its own pass over the behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = LANDSCAPE_QUALIFIERS)
class CalculatorScreenLandscapeTest {

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

    @Test
    fun `the whole keypad is present in landscape`() {
        listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "=",
               "C", "( )", "%", "÷", "×", "-", "+", "•••", "⌫")
            .forEach { compose.assertShows(it) }
    }

    @Test
    fun `arithmetic works in landscape`() {
        tapAll("1", "2", "+", "3", "4", "=")
        compose.assertShows("46")
    }

    @Test
    fun `the live result updates in landscape`() {
        tapAll("1", "2", "+", "3", "4")
        compose.assertShows("12+34")
        compose.assertShows("46")
    }

    @Test
    fun `the expression and result agree in landscape`() {
        tapAll("1", "2", "3", "+", "1", "2", "3", ".", "4", "5", "6", "=")
        compose.assertShows("246.456")
        compose.assertDoesNotShow("246.45600000000002")
    }

    @Test
    fun `clear works in landscape`() {
        tapAll("1", "2", "3")
        compose.assertShows("123")
        compose.tap("C")
        compose.assertDoesNotShow("123")
    }

    @Test
    fun `errors are shown in landscape`() {
        tapAll("5", "÷", "0", "=")
        compose.assertShows("Error")
    }

    @Test
    fun `the scientific sheet opens in landscape`() {
        compose.tap("•••")
        compose.assertShows("Scientific functions")
        compose.tap("√")
        tapAll("8", "1")
        compose.assertShows("9")
    }

    @Test
    fun `history is reachable in landscape`() {
        tapAll("1", "2", "+", "3", "4", "=")
        compose.onNodeWithContentDescription("History").performClick()
        compose.assertShows("12+34")
        compose.assertShows("= 46")
    }

    @Test
    fun `backspace works in landscape`() {
        tapAll("1", "2", "3")
        compose.tap("⌫")
        compose.assertShows("12")
        compose.assertDoesNotShow("123")
    }
}
