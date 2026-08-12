package com.acalc.ui.screens.cnc

import androidx.compose.ui.test.junit4.createComposeRule
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.enterText
import com.acalc.ui.screens.TriangleCalculatorContent
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI tests for the triangle solver — right-triangle and general (any-triangle) modes. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TriangleCalculatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { TriangleCalculatorContent() }
        }
    }

    // ── Right triangle ──

    @Test
    fun `right triangle mode is selected by default`() {
        compose.assertShows("Hyp. c")
        compose.assertDoesNotShow("Angle C°")
    }

    @Test
    fun `solves the 3-4-5 triangle from two legs`() {
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.tap("Calculate")
        compose.assertShows("5")      // hypotenuse
        compose.assertShows("36.87")  // angle A
        compose.assertShows("53.13")  // angle B
        compose.assertShows("90")     // angle C
    }

    @Test
    fun `reports area and perimeter`() {
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.tap("Calculate")
        compose.assertShows("Area")
        compose.assertShows("Perimeter")
        compose.assertShows("6")   // ½ × 3 × 4
        compose.assertShows("12")  // 3 + 4 + 5
    }

    @Test
    fun `solves a leg from the hypotenuse`() {
        compose.enterText("Side a", "3")
        compose.enterText("Hyp. c", "5")
        compose.tap("Calculate")
        compose.assertShows("4")
    }

    @Test
    fun `solves from the hypotenuse and an angle`() {
        compose.enterText("Hyp. c", "10")
        compose.enterText("Angle A°", "30")
        compose.tap("Calculate")
        compose.assertShows("5")     // a = 10 sin30
        compose.assertShows("8.66")  // b = 10 cos30
        compose.assertShows("30")
    }

    @Test
    fun `solves from a leg and an angle`() {
        compose.enterText("Side b", "4")
        compose.enterText("Angle B°", "45")
        compose.tap("Calculate")
        compose.assertShows("4")   // a = b / tan45
        compose.assertShows("45")
    }

    @Test
    fun `rejects an angle of 90 degrees or more`() {
        compose.enterText("Side a", "3")
        compose.enterText("Angle A°", "90")
        compose.tap("Calculate")
        compose.assertShows("Enter any 2 values (sides a/b/c or angles A°/B°)")
    }

    @Test
    fun `rejects a hypotenuse shorter than a leg`() {
        compose.enterText("Side a", "5")
        compose.enterText("Hyp. c", "3")
        compose.tap("Calculate")
        compose.assertShows("Enter any 2 values (sides a/b/c or angles A°/B°)")
    }

    @Test
    fun `rejects a single value`() {
        compose.enterText("Side a", "3")
        compose.tap("Calculate")
        compose.assertShows("Enter any 2 values (sides a/b/c or angles A°/B°)")
    }

    @Test
    fun `rejects an empty form`() {
        compose.tap("Calculate")
        compose.assertShows("Enter any 2 values (sides a/b/c or angles A°/B°)")
    }

    @Test
    fun `no results are shown before calculating`() {
        compose.assertDoesNotShow("Results")
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.assertDoesNotShow("Results")
        compose.tap("Calculate")
        compose.assertShows("Results")
    }

    // ── Any triangle ──

    @Test
    fun `any triangle mode exposes a third side and angle`() {
        compose.tap("Any Triangle")
        compose.assertShows("Side c")
        compose.assertShows("Angle C°")
        compose.assertDoesNotShow("Hyp. c")
    }

    @Test
    fun `solves side-angle-side`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.enterText("Angle C°", "90")
        compose.tap("Calculate")
        compose.assertShows("5")
        compose.assertShows("36.87")
        compose.assertShows("53.13")
    }

    @Test
    fun `solves side-side-side`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.enterText("Side c", "5")
        compose.tap("Calculate")
        compose.assertShows("90")
        compose.assertShows("6")  // area
    }

    @Test
    fun `solves an equilateral triangle from three sides`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "10")
        compose.enterText("Side b", "10")
        compose.enterText("Side c", "10")
        compose.tap("Calculate")
        compose.assertShows("60")
        compose.assertShows("30")  // perimeter
    }

    @Test
    fun `solves angle-side-angle using the law of sines`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "10")
        compose.enterText("Angle A°", "30")
        compose.enterText("Angle B°", "60")
        compose.tap("Calculate")
        compose.assertShows("90")     // the third angle
        compose.assertShows("17.32")  // b = 10 sin60 / sin30
        compose.assertShows("20")     // c = 10 / sin30
    }

    @Test
    fun `rejects a set of angles with no side`() {
        compose.tap("Any Triangle")
        compose.enterText("Angle A°", "60")
        compose.enterText("Angle B°", "60")
        compose.tap("Calculate")
        compose.assertShows("Not enough data or invalid values")
    }

    @Test
    fun `rejects a non-positive side`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "0")
        compose.enterText("Side b", "4")
        compose.enterText("Angle C°", "90")
        compose.tap("Calculate")
        compose.assertShows("Not enough data or invalid values")
    }

    @Test
    fun `rejects an angle of 180 degrees or more`() {
        compose.tap("Any Triangle")
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.enterText("Angle C°", "180")
        compose.tap("Calculate")
        compose.assertShows("Not enough data or invalid values")
    }

    // ── Mode switching ──

    @Test
    fun `switching modes clears the inputs and results`() {
        compose.enterText("Side a", "3")
        compose.enterText("Side b", "4")
        compose.tap("Calculate")
        compose.assertShows("Results")

        compose.tap("Any Triangle")
        compose.assertDoesNotShow("Results")

        compose.tap("Right Triangle")
        compose.assertDoesNotShow("Results")
    }
}
