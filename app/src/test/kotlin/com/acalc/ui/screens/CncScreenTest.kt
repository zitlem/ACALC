package com.acalc.ui.screens

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.assertDoesNotShow
import com.acalc.ui.assertShows
import com.acalc.ui.clearAppPreferences
import com.acalc.ui.tap
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI tests for the CNC screen's sub-tab bar and its remembered selection. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CncScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
    }

    private fun launch() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { CncScreen() }
        }
    }

    private fun prefs() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)

    private fun setStartSubTab(index: Int) {
        prefs().edit().putInt("cnc_last_tab", index).commit()
    }

    // ── Sub-tabs ──

    @Test
    fun `all five sub-tabs are shown`() {
        launch()
        listOf("Triangle", "SFM/RPM", "Thread", "Bolt Circle", "Keyway")
            .forEach { compose.assertShows(it) }
    }

    @Test
    fun `the triangle solver is the default sub-tab`() {
        launch()
        compose.assertShows("Right Triangle")
        compose.assertShows("Hyp. c")
    }

    @Test
    fun `the SFM sub-tab shows the speed solver`() {
        launch()
        compose.tap("SFM/RPM")
        compose.assertShows("SFM (Surface ft/min)")
        compose.assertShows("Solve")
        compose.assertDoesNotShow("Right Triangle")
    }

    @Test
    fun `the thread sub-tab shows the thread calculator`() {
        launch()
        compose.tap("Thread")
        compose.assertShows("Calculate")
        compose.assertShows("Chart")
        compose.assertShows("Major Diameter (in)")
    }

    @Test
    fun `the bolt circle sub-tab shows the hole generator`() {
        launch()
        compose.tap("Bolt Circle")
        compose.assertShows("Number of Holes (N ≥ 2)")
        compose.assertShows("Compute")
    }

    @Test
    fun `the keyway sub-tab shows the keyway calculator`() {
        launch()
        compose.tap("Keyway")
        compose.assertShows("Shaft diameter (in)")
        compose.assertShows("Inch (ASME B17.1)")
    }

    @Test
    fun `sub-tabs can be switched back and forth`() {
        launch()
        compose.tap("Keyway")
        compose.assertShows("Shaft diameter (in)")
        compose.tap("Triangle")
        compose.assertShows("Hyp. c")
        compose.assertDoesNotShow("Shaft diameter (in)")
    }

    // ── Remembered selection ──

    @Test
    fun `choosing a sub-tab records it`() {
        launch()
        compose.tap("Bolt Circle")
        assertEquals(3, prefs().getInt("cnc_last_tab", -1))
        compose.tap("Keyway")
        assertEquals(4, prefs().getInt("cnc_last_tab", -1))
    }

    @Test
    fun `it opens on the remembered sub-tab`() {
        setStartSubTab(4)
        launch()
        compose.assertShows("Shaft diameter (in)")
    }

    @Test
    fun `an out-of-range remembered index is clamped`() {
        setStartSubTab(99)
        launch()
        compose.assertShows("Keyway")
        compose.assertShows("Shaft diameter (in)")
    }

    @Test
    fun `a negative remembered index is clamped to the first sub-tab`() {
        setStartSubTab(-5)
        launch()
        compose.assertShows("Right Triangle")
    }
}
