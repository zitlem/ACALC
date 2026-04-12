package com.acalc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private enum class TriangleMode { RIGHT, ANY }

@Composable
fun TriangleCalculatorContent(modifier: Modifier = Modifier) {
    var mode    by remember { mutableStateOf(TriangleMode.RIGHT) }
    var inputs  by remember { mutableStateOf(mapOf<String, String>()) }
    var results by remember { mutableStateOf<Map<String, Double>?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun onInput(k: String, v: String) { inputs = inputs.toMutableMap().also { it[k] = v } }
    fun onModeChange(m: TriangleMode) { mode = m; inputs = emptyMap(); results = null; errorMsg = null }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Mode toggle ────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == TriangleMode.RIGHT,
                onClick  = { onModeChange(TriangleMode.RIGHT) },
                label    = { Text("Right Triangle") }
            )
            FilterChip(
                selected = mode == TriangleMode.ANY,
                onClick  = { onModeChange(TriangleMode.ANY) },
                label    = { Text("Any Triangle") }
            )
        }

        // ── Inputs ─────────────────────────────────────────────────────────────
        if (mode == TriangleMode.RIGHT) {
            Text(
                "Enter any 2 values (sides and/or angles A°/B°). C = 90°, c = hypotenuse.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriInput("a",  "Side a",    inputs, ::onInput, Modifier.weight(1f))
                TriInput("b",  "Side b",    inputs, ::onInput, Modifier.weight(1f))
                TriInput("c",  "Hyp. c",    inputs, ::onInput, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriInput("A",  "Angle A°",  inputs, ::onInput, Modifier.weight(1f))
                TriInput("B",  "Angle B°",  inputs, ::onInput, Modifier.weight(1f))
            }
        } else {
            Text(
                "Enter 3+ values (at least 1 side). Angles in degrees.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriInput("a", "Side a",   inputs, ::onInput, Modifier.weight(1f))
                TriInput("b", "Side b",   inputs, ::onInput, Modifier.weight(1f))
                TriInput("c", "Side c",   inputs, ::onInput, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriInput("A", "Angle A°", inputs, ::onInput, Modifier.weight(1f))
                TriInput("B", "Angle B°", inputs, ::onInput, Modifier.weight(1f))
                TriInput("C", "Angle C°", inputs, ::onInput, Modifier.weight(1f))
            }
        }

        // ── Calculate ──────────────────────────────────────────────────────────
        Button(
            onClick = {
                val p = inputs.mapNotNull { (k, v) -> v.toDoubleOrNull()?.let { k to it } }.toMap()
                val computed = if (mode == TriangleMode.RIGHT)
                    solveRightTriangle(p["a"], p["b"], p["c"], p["A"], p["B"])
                else
                    solveAnyTriangle(p["a"], p["b"], p["c"], p["A"], p["B"], p["C"])
                if (computed != null) { results = computed; errorMsg = null }
                else {
                    results = null
                    errorMsg = if (mode == TriangleMode.RIGHT)
                        "Enter any 2 values (sides a/b/c or angles A°/B°)"
                    else "Not enough data or invalid values"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Calculate") }

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
        }

        // ── Results ────────────────────────────────────────────────────────────
        results?.let { res ->
            HorizontalDivider()

            // Visual diagram
            TriangleDiagram(res, isRightTriangle = mode == TriangleMode.RIGHT)

            Spacer(Modifier.height(4.dp))
            Text("Results", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))

            Text("Sides", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultTile("a", res["a"] ?: 0.0, Modifier.weight(1f))
                ResultTile("b", res["b"] ?: 0.0, Modifier.weight(1f))
                ResultTile("c", res["c"] ?: 0.0, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Text("Angles", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultTile("A°", res["A"] ?: 0.0, Modifier.weight(1f))
                ResultTile("B°", res["B"] ?: 0.0, Modifier.weight(1f))
                ResultTile("C°", res["C"] ?: 0.0, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultTile("Area",      res["area"]      ?: 0.0, Modifier.weight(1f))
                ResultTile("Perimeter", res["perimeter"] ?: 0.0, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Visual diagram ───────────────────────────────────────────────────────────

@Composable
private fun TriangleDiagram(results: Map<String, Double>, isRightTriangle: Boolean) {
    val a    = results["a"] ?: return
    val b    = results["b"] ?: return
    val c    = results["c"] ?: return
    val aDeg = results["A"] ?: return
    val bDeg = results["B"] ?: return
    val cDeg = results["C"] ?: return
    val cRad = Math.toRadians(cDeg)

    val colorPrimary    = MaterialTheme.colorScheme.primary
    val colorOutline    = MaterialTheme.colorScheme.outline
    val colorOnSurface  = MaterialTheme.colorScheme.onSurface
    val colorFill       = colorPrimary.copy(alpha = 0.10f)
    val textMeasurer    = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        // ── Vertex positions in math space (Y-up) ───────────────────────────
        // Convention: C at origin, B along +X, A above.
        //   |BC| = a,  |AC| = b,  angle at C = cRad
        val mC = Offset(0f, 0f)
        val mB = Offset(a.toFloat(), 0f)
        val mA = Offset((b * cos(cRad)).toFloat(), (b * sin(cRad)).toFloat())

        // ── Scale & center to fit canvas (flip Y) ───────────────────────────
        val xs = floatArrayOf(mC.x, mB.x, mA.x)
        val ys = floatArrayOf(mC.y, mB.y, mA.y)
        val minX = xs.min(); val maxX = xs.max()
        val minY = ys.min(); val maxY = ys.max()

        val padPx  = 58.dp.toPx()
        val drawW  = (size.width  - padPx * 2).coerceAtLeast(1f)
        val drawH  = (size.height - padPx * 2).coerceAtLeast(1f)
        val rangeX = (maxX - minX).coerceAtLeast(1e-6f)
        val rangeY = (maxY - minY).coerceAtLeast(1e-6f)
        val scale  = minOf(drawW / rangeX, drawH / rangeY)

        val shiftX = padPx + (drawW - rangeX * scale) / 2f
        val shiftY = padPx + (drawH - rangeY * scale) / 2f

        fun tx(p: Offset) = Offset(
            shiftX + (p.x - minX) * scale,
            size.height - shiftY - (p.y - minY) * scale
        )

        val cPt = tx(mC)
        val bPt = tx(mB)
        val aPt = tx(mA)
        val centroid = Offset((cPt.x + bPt.x + aPt.x) / 3f, (cPt.y + bPt.y + aPt.y) / 3f)

        // helper: unit vector
        fun unit(o: Offset): Offset {
            val len = sqrt(o.x * o.x + o.y * o.y)
            return if (len > 0f) Offset(o.x / len, o.y / len) else Offset.Zero
        }

        // ── Fill & outline ──────────────────────────────────────────────────
        val triPath = Path().apply {
            moveTo(cPt.x, cPt.y); lineTo(bPt.x, bPt.y); lineTo(aPt.x, aPt.y); close()
        }
        drawPath(triPath, color = colorFill)
        drawPath(triPath, color = colorPrimary, style = Stroke(width = 2.dp.toPx()))

        // Vertex dots
        val dotR = 4.dp.toPx()
        for (v in listOf(aPt, bPt, cPt)) drawCircle(colorPrimary, radius = dotR, center = v)

        // ── Right-angle square at C ─────────────────────────────────────────
        if (isRightTriangle) {
            val sq  = 10.dp.toPx()
            val d1  = unit(bPt - cPt) * sq
            val d2  = unit(aPt - cPt) * sq
            val sqPath = Path().apply {
                moveTo(cPt.x + d1.x,        cPt.y + d1.y)
                lineTo(cPt.x + d1.x + d2.x, cPt.y + d1.y + d2.y)
                lineTo(cPt.x + d2.x,        cPt.y + d2.y)
            }
            drawPath(sqPath, color = colorOutline, style = Stroke(1.5.dp.toPx()))
        }

        // ── Angle arcs ──────────────────────────────────────────────────────
        fun arcAt(vertex: Offset, p1: Offset, p2: Offset) {
            val r   = 16.dp.toPx()
            val s1  = atan2((p1 - vertex).y, (p1 - vertex).x) * 180f / PI.toFloat()
            val s2  = atan2((p2 - vertex).y, (p2 - vertex).x) * 180f / PI.toFloat()
            var sw  = s2 - s1
            while (sw < -180f) sw += 360f
            while (sw >  180f) sw -= 360f
            drawArc(
                color      = colorPrimary.copy(alpha = 0.55f),
                startAngle = s1, sweepAngle = sw,
                useCenter  = false,
                topLeft    = Offset(vertex.x - r, vertex.y - r),
                size       = Size(r * 2, r * 2),
                style      = Stroke(1.5.dp.toPx())
            )
        }
        arcAt(aPt, cPt, bPt)
        arcAt(bPt, aPt, cPt)
        if (!isRightTriangle) arcAt(cPt, bPt, aPt)

        // ── Text helper ─────────────────────────────────────────────────────
        fun label(text: String, center: Offset, sizeSp: Float,
                  color: Color, bold: Boolean = false) {
            val style = TextStyle(
                fontSize   = sizeSp.sp,
                color      = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                lineHeight = (sizeSp * 1.25f).sp
            )
            val m = textMeasurer.measure(text, style)
            drawText(
                textMeasurer = textMeasurer,
                text         = text,
                topLeft      = Offset(center.x - m.size.width / 2f, center.y - m.size.height / 2f),
                style        = style
            )
        }

        // ── Vertex labels ────────────────────────────────────────────────────
        // Each label is outside the triangle (away from centroid).
        val vDist = 28.dp.toPx()
        fun vLabel(v: Offset) = v + unit(v - centroid) * vDist

        label("A\n${fmtD(aDeg)}°", vLabel(aPt), 11f, colorOnSurface, bold = true)
        label("B\n${fmtD(bDeg)}°", vLabel(bPt), 11f, colorOnSurface, bold = true)
        label("C\n${fmtD(cDeg)}°", vLabel(cPt), 11f, colorOnSurface, bold = true)

        // ── Side midpoint labels ─────────────────────────────────────────────
        // Each label is outside the triangle (away from centroid).
        val sDist = 16.dp.toPx()
        fun sMid(v1: Offset, v2: Offset): Offset {
            val mid = Offset((v1.x + v2.x) / 2f, (v1.y + v2.y) / 2f)
            return mid + unit(mid - centroid) * sDist
        }

        // side a = |BC|, side b = |AC|, side c = |AB|
        label("a=${fmtD(a)}", sMid(bPt, cPt), 10f, colorPrimary, bold = true)
        label("b=${fmtD(b)}", sMid(aPt, cPt), 10f, colorPrimary, bold = true)
        label("c=${fmtD(c)}", sMid(aPt, bPt), 10f, colorPrimary, bold = true)
    }
}

// ─── Shared composables ───────────────────────────────────────────────────────

@Composable
private fun TriInput(
    key: String, label: String,
    inputs: Map<String, String>,
    onInput: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = inputs[key] ?: "",
        onValueChange = { onInput(key, it) },
        label         = { Text(label) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier      = modifier
    )
}

@Composable
private fun ResultTile(label: String, value: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier       = modifier,
        shape          = MaterialTheme.shapes.small,
        color          = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmtD(value), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Number formatting ────────────────────────────────────────────────────────

// Compact format for diagram labels (4 sig-fig max)
private fun fmtD(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    val absV = abs(v)
    return when {
        absV == 0.0  -> "0"
        absV >= 1000 -> "%.0f".format(v)
        absV >= 100  -> "%.1f".format(v).trimEnd('0').trimEnd('.')
        absV >= 10   -> "%.2f".format(v).trimEnd('0').trimEnd('.')
        absV >= 1    -> "%.3f".format(v).trimEnd('0').trimEnd('.')
        else         -> "%.4f".format(v).trimEnd('0').trimEnd('.')
    }
}

// Full precision for results table
private fun fmtFull(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    if (v == floor(v) && v in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble())
        return v.toLong().toString()
    return "%.6f".format(v).trimEnd('0').trimEnd('.')
}

// ─── Math solvers ─────────────────────────────────────────────────────────────

private fun solveRightTriangle(
    a: Double?, b: Double?, c: Double?,
    ADeg: Double? = null, BDeg: Double? = null
): Map<String, Double>? {
    // C = 90°, c = hypotenuse; A opposite a, B opposite b.
    // Build result from any valid pair of knowns.
    fun result(aV: Double, bV: Double, cV: Double): Map<String, Double> {
        val aAng = Math.toDegrees(atan2(aV, bV))
        return mapOf(
            "a" to aV, "b" to bV, "c" to cV,
            "A" to aAng, "B" to 90.0 - aAng, "C" to 90.0,
            "area" to 0.5 * aV * bV, "perimeter" to aV + bV + cV
        )
    }

    // Convert angle inputs to radians for trig; reject if out of (0°, 90°)
    val aRad = ADeg?.let { if (it > 0 && it < 90) Math.toRadians(it) else null }
    val bRad = BDeg?.let { if (it > 0 && it < 90) Math.toRadians(it) else null }

    return when {
        // ── Two sides ────────────────────────────────────────────────────────
        a != null && b != null && a > 0 && b > 0 ->
            result(a, b, sqrt(a * a + b * b))
        a != null && c != null && a > 0 && c > a ->
            result(a, sqrt(c * c - a * a), c)
        b != null && c != null && b > 0 && c > b ->
            result(sqrt(c * c - b * b), b, c)

        // ── Side a + angle A or B ─────────────────────────────────────────
        a != null && a > 0 && aRad != null ->
            // tan(A) = a/b  →  b = a/tan(A)
            result(a, a / kotlin.math.tan(aRad), a / sin(aRad))
        a != null && a > 0 && bRad != null ->
            // tan(B) = b/a  →  b = a*tan(B)
            result(a, a * kotlin.math.tan(bRad), a / cos(bRad))

        // ── Side b + angle A or B ─────────────────────────────────────────
        b != null && b > 0 && aRad != null ->
            // tan(A) = a/b  →  a = b*tan(A)
            result(b * kotlin.math.tan(aRad), b, b / cos(aRad))
        b != null && b > 0 && bRad != null ->
            // tan(B) = b/a  →  a = b/tan(B)
            result(b / kotlin.math.tan(bRad), b, b / sin(bRad))

        // ── Hypotenuse + angle A or B ─────────────────────────────────────
        c != null && c > 0 && aRad != null ->
            result(c * sin(aRad), c * cos(aRad), c)
        c != null && c > 0 && bRad != null ->
            result(c * cos(bRad), c * sin(bRad), c)

        else -> null
    }
}

private fun solveAnyTriangle(
    a: Double?, b: Double?, c: Double?,
    ADeg: Double?, BDeg: Double?, CDeg: Double?
): Map<String, Double>? {
    var aV = a; var bV = b; var cV = c
    var A  = ADeg?.let { Math.toRadians(it) }
    var B  = BDeg?.let { Math.toRadians(it) }
    var C  = CDeg?.let { Math.toRadians(it) }

    for (s   in listOfNotNull(aV, bV, cV)) if (s <= 0) return null
    for (ang in listOfNotNull(A,  B,  C))  if (ang <= 0 || ang >= PI) return null

    fun fillThird() {
        if      (A != null && B != null && C == null) C = PI - A!! - B!!
        else if (A != null && C != null && B == null) B = PI - A!! - C!!
        else if (B != null && C != null && A == null) A = PI - B!! - C!!
    }
    fillThird()

    // SAS → third side
    if (aV != null && bV != null && C != null && cV == null)
        cV = sqrt(aV!! * aV!! + bV!! * bV!! - 2 * aV!! * bV!! * cos(C!!))
    if (aV != null && cV != null && B != null && bV == null)
        bV = sqrt(aV!! * aV!! + cV!! * cV!! - 2 * aV!! * cV!! * cos(B!!))
    if (bV != null && cV != null && A != null && aV == null)
        aV = sqrt(bV!! * bV!! + cV!! * cV!! - 2 * bV!! * cV!! * cos(A!!))

    // SSS → angles
    if (aV != null && bV != null && cV != null) {
        if (A == null) A = acos(((bV!! * bV!! + cV!! * cV!! - aV!! * aV!!) / (2 * bV!! * cV!!)).coerceIn(-1.0, 1.0))
        if (B == null) B = acos(((aV!! * aV!! + cV!! * cV!! - bV!! * bV!!) / (2 * aV!! * cV!!)).coerceIn(-1.0, 1.0))
        if (C == null) C = acos(((aV!! * aV!! + bV!! * bV!! - cV!! * cV!!) / (2 * aV!! * bV!!)).coerceIn(-1.0, 1.0))
    }

    fillThird()

    // Law of sines for remaining unknowns
    val ratio: Double? = when {
        aV != null && A != null && sin(A!!) > 0 -> aV!! / sin(A!!)
        bV != null && B != null && sin(B!!) > 0 -> bV!! / sin(B!!)
        cV != null && C != null && sin(C!!) > 0 -> cV!! / sin(C!!)
        else -> null
    }
    if (ratio != null && ratio > 0) {
        if (aV == null && A != null) aV = ratio * sin(A!!)
        if (bV == null && B != null) bV = ratio * sin(B!!)
        if (cV == null && C != null) cV = ratio * sin(C!!)
        if (A  == null && aV != null) A = asin((aV!! / ratio).coerceIn(-1.0, 1.0))
        if (B  == null && bV != null) B = asin((bV!! / ratio).coerceIn(-1.0, 1.0))
        if (C  == null && cV != null) C = asin((cV!! / ratio).coerceIn(-1.0, 1.0))
    }

    fillThird()

    if (aV == null || bV == null || cV == null || A == null || B == null || C == null) return null
    if (aV!! <= 0 || bV!! <= 0 || cV!! <= 0) return null
    if (A!! <= 0  || B!! <= 0  || C!! <= 0)  return null

    val area = 0.5 * aV!! * bV!! * sin(C!!)
    return mapOf(
        "a" to aV!!, "b" to bV!!, "c" to cV!!,
        "A" to Math.toDegrees(A!!), "B" to Math.toDegrees(B!!), "C" to Math.toDegrees(C!!),
        "area" to area, "perimeter" to aV!! + bV!! + cV!!
    )
}
