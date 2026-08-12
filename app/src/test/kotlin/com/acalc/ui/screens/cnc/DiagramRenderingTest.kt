package com.acalc.ui.screens.cnc

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.acalc.ui.AcalcTheme
import com.acalc.ui.PHONE_QUALIFIERS
import com.acalc.ui.enterText
import com.acalc.ui.screens.TriangleCalculatorContent
import com.acalc.ui.tap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The triangle and bolt-circle diagrams are drawn in `Canvas` blocks. Compose only runs a draw
 * lambda when something rasterises the tree, so tests that assert on semantics alone leave all of
 * that geometry unexecuted. Drawing the decor view into a bitmap forces the draw pass.
 *
 * These are not pixel comparisons — they prove the drawing maths survives the inputs most likely
 * to break it: degenerate shapes, extreme aspect ratios, and very large or very small
 * coordinates, any of which could produce a NaN offset or divide by zero mid-draw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiagramRenderingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Runs the full draw pass, executing every Canvas block currently on screen. */
    private fun render() {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(bitmap))
    }

    private fun triangle(block: () -> Unit) {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { TriangleCalculatorContent() }
        }
        block()
        render()
    }

    private fun boltCircle(holes: String, bcd: String, startAngle: String? = null) {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { BoltCircleCalculator() }
        }
        compose.enterText("Number of Holes", holes)
        compose.enterText("Bolt Circle Diameter", bcd)
        if (startAngle != null) compose.enterText("Start Angle", startAngle)
        compose.tap("Compute")
        render()
    }

    // ── Triangle diagram ──

    @Test
    fun `the right-triangle diagram draws`() {
        triangle {
            compose.enterText("Side a", "3")
            compose.enterText("Side b", "4")
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws for a very wide triangle`() {
        triangle {
            compose.enterText("Side a", "1")
            compose.enterText("Side b", "1000")
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws for a very tall triangle`() {
        triangle {
            compose.enterText("Side a", "1000")
            compose.enterText("Side b", "1")
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws for a near-degenerate triangle`() {
        triangle {
            compose.tap("Any Triangle")
            compose.enterText("Side a", "10")
            compose.enterText("Side b", "10")
            compose.enterText("Angle C°", "0.01")   // almost a straight line
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws for an obtuse triangle`() {
        triangle {
            compose.tap("Any Triangle")
            compose.enterText("Side a", "3")
            compose.enterText("Side b", "4")
            compose.enterText("Angle C°", "150")
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws at tiny scale`() {
        triangle {
            compose.enterText("Side a", "0.001")
            compose.enterText("Side b", "0.002")
            compose.tap("Calculate")
        }
    }

    @Test
    fun `the diagram draws for an equilateral triangle`() {
        triangle {
            compose.tap("Any Triangle")
            compose.enterText("Side a", "10")
            compose.enterText("Side b", "10")
            compose.enterText("Side c", "10")
            compose.tap("Calculate")
        }
    }

    // ── Bolt circle diagram ──

    @Test
    fun `the bolt circle diagram draws`() {
        boltCircle(holes = "6", bcd = "10")
    }

    @Test
    fun `the bolt circle diagram draws for the minimum hole count`() {
        boltCircle(holes = "2", bcd = "1")
    }

    @Test
    fun `the bolt circle diagram draws for many holes`() {
        boltCircle(holes = "48", bcd = "250")
    }

    @Test
    fun `the bolt circle diagram draws with a start angle offset`() {
        boltCircle(holes = "5", bcd = "3.5", startAngle = "37.5")
    }

    @Test
    fun `the bolt circle diagram draws for a very small circle`() {
        boltCircle(holes = "3", bcd = "0.001")
    }

    @Test
    fun `the bolt circle diagram draws for a negative start angle`() {
        boltCircle(holes = "4", bcd = "10", startAngle = "-90")
    }

    @Test
    fun `the bolt circle diagram draws in metric`() {
        compose.setContent {
            AcalcTheme(dynamicColor = false) { BoltCircleCalculator() }
        }
        compose.tap("Metric")
        compose.enterText("Number of Holes", "8")
        compose.enterText("Bolt Circle Diameter", "120")
        compose.tap("Compute")
        render()
    }
}
