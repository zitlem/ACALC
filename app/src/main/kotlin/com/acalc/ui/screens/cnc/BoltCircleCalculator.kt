package com.acalc.ui.screens.cnc

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun BoltCircleCalculator(modifier: Modifier = Modifier) {
    var metric       by remember { mutableStateOf(false) }
    var nStr         by remember { mutableStateOf("") }
    var bcdStr       by remember { mutableStateOf("") }
    var startAngleStr by remember { mutableStateOf("0") }
    var holes        by remember { mutableStateOf<List<Triple<Int, Double, Double>>>(emptyList()) }
    var error        by remember { mutableStateOf<String?>(null) }

    val unit = if (metric) "mm" else "in"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !metric, onClick = { metric = false }, label = { Text("Inch") })
            FilterChip(selected = metric,  onClick = { metric = true  }, label = { Text("Metric") })
        }

        OutlinedTextField(
            value = nStr,
            onValueChange = { nStr = it },
            label = { Text("Number of Holes (N ≥ 2)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bcdStr,
            onValueChange = { bcdStr = it },
            label = { Text("Bolt Circle Diameter (BCD, $unit)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = startAngleStr,
            onValueChange = { startAngleStr = it },
            label = { Text("Start Angle (°)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val n          = nStr.trim().toIntOrNull()
                val bcd        = bcdStr.toDoubleOrNull()
                val startAngle = startAngleStr.toDoubleOrNull() ?: 0.0
                when {
                    n == null || n < 2     -> { error = "Enter N ≥ 2"; holes = emptyList() }
                    bcd == null || bcd <= 0 -> { error = "Enter a positive BCD"; holes = emptyList() }
                    else -> {
                        error = null
                        val step = 360.0 / n
                        holes = (0 until n).map { i ->
                            val ang = Math.toRadians(startAngle + step * i)
                            Triple(i + 1, (bcd / 2.0) * cos(ang), (bcd / 2.0) * sin(ang))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Compute") }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        // ── Diagram ──────────────────────────────────────────────────────────
        if (holes.isNotEmpty()) {
            val bcd        = bcdStr.toDoubleOrNull() ?: 1.0
            val primary    = MaterialTheme.colorScheme.primary
            val onSurface  = MaterialTheme.colorScheme.onSurface

            Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val drawRadius = min(size.width, size.height) * 0.38f
                val strokePx   = 1.5.dp.toPx()
                val dotRadius  = 6.dp.toPx()

                // Faint axis lines
                val axisColor = onSurface.copy(alpha = 0.2f)
                drawLine(axisColor, Offset(cx - drawRadius * 1.25f, cy), Offset(cx + drawRadius * 1.25f, cy), strokePx)
                drawLine(axisColor, Offset(cx, cy - drawRadius * 1.25f), Offset(cx, cy + drawRadius * 1.25f), strokePx)

                // Bolt circle ring
                drawCircle(primary.copy(alpha = 0.25f), drawRadius, Offset(cx, cy), style = Stroke(strokePx))

                // Centre dot
                drawCircle(onSurface.copy(alpha = 0.35f), 3.dp.toPx(), Offset(cx, cy))

                // Hole dots + number labels
                val textPaint = NativePaint().apply {
                    color     = primary.toArgb()
                    textSize  = 11.dp.toPx()
                    textAlign = NativePaint.Align.CENTER
                    isAntiAlias = true
                }
                val radius = bcd / 2.0
                holes.forEach { (num, x, y) ->
                    val sx = cx + (x / radius).toFloat() * drawRadius
                    val sy = cy - (y / radius).toFloat() * drawRadius  // invert Y for screen coords
                    drawCircle(primary, dotRadius, Offset(sx, sy))
                    drawIntoCanvas { c ->
                        c.nativeCanvas.drawText("$num", sx, sy - dotRadius - 2.dp.toPx(), textPaint)
                    }
                }
            }

            HorizontalDivider()

            // ── Coordinate table ─────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Hole #",   style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("X ($unit)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("Y ($unit)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            }
            HorizontalDivider()
            holes.forEach { (holeNum, x, y) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("$holeNum",          style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("%.4f".format(x),    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                    Text("%.4f".format(y),    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                }
                HorizontalDivider()
            }
        }
    }
}
