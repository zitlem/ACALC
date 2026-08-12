package com.acalc.ui.screens.cnc

import androidx.compose.ui.test.junit4.createComposeRule
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.clearText
import com.acalc.ui.enterText
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI tests for the bolt-circle hole coordinate generator. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoltCircleCalculatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { BoltCircleCalculator() }
        }
    }

    private fun compute(holes: String, bcd: String, startAngle: String? = null) {
        compose.enterText("Number of Holes", holes)
        compose.enterText("Bolt Circle Diameter", bcd)
        if (startAngle != null) compose.enterText("Start Angle", startAngle)
        compose.tap("Compute")
    }

    // ── Coordinates ──

    @Test
    fun `four holes on a 10 inch circle land on the axes`() {
        compute(holes = "4", bcd = "10")
        // radius 5, starting at 0°, stepping 90°: (5,0) (0,5) (-5,0) (0,-5)
        compose.assertShows("5.0000")
        compose.assertShows("-5.0000")
        compose.assertShows("0.0000")
    }

    @Test
    fun `produces one table row per hole`() {
        compute(holes = "6", bcd = "4")
        listOf("1", "2", "3", "4", "5", "6").forEach { compose.assertShows(it) }
    }

    @Test
    fun `start angle rotates the pattern`() {
        compute(holes = "4", bcd = "10", startAngle = "90")
        // First hole moves from (5,0) to (0,5)
        compose.assertShows("5.0000")
        compose.assertShows("-5.0000")
    }

    @Test
    fun `two holes sit diametrically opposite`() {
        compute(holes = "2", bcd = "3")
        compose.assertShows("1.5000")
        compose.assertShows("-1.5000")
    }

    @Test
    fun `blank start angle is treated as zero`() {
        compose.enterText("Number of Holes", "4")
        compose.enterText("Bolt Circle Diameter", "10")
        compose.clearText("Start Angle")
        compose.tap("Compute")
        compose.assertShows("5.0000")
    }

    // ── Units ──

    @Test
    fun `inch is the default unit`() {
        compose.assertShows("Bolt Circle Diameter (BCD, in)")
    }

    @Test
    fun `metric mode relabels the diameter field and table headers`() {
        compose.tap("Metric")
        compose.assertShows("Bolt Circle Diameter (BCD, mm)")
        compute(holes = "4", bcd = "100")
        compose.assertShows("X (mm)")
        compose.assertShows("Y (mm)")
    }

    // ── Validation ──

    @Test
    fun `rejects fewer than two holes`() {
        compute(holes = "1", bcd = "10")
        compose.assertShows("Enter N ≥ 2")
    }

    @Test
    fun `rejects a missing hole count`() {
        compose.enterText("Bolt Circle Diameter", "10")
        compose.tap("Compute")
        compose.assertShows("Enter N ≥ 2")
    }

    @Test
    fun `rejects a zero diameter`() {
        compute(holes = "4", bcd = "0")
        compose.assertShows("Enter a positive BCD")
    }

    @Test
    fun `rejects a negative diameter`() {
        compute(holes = "4", bcd = "-5")
        compose.assertShows("Enter a positive BCD")
    }

    @Test
    fun `an invalid entry clears any previous results`() {
        compute(holes = "4", bcd = "10")
        compose.assertShows("5.0000")
        compute(holes = "1", bcd = "10")
        compose.assertDoesNotShow("5.0000")
    }

    @Test
    fun `no table is shown before computing`() {
        compose.assertDoesNotShow("Hole #")
    }
}
