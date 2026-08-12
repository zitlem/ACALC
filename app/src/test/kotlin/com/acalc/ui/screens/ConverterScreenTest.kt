package com.acalc.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertDoesNotShowContaining
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

/** End-to-end UI tests for the live dual-display unit converter. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class ConverterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ConverterScreen() }
        }
    }

    private fun tapAll(vararg labels: String) = labels.forEach { compose.tap(it) }

    // ── Default layout ──

    @Test
    fun `length is the default category`() {
        compose.assertShows("Millimeter")
        compose.assertShows("Centimeter")
        compose.assertShows("Inch")
        compose.assertShows("Foot")
    }

    @Test
    fun `every category has a tab`() {
        listOf("Length", "Weight", "Volume", "Temp", "Area", "Speed", "Time",
               "Force", "Pressure", "Energy", "Power", "Angle", "Data")
            .forEach { compose.assertShows(it) }
    }

    @Test
    fun `the numpad renders every key`() {
        listOf("C", "±", "÷", "7", "8", "9", "×", "4", "5", "6", "-", "1", "2", "3", "+", "0", ".", "=")
            .forEach { compose.assertShows(it) }
        compose.onNodeWithContentDescription("Backspace").assertIsDisplayed()
        compose.onNodeWithContentDescription("Focus next row").assertIsDisplayed()
    }

    // ── Live conversion — the app's core feature ──

    @Test
    fun `typing millimetres converts every other row live`() {
        tapAll("2", "5", ".", "4")
        compose.assertShows("25.4")          // mm, as typed
        compose.assertShows("2.54")          // cm
        compose.assertShows("0.0833333333")  // foot
    }

    @Test
    fun `conversion updates on every keystroke`() {
        compose.tap("1")
        compose.assertShows("0.0393700787")  // 1 mm in inches
        compose.tap("0")
        compose.assertShows("0.3937007874")  // 10 mm in inches
    }

    @Test
    fun `clear empties every row`() {
        tapAll("2", "5", ".", "4")
        compose.assertShows("2.54")
        compose.tap("C")
        compose.assertDoesNotShow("2.54")
        compose.assertDoesNotShow("25.4")
    }

    @Test
    fun `backspace shortens the value and reconverts`() {
        tapAll("2", "5")
        compose.assertShows("2.5")           // 25 mm = 2.5 cm
        compose.onNodeWithContentDescription("Backspace").performClick()
        compose.assertShows("0.2")           // 2 mm = 0.2 cm
    }

    @Test
    fun `the sign key negates the value`() {
        tapAll("5", "±")
        compose.assertShows("-5")
        compose.assertShows("-0.5")          // cm
    }

    // ── Categories ──

    @Test
    fun `switching to temperature shows the temperature scales`() {
        compose.tap("Temp")
        compose.assertShows("Celsius")
        compose.assertShows("Fahrenheit")
        compose.assertShows("Kelvin")
    }

    @Test
    fun `100 celsius converts to 212 fahrenheit and 373 point 15 kelvin`() {
        compose.tap("Temp")
        tapAll("1", "0", "0")
        compose.assertShows("212")
        compose.assertShows("373.15")
    }

    @Test
    fun `switching to weight shows the weight units`() {
        compose.tap("Weight")
        compose.assertShows("Gram")
        compose.assertShows("Kilogram")
        compose.assertShows("Ounce")
        compose.assertShows("Pound")
    }

    @Test
    fun `1000 grams converts to pounds and ounces`() {
        compose.tap("Weight")
        tapAll("1", "0", "0", "0")
        compose.assertShows("1000")
        compose.assertShows("2.2046226218")   // pounds
        compose.assertShows("35.2739619496")  // ounces
    }

    @Test
    fun `each category keeps its own values`() {
        tapAll("2", "5")
        compose.tap("Weight")
        compose.assertDoesNotShow("25")
        compose.tap("Length")
        compose.assertShows("25")
    }

    // ── Unit picker ──

    @Test
    fun `tapping a unit opens the picker`() {
        compose.tap("Millimeter")
        compose.assertShows("Select Unit")
        compose.assertShows("Meter")
        compose.assertShows("Kilometer")
        compose.assertShows("Yard")
    }

    @Test
    fun `choosing a unit reconverts that row`() {
        tapAll("1", "0", "0", "0")   // 1000 mm
        compose.tap("Centimeter")
        compose.tap("Meter")
        compose.assertDoesNotShow("Select Unit")
        compose.assertShows("Meter")
        compose.assertShows("39.3700787402")  // 1000 mm in inches, unchanged by the swap
    }

    // ── Conversion hint ──

    @Test
    fun `the hint shows the rate in both directions`() {
        compose.assertShowsContaining("Millimeter → Centimeter")
        compose.assertShowsContaining("Centimeter → Millimeter")
    }

    @Test
    fun `temperature has no multiplicative hint`() {
        compose.tap("Temp")
        compose.assertDoesNotShowContaining("Celsius →")
    }

    // ── Expression entry ──

    @Test
    fun `an in-progress expression previews its result`() {
        tapAll("2", "+", "3")
        compose.assertShows("2+3")
        compose.assertShows("= 5")
    }

    @Test
    fun `equals commits the expression and converts it`() {
        tapAll("2", "5", "+", "0", ".", "4")
        compose.tap("=")
        compose.assertDoesNotShow("25+0.4")
        compose.assertShows("25.4")
        compose.assertShows("2.54")
    }

    @Test
    fun `multiplication in an expression is accepted`() {
        tapAll("1", "0", "×", "5")
        compose.tap("=")
        compose.assertShows("50")
        compose.assertShows("1.968503937")   // 50 mm in inches
    }

    // ── Focus movement ──

    @Test
    fun `the focus key moves input to the next row`() {
        compose.tap("1")
        compose.onNodeWithContentDescription("Focus next row").performClick()
        compose.tap("C")
        compose.tap("2")
        // The centimetre row now holds 2, so the millimetre row shows 20
        compose.assertShows("20")
    }
}
