package com.acalc.ui.screens

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.acalc.ui.AcalcTheme
import com.acalc.ui.LANDSCAPE_QUALIFIERS
import com.acalc.ui.SHORT_PHONE_QUALIFIERS
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

private fun ComposeContentTestRule.tapAll(vararg labels: String) = labels.forEach { tap(it) }

/**
 * The converter has a separate landscape layout — rows and hint on the left, numpad on the right —
 * which is a whole second branch of the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = LANDSCAPE_QUALIFIERS)
class ConverterScreenLandscapeTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ConverterScreen() }
        }
    }

    @Test
    fun `rows and numpad are both present in landscape`() {
        compose.assertShows("Millimeter")
        compose.assertShows("Centimeter")
        compose.assertShows("Inch")
        compose.assertShows("Foot")
        listOf("C", "±", "÷", "7", "8", "9", "×", "0", ".", "=")
            .forEach { compose.assertShows(it) }
    }

    @Test
    fun `live conversion works in landscape`() {
        compose.tapAll("2", "5", ".", "4")
        compose.assertShows("25.4")
        compose.assertShows("2.54")
        compose.assertShows("0.0833333333")
    }

    @Test
    fun `the conversion hint is shown in landscape`() {
        compose.assertShowsContaining("Millimeter → Centimeter")
    }

    @Test
    fun `categories can be switched in landscape`() {
        compose.tap("Temp")
        compose.assertShows("Celsius")
        compose.tapAll("1", "0", "0")
        compose.assertShows("212")
        compose.assertShows("373.15")
    }

    @Test
    fun `the unit picker opens in landscape`() {
        compose.tap("Millimeter")
        compose.assertShows("Select Unit")
        compose.tap("Meter")
        compose.assertDoesNotShow("Select Unit")
        compose.assertShows("Meter")
    }

    @Test
    fun `expression entry previews its result in landscape`() {
        compose.tapAll("2", "+", "3")
        compose.assertShows("2+3")
        compose.assertShowsContaining("= 5")
    }

    @Test
    fun `the focus key moves between rows in landscape`() {
        compose.tap("1")
        compose.onNodeWithContentDescription("Focus next row").performClick()
        compose.tap("C")
        compose.tap("2")
        compose.assertShows("20")
    }
}

/**
 * On a short screen the converter rows drop below the 52dp threshold and switch to a compact
 * expression layout. The single-line mode is distinguishable because it renders the result with a
 * leading space (" = 5") where the taller modes render "= 5".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = SHORT_PHONE_QUALIFIERS)
class ConverterScreenShortScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ConverterScreen() }
        }
    }

    @Test
    fun `the rows still convert on a short screen`() {
        compose.tapAll("2", "5", ".", "4")
        compose.assertShows("25.4")
        compose.assertShows("2.54")
    }

    @Test
    fun `an expression still previews its result on a short screen`() {
        compose.tapAll("2", "+", "3")
        compose.assertShows("2+3")
        // Either the scaled two-line ("= 5") or the single-line (" = 5") compact mode.
        compose.assertShowsContaining("= 5")
    }

    @Test
    fun `committing an expression still converts on a short screen`() {
        compose.tapAll("2", "5", "+", "0", ".", "4")
        compose.tap("=")
        compose.assertShows("25.4")
        compose.assertShows("2.54")
    }
}
