package com.acalc.ui.screens.cnc

import androidx.compose.ui.test.junit4.createComposeRule
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.enterText
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests for the thread calculator — both the Calculate sub-tab (pitch diameters, minor
 * diameter, best wire size, measurement over wires, tap drill) and the Chart sub-tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class ThreadCalculatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { ThreadCalculator() }
        }
    }

    /** A 1/2-13 UNC thread — the worked example used across most of these tests. */
    private fun enterHalfInch13Tpi() {
        compose.enterText("Major Diameter", "0.5")
        compose.enterText("TPI", "13")
    }

    // ── Nothing until both inputs are present ──

    @Test
    fun `no results until a diameter and pitch are entered`() {
        compose.assertDoesNotShow("Class 2A")
        compose.enterText("Major Diameter", "0.5")
        compose.assertDoesNotShow("Class 2A")
    }

    @Test
    fun `a zero pitch produces no results`() {
        compose.enterText("Major Diameter", "0.5")
        compose.enterText("TPI", "0")
        compose.assertDoesNotShow("Class 2A")
    }

    // ── Inch, external ──

    @Test
    fun `external inch thread defaults to class 2A`() {
        enterHalfInch13Tpi()
        compose.assertShows("Class 2A")
    }

    @Test
    fun `minor diameter follows the UN formula`() {
        enterHalfInch13Tpi()
        // 0.5 − 1.2990/13 = 0.40008
        compose.assertShows("0.4001")
    }

    @Test
    fun `best wire size is 0 point 57735 over TPI`() {
        enterHalfInch13Tpi()
        // 0.57735 / 13 = 0.044412
        compose.assertShows("0.0444")
    }

    @Test
    fun `results are labelled in inches`() {
        enterHalfInch13Tpi()
        compose.assertShows("Minor Dia (in)")
        compose.assertShows("Wire Ø (in)")
        compose.assertShows("Meas. Over Wires (in)")
        compose.assertShows("Max PD (in)")
        compose.assertShows("Min PD (in)")
    }

    @Test
    fun `an external thread shows no tap drill`() {
        enterHalfInch13Tpi()
        compose.assertDoesNotShow("Tap Drill")
    }

    @Test
    fun `thread class chips switch the reported class`() {
        enterHalfInch13Tpi()
        compose.tap("3A")
        compose.assertShows("Class 3A")
        compose.tap("1A")
        compose.assertShows("Class 1A")
    }

    @Test
    fun `class 3A is tighter than class 1A`() {
        enterHalfInch13Tpi()
        compose.tap("3A")
        // 3A max PD is the basic pitch diameter: 0.5 − 0.6495/13 = 0.45004
        compose.assertShows("0.4500")
    }

    // ── Inch, internal ──

    @Test
    fun `switching to internal offers B classes and defaults to 2B`() {
        compose.tap("Internal")
        enterHalfInch13Tpi()
        compose.assertShows("Class 2B")
    }

    @Test
    fun `an internal thread reports a tap drill`() {
        compose.tap("Internal")
        enterHalfInch13Tpi()
        compose.assertShows("Tap Drill")
        // 0.5 − 0.9743/13 = 0.42505
        compose.assertShows("0.4251")
    }

    @Test
    fun `a tap drill matching a standard size is named`() {
        compose.tap("Internal")
        compose.enterText("Major Diameter", "0.25")
        compose.enterText("TPI", "20")
        // 0.25 − 0.9743/20 = 0.20129, within 0.002 of the #7 drill at 0.2010
        compose.assertShows("0.2013 (#7)")
    }

    @Test
    fun `internal minimum pitch diameter is the basic pitch diameter`() {
        compose.tap("Internal")
        enterHalfInch13Tpi()
        compose.assertShows("0.4500")
    }

    // ── Metric ──

    @Test
    fun `metric mode relabels the inputs and hides the inch classes`() {
        compose.tap("Metric (pitch)")
        compose.assertShows("Major Diameter (mm)")
        compose.assertShows("Pitch (mm)")
        compose.assertDoesNotShow("2A")
    }

    @Test
    fun `metric external thread is class 6g`() {
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        compose.assertShows("Class 6g")
    }

    @Test
    fun `metric internal thread is class 6H`() {
        compose.tap("Internal")
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        compose.assertShows("Class 6H")
    }

    @Test
    fun `metric minor diameter follows the ISO formula`() {
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        // 6 − 1.2990 × 1 = 4.7010
        compose.assertShows("4.7010")
    }

    @Test
    fun `metric best wire size is 0 point 57735 times the pitch`() {
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        compose.assertShows("0.5774")
    }

    @Test
    fun `metric internal tap drill is major diameter minus pitch`() {
        compose.tap("Internal")
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        compose.assertShows("5.000 mm")
    }

    @Test
    fun `switching unit system clears the inputs`() {
        enterHalfInch13Tpi()
        compose.tap("Metric (pitch)")
        compose.assertDoesNotShow("Class 2A")
        compose.assertDoesNotShow("Class 6g")
    }

    // ── Custom wire ──

    @Test
    fun `a custom wire diameter replaces the best wire size`() {
        enterHalfInch13Tpi()
        compose.tap("Custom Wire")
        compose.enterText("Wire Diameter", "0.05")
        compose.assertShows("0.0500")
        compose.assertDoesNotShow("0.0444")
    }

    @Test
    fun `the custom wire field only appears when custom wire is selected`() {
        enterHalfInch13Tpi()
        compose.assertDoesNotShow("Wire Diameter (in)")
        compose.tap("Custom Wire")
        compose.assertShows("Wire Diameter (in)")
    }

    @Test
    fun `an empty custom wire falls back to the best wire size`() {
        enterHalfInch13Tpi()
        compose.tap("Custom Wire")
        compose.assertShows("0.0444")
    }

    // ── Chart sub-tab ──

    @Test
    fun `the chart tab lists UNC sizes by default`() {
        compose.tap("Chart")
        compose.assertShows("1/4-20")
        compose.assertShows("1/2-13")
        compose.assertShows("Tap Drill")
    }

    @Test
    fun `the chart switches to UNF sizes`() {
        compose.tap("Chart")
        compose.tap("UNF")
        compose.assertShows("1/4-28")
        compose.assertDoesNotShow("1/4-20")
    }

    @Test
    fun `the chart switches to metric coarse sizes`() {
        compose.tap("Chart")
        compose.tap("Metric")
        compose.assertShows("M6")
        compose.assertShows("M12")
        compose.assertShows("Pitch (mm)")
        compose.assertShows("Tap Drill (mm)")
    }

    /**
     * Regression: the sub-tabs are separate call sites in a `when (subTab)`, so state remembered
     * inside a child would be dropped when it leaves the composition. Visiting the chart used to
     * wipe everything typed into Calculate.
     */
    @Test
    fun `returning from the chart restores the calculate inputs`() {
        enterHalfInch13Tpi()
        compose.assertShows("Class 2A")
        compose.tap("Chart")
        compose.tap("Calculate")
        compose.assertShows("Class 2A")
        compose.assertShows("0.4001")   // minor diameter, recomputed from the restored inputs
    }

    @Test
    fun `the chart remembers which table was selected`() {
        compose.tap("Chart")
        compose.tap("Metric")
        compose.assertShows("M6")
        compose.tap("Calculate")
        compose.tap("Chart")
        compose.assertShows("M6")
        compose.assertDoesNotShow("1/4-20")
    }

    @Test
    fun `internal and metric selections survive a trip to the chart`() {
        compose.tap("Internal")
        compose.tap("Metric (pitch)")
        compose.enterText("Major Diameter", "6")
        compose.enterText("Pitch (mm)", "1")
        compose.assertShows("Class 6H")

        compose.tap("Chart")
        compose.tap("Calculate")
        compose.assertShows("Class 6H")
        compose.assertShows("5.000 mm")   // tap drill, still in metric internal mode
    }
}
