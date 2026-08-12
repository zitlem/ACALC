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

/** UI tests for the keyway calculator's ASME B17.1 / ISO 773 lookups and derived depths. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class KeywayCalculatorTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { KeywayCalculator() }
        }
    }

    // ── Inch lookup (ASME B17.1) ──

    @Test
    fun `inch is the default standard`() {
        compose.assertShows("Shaft diameter (in)")
    }

    @Test
    fun `a 1 inch shaft looks up a quarter inch key`() {
        compose.enterText("Shaft diameter", "1")
        compose.assertShows("ASME B17.1 standard for this shaft:")
        compose.assertShows("0.2500") // key width and height
        compose.assertShows("0.1250") // depth in shaft and hub
    }

    @Test
    fun `a small shaft picks the smallest key in the table`() {
        compose.enterText("Shaft diameter", "0.25")
        // 3/32 wide, 3/64 deep
        compose.assertShows("0.0938")
        compose.assertShows("0.0469")
    }

    @Test
    fun `a large shaft picks a larger key`() {
        compose.enterText("Shaft diameter", "3")
        // 3/4 wide, 3/8 deep
        compose.assertShows("0.7500")
        compose.assertShows("0.3750")
    }

    @Test
    fun `the table boundary is exclusive`() {
        // 0.4375 is the upper bound of the first row, so it must fall into the second row (1/8)
        compose.enterText("Shaft diameter", "0.4375")
        compose.assertShows("0.1250")
        compose.assertDoesNotShow("0.0938")
    }

    @Test
    fun `an oversized shaft reports being out of range`() {
        compose.enterText("Shaft diameter", "10")
        compose.assertShows("Shaft diameter outside ASME B17.1 range")
    }

    @Test
    fun `a zero shaft shows neither a lookup nor an error`() {
        compose.enterText("Shaft diameter", "0")
        compose.assertDoesNotShow("Shaft diameter outside ASME B17.1 range")
        compose.assertDoesNotShow("ASME B17.1 standard for this shaft:")
    }

    // ── Metric lookup (ISO 773) ──

    @Test
    fun `metric mode switches to the ISO 773 table`() {
        compose.tap("Metric (ISO 773)")
        compose.assertShows("Shaft diameter (mm)")
        compose.enterText("Shaft diameter", "20")
        compose.assertShows("ISO 773 standard for this shaft:")
        compose.assertShows("6.00") // key width
        compose.assertShows("3.50") // depth in shaft
        compose.assertShows("2.80") // depth in hub
    }

    @Test
    fun `metric keys above 22mm are not square`() {
        compose.tap("Metric (ISO 773)")
        compose.enterText("Shaft diameter", "25")
        compose.assertShows("8.00") // width
        compose.assertShows("7.00") // height differs from width
    }

    @Test
    fun `a metric shaft at or below 6mm is out of range`() {
        compose.tap("Metric (ISO 773)")
        compose.enterText("Shaft diameter", "5")
        compose.assertShows("Shaft diameter outside ISO 773 range")
    }

    @Test
    fun `an oversized metric shaft is out of range`() {
        compose.tap("Metric (ISO 773)")
        compose.enterText("Shaft diameter", "200")
        compose.assertShows("Shaft diameter outside ISO 773 range")
    }

    @Test
    fun `switching standards clears the shaft diameter`() {
        compose.enterText("Shaft diameter", "1")
        compose.assertShows("ASME B17.1 standard for this shaft:")
        compose.tap("Metric (ISO 773)")
        compose.assertDoesNotShow("ISO 773 standard for this shaft:")
    }

    // ── Derived values from user-entered key dimensions ──

    @Test
    fun `key height splits evenly between shaft and hub`() {
        compose.enterText("Shaft diameter", "1")
        compose.enterText("Key Height", "0.25")
        compose.assertShows("Depth in Shaft (in)")
        compose.assertShows("Depth in Hub (in)")
        compose.assertShows("0.1250")
    }

    @Test
    fun `shaft plus key is the radius plus the protruding half of the key`() {
        compose.enterText("Shaft diameter", "1")
        compose.enterText("Key Height", "0.25")
        // 1/2 + (0.25 − 0.125) = 0.625
        compose.assertShows("Shaft + Key (in)")
        compose.assertShows("0.6250")
    }

    @Test
    fun `hub OD to keyway is only shown once a hub OD is entered`() {
        compose.enterText("Shaft diameter", "1")
        compose.enterText("Key Height", "0.25")
        compose.assertDoesNotShow("Hub OD to Keyway (in)")

        compose.enterText("Hub OD", "2")
        compose.assertShows("Hub OD to Keyway (in)")
        // (2 − 1)/2 + 0.125 = 0.625
        compose.assertShows("0.6250")
    }

    @Test
    fun `derived values need both a shaft diameter and a key height`() {
        compose.enterText("Key Height", "0.25")
        compose.assertDoesNotShow("Depth in Shaft (in)")

        compose.enterText("Shaft diameter", "1")
        compose.assertShows("Depth in Shaft (in)")
    }

    @Test
    fun `key width is editable independently of the lookup`() {
        compose.enterText("Shaft diameter", "1")
        compose.enterText("Key Width", "0.3")
        // The standard width tile still reports the table value
        compose.assertShows("0.2500")
    }

    @Test
    fun `derived values still compute for an out-of-range shaft`() {
        compose.enterText("Shaft diameter", "10")
        compose.enterText("Key Height", "1")
        compose.assertShows("Shaft diameter outside ASME B17.1 range")
        compose.assertShows("Depth in Shaft (in)")
        compose.assertShows("0.5000")
    }

    @Test
    fun `metric derived values use two decimal places`() {
        compose.tap("Metric (ISO 773)")
        compose.enterText("Shaft diameter", "20")
        compose.enterText("Key Height", "6")
        compose.assertShows("Depth in Shaft (mm)")
        compose.assertShows("3.00")
    }
}
