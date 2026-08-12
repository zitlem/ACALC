package com.acalc.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertShows
import com.acalc.ui.clearAppPreferences
import com.acalc.ui.tap
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The converter value fields used to be only as tall as a single line of text — a 36px strip
 * floating in the middle of a ~123px row — so roughly 70% of each row was dead. Tapping outside
 * that strip activated the row in the view model but never focused the field, so no cursor
 * appeared and the tap looked like it had been ignored.
 *
 * These tests pin the hit target to the full height of the row and to Android's 48dp minimum.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class ConverterRowTouchTargetTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ConverterScreen() }
        }
    }

    private fun fieldBounds() =
        compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }

    @Test
    fun `every value field meets the minimum touch target`() {
        val minPx = with(compose.density) { 48.dp.toPx() }
        fieldBounds().forEachIndexed { i, b ->
            assertTrue(
                "row $i value field is only ${b.height}px tall, below the ${minPx}px minimum",
                b.height >= minPx
            )
        }
    }

    @Test
    fun `the value field fills the row instead of floating in its middle`() {
        val bounds = fieldBounds()
        assertTrue("expected at least two rows", bounds.size >= 2)

        // Row pitch: the vertical distance between consecutive fields.
        val pitch = bounds[1].top - bounds[0].top
        bounds.forEachIndexed { i, b ->
            assertTrue(
                "row $i covers only ${b.height}px of its ${pitch}px row",
                b.height >= pitch * 0.7f
            )
        }
    }

    @Test
    fun `tapping near the top of a row focuses its field`() {
        val field = compose.onAllNodes(hasSetTextAction())[2]
        field.performTouchInput { click(Offset(centerX, top + 2f)) }
        field.assertIsFocused()
    }

    @Test
    fun `tapping near the bottom of a row focuses its field`() {
        val field = compose.onAllNodes(hasSetTextAction())[2]
        field.performTouchInput { click(Offset(centerX, bottom - 2f)) }
        field.assertIsFocused()
    }

    @Test
    fun `tapping the top of a row still routes numpad input to it`() {
        compose.tap("5")   // millimetre row
        val field = compose.onAllNodes(hasSetTextAction())[2]
        field.performTouchInput { click(Offset(centerX, top + 2f)) }
        compose.tap("C")
        compose.tap("2")
        // The inch row is now the source: 2 in = 50.8 mm
        compose.assertShows("50.8")
    }

    @Test
    fun `the last row is tappable across its whole height`() {
        val field = compose.onAllNodes(hasSetTextAction()).onLast()
        field.performTouchInput { click(Offset(centerX, top + 2f)) }
        field.assertIsFocused()
    }
}
