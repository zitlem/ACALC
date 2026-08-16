package com.acalc.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
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
 * The highlighted row and the text caret used to be driven by two unrelated things — the view
 * model's `activeRowIndex` and whichever `BasicTextField` happened to hold Compose focus — with
 * nothing keeping them together. The caret would sit in one row while another was highlighted and
 * took the numpad input.
 *
 * Each test here pins one of the ways they came apart. Focus is the observable proxy for the
 * caret; "which row took the input" is the proxy for the highlight, since both are read off
 * `activeRowIndex`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class ConverterRowFocusSyncTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ConverterScreen() }
        }
    }

    /** Default LENGTH rows, in order: 0 mm, 1 cm, 2 inch, 3 foot. */
    private fun field(row: Int) = compose.onAllNodes(hasSetTextAction())[row]

    @Test
    fun `tapping a derived row past the end of its text still activates it`() {
        // Every derived row parks its cursor at end-of-text and the text is right-aligned, so a
        // tap at the right edge resolves to the offset already stored: no selection change, and
        // therefore no onValueChange. Focus is the only signal that the row was picked.
        compose.tap("5")   // 5 mm — the inch row now holds a derived value
        field(2).performTouchInput { click(Offset(right - 2f, centerY)) }

        field(2).assertIsFocused()

        compose.tap("C")
        compose.tap("2")
        compose.assertShows("50.8")   // 2 in = 50.8 mm, so the inch row took the input
    }

    @Test
    fun `the focus next key moves the caret with the highlight`() {
        compose.tap("1")
        compose.onNodeWithContentDescription("Focus next row").performClick()

        field(1).assertIsFocused()
    }

    @Test
    fun `the caret survives the switch into and out of expression mode`() {
        // Becoming an evaluable expression swaps the row into a different subtree, which disposes
        // the focused field; committing with = swaps it back.
        compose.tap("2")
        field(0).assertIsFocused()

        compose.tap("+")
        compose.tap("3")
        compose.assertShows("= 5")
        field(0).assertIsFocused()

        compose.tap("=")
        field(0).assertIsFocused()
    }

    @Test
    fun `choosing a unit moves the caret and the input to that row`() {
        compose.tap("5")        // 5 mm, entered on row 0
        compose.tap("Inch")     // activates row 2 and opens the picker
        compose.tap("Meter")    // row 2 becomes metres

        field(2).assertIsFocused()

        compose.tap("C")
        compose.tap("2")
        compose.assertShows("2000")   // 2 m = 2000 mm, so row 2 took the input
    }

    @Test
    fun `swapping a derived row's unit converts it rather than reinterpreting it`() {
        // Selecting a row must not make it the conversion source: 5 mm stays 5 mm.
        compose.tap("5")
        compose.tap("Inch")
        compose.tap("Meter")

        compose.assertShows("5")      // millimetre row, untouched
        compose.assertShows("0.005")  // the same quantity, now shown in metres
    }
}
