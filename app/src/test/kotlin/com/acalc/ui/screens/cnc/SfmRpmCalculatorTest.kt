package com.acalc.ui.screens.cnc

import androidx.compose.ui.test.junit4.createComposeRule
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.assertShowsContaining
import com.acalc.ui.enterText
import com.acalc.ui.tap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests for the SFM/RPM cutting-speed solver. Its arithmetic lives inside the composable,
 * so driving the UI is the only way to cover it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class SfmRpmCalculatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { SfmRpmCalculator() }
        }
    }

    // ── Inch mode: SFM = RPM × π × D / 12 ──

    @Test
    fun `solves SFM from RPM and diameter`() {
        compose.enterText("RPM", "1000")
        compose.enterText("Diameter (in)", "1")
        compose.tap("Solve")
        // 1000 × π × 1 / 12 = 261.799
        compose.assertShows("261.799")
    }

    @Test
    fun `solves RPM from SFM and diameter`() {
        compose.enterText("SFM (Surface ft/min)", "100")
        compose.enterText("Diameter (in)", "1")
        compose.tap("Solve")
        // 12 × 100 / (π × 1) = 381.972
        compose.assertShows("381.972")
    }

    @Test
    fun `solves diameter from SFM and RPM`() {
        compose.enterText("SFM (Surface ft/min)", "100")
        compose.enterText("RPM", "1000")
        compose.tap("Solve")
        // 12 × 100 / (π × 1000) = 0.382
        compose.assertShows("0.382")
    }

    @Test
    fun `marks the field it solved`() {
        compose.enterText("RPM", "1000")
        compose.enterText("Diameter (in)", "1")
        compose.tap("Solve")
        compose.assertShows("solved")
    }

    // ── Metric mode: SMM = RPM × π × D / 1000 ──

    @Test
    fun `metric mode relabels the surface speed and diameter fields`() {
        compose.tap("Metric")
        compose.assertShows("SMM (Surface m/min)")
        compose.assertShows("Diameter (mm)")
    }

    @Test
    fun `solves surface metres per minute in metric mode`() {
        compose.tap("Metric")
        compose.enterText("RPM", "1000")
        compose.enterText("Diameter (mm)", "100")
        compose.tap("Solve")
        // 1000 × π × 100 / 1000 = 314.159
        compose.assertShows("314.159")
    }

    @Test
    fun `switching unit system clears the fields`() {
        compose.enterText("RPM", "1234")
        compose.tap("Metric")
        compose.assertDoesNotShow("1234")
    }

    // ── Validation ──

    @Test
    fun `requires exactly two values`() {
        compose.tap("Solve")
        compose.assertShows("Enter exactly 2 values, leave the third blank")
    }

    @Test
    fun `rejects all three values being filled in`() {
        compose.enterText("SFM (Surface ft/min)", "100")
        compose.enterText("RPM", "1000")
        compose.enterText("Diameter (in)", "1")
        compose.tap("Solve")
        compose.assertShows("Enter exactly 2 values, leave the third blank")
    }

    @Test
    fun `reports a divide by zero when the diameter is zero`() {
        compose.enterText("RPM", "1000")
        compose.enterText("Diameter (in)", "0")
        compose.tap("Solve")
        compose.assertShows("Cannot solve: divide by zero")
    }

    @Test
    fun `reports a divide by zero when solving diameter at zero RPM`() {
        compose.enterText("SFM (Surface ft/min)", "100")
        compose.enterText("RPM", "0")
        compose.tap("Solve")
        compose.assertShows("Cannot solve: divide by zero")
    }

    @Test
    fun `editing a field clears the previous error`() {
        compose.tap("Solve")
        compose.assertShows("Enter exactly 2 values, leave the third blank")
        compose.enterText("RPM", "500")
        compose.assertDoesNotShow("Enter exactly 2 values, leave the third blank")
    }

    @Test
    fun `non-numeric input is treated as blank`() {
        compose.enterText("RPM", "abc")
        compose.enterText("Diameter (in)", "1")
        compose.tap("Solve")
        compose.assertShowsContaining("Enter exactly 2 values")
    }
}
