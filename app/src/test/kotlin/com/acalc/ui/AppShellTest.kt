package com.acalc.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** UI tests for the top-level tab shell: Calculator / Converter / CNC and its start-tab memory. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class AppShellTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        clearAppPreferences()
    }

    private fun launch() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { AppShell() }
        }
    }

    private fun setStartTab(index: Int) {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("last_tab_index", index)
            .commit()
    }

    private fun storedTabIndex(): Int =
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
            .getInt("last_tab_index", -1)

    // ── Tabs ──

    @Test
    fun `all three tabs are shown`() {
        launch()
        compose.assertShows("Calculator")
        compose.assertShows("Converter")
        compose.assertShows("CNC")
    }

    @Test
    fun `the calculator is the default tab`() {
        launch()
        compose.assertShows("•••")   // calculator keypad strip
        compose.assertShows("( )")
        compose.assertDoesNotShow("Millimeter")
    }

    @Test
    fun `the converter tab shows the converter`() {
        launch()
        compose.tap("Converter")
        compose.assertShows("Millimeter")
        compose.assertShows("Length")
        compose.assertDoesNotShow("( )")
    }

    @Test
    fun `the CNC tab shows the CNC sub-tabs`() {
        launch()
        compose.tap("CNC")
        compose.assertShows("Triangle")
        compose.assertShows("SFM/RPM")
        compose.assertShows("Thread")
        compose.assertShows("Bolt Circle")
        compose.assertShows("Keyway")
    }

    @Test
    fun `tabs can be switched back and forth`() {
        launch()
        compose.tap("Converter")
        compose.assertShows("Millimeter")
        compose.tap("CNC")
        compose.assertShows("SFM/RPM")
        compose.tap("Calculator")
        compose.assertShows("( )")
        compose.assertDoesNotShow("SFM/RPM")
    }

    // ── Start-tab memory ──

    @Test
    fun `selecting a tab records it as the start tab`() {
        launch()
        compose.tap("Converter")
        assert(storedTabIndex() == 1) { "expected the converter tab to be stored, got ${storedTabIndex()}" }
        compose.tap("CNC")
        assert(storedTabIndex() == 2) { "expected the CNC tab to be stored, got ${storedTabIndex()}" }
    }

    @Test
    fun `it opens on the converter when that was the last tab used`() {
        setStartTab(1)
        launch()
        compose.assertShows("Millimeter")
    }

    @Test
    fun `it opens on CNC when that was the last tab used`() {
        setStartTab(2)
        launch()
        compose.assertShows("SFM/RPM")
    }

    @Test
    fun `an unknown stored tab index falls back to the calculator`() {
        setStartTab(99)
        launch()
        compose.assertShows("( )")
    }

    // ── Cross-tab behaviour ──

    @Test
    fun `the calculator expression survives a trip to another tab`() {
        launch()
        compose.tap("1")
        compose.tap("2")
        compose.tap("+")
        compose.tap("3")
        compose.assertShows("12+3")

        compose.tap("Converter")
        compose.assertDoesNotShow("12+3")

        compose.tap("Calculator")
        compose.assertShows("12+3")
        compose.assertShows("15")
    }

    @Test
    fun `converter values survive a trip to another tab`() {
        launch()
        compose.tap("Converter")
        compose.tap("2")
        compose.tap("5")
        compose.assertShows("2.5")   // centimetres

        compose.tap("CNC")
        compose.tap("Converter")
        compose.assertShows("25")
        compose.assertShows("2.5")
    }
}
