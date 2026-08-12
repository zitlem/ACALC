package com.acalc.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [AcalcTheme] picks between four colour schemes — dynamic light/dark on Android 12+, and the
 * bundled light/dark palettes otherwise. Every branch is reachable from tests by pinning the SDK
 * level and the night-mode qualifier.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
class AppThemeTest {

    @get:Rule
    val compose = createComposeRule()

    /** Renders [AcalcTheme] and hands back the scheme it resolved. */
    private fun schemeFor(darkTheme: Boolean? = null, dynamicColor: Boolean): ColorScheme {
        lateinit var scheme: ColorScheme
        compose.setContent {
            if (darkTheme == null) {
                AcalcTheme(dynamicColor = dynamicColor) {
                    scheme = MaterialTheme.colorScheme
                    Text("themed")
                }
            } else {
                AcalcTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                    scheme = MaterialTheme.colorScheme
                    Text("themed")
                }
            }
        }
        compose.waitForIdle()
        return scheme
    }

    /**
     * Resolves the light and dark schemes in one composition — [createComposeRule] allows only a
     * single `setContent` per test.
     */
    private fun lightAndDark(dynamicColor: Boolean): Pair<ColorScheme, ColorScheme> {
        lateinit var light: ColorScheme
        lateinit var dark: ColorScheme
        compose.setContent {
            AcalcTheme(darkTheme = false, dynamicColor = dynamicColor) {
                light = MaterialTheme.colorScheme
            }
            AcalcTheme(darkTheme = true, dynamicColor = dynamicColor) {
                dark = MaterialTheme.colorScheme
            }
        }
        compose.waitForIdle()
        return light to dark
    }

    // ── Bundled palettes ──

    @Test
    fun `the light palette uses the bundled primary`() {
        val scheme = schemeFor(darkTheme = false, dynamicColor = false)
        assertEquals(Color(0xFF6650A4), scheme.primary)
    }

    @Test
    fun `the dark palette uses the bundled primary`() {
        val scheme = schemeFor(darkTheme = true, dynamicColor = false)
        assertEquals(Color(0xFFD0BCFF), scheme.primary)
    }

    @Test
    fun `light and dark palettes differ`() {
        val (light, dark) = lightAndDark(dynamicColor = false)
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.background, dark.background)
    }

    @Test
    fun `the secondary and tertiary roles are themed too`() {
        val (light, dark) = lightAndDark(dynamicColor = false)
        assertEquals(Color(0xFF625B71), light.secondary)
        assertEquals(Color(0xFF7D5260), light.tertiary)
        assertEquals(Color(0xFFCCC2DC), dark.secondary)
        assertEquals(Color(0xFFEFB8C8), dark.tertiary)
    }

    // ── Dynamic colour (Android 12+) ──

    @Test
    @Config(sdk = [34])
    fun `dynamic colour resolves a light scheme on Android 12 and above`() {
        val scheme = schemeFor(darkTheme = false, dynamicColor = true)
        // The device palette is not the bundled one.
        assertNotEquals(Color(0xFF6650A4), scheme.primary)
        assertNotEquals(Color.Unspecified, scheme.primary)
    }

    @Test
    @Config(sdk = [34])
    fun `dynamic colour resolves a dark scheme on Android 12 and above`() {
        val (light, dark) = lightAndDark(dynamicColor = true)
        assertNotEquals(light.background, dark.background)
    }

    @Test
    @Config(sdk = [26])
    fun `dynamic colour falls back to the bundled palette below Android 12`() {
        val scheme = schemeFor(darkTheme = false, dynamicColor = true)
        assertEquals(Color(0xFF6650A4), scheme.primary)
    }

    @Test
    @Config(sdk = [26])
    fun `dynamic colour falls back to the bundled dark palette below Android 12`() {
        val scheme = schemeFor(darkTheme = true, dynamicColor = true)
        assertEquals(Color(0xFFD0BCFF), scheme.primary)
    }

    // ── Following the system setting ──

    @Test
    @Config(qualifiers = "$PHONE_QUALIFIERS-notnight")
    fun `it follows the system light setting by default`() {
        val scheme = schemeFor(dynamicColor = false)
        assertEquals(Color(0xFF6650A4), scheme.primary)
    }

    @Test
    @Config(qualifiers = "$PHONE_QUALIFIERS-night")
    fun `it follows the system dark setting by default`() {
        val scheme = schemeFor(dynamicColor = false)
        assertEquals(Color(0xFFD0BCFF), scheme.primary)
    }
}
