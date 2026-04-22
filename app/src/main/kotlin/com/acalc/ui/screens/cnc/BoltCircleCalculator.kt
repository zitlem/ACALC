package com.acalc.ui.screens.cnc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BoltCircleCalculator(modifier: Modifier = Modifier) {
    var metric by remember { mutableStateOf(false) }
    var nStr by remember { mutableStateOf("") }
    var bcdStr by remember { mutableStateOf("") }
    var startAngleStr by remember { mutableStateOf("0") }
    var holes by remember { mutableStateOf<List<Triple<Int, Double, Double>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    val unit = if (metric) "mm" else "in"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Inch / Metric toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !metric,
                onClick = { metric = false },
                label = { Text("Inch") }
            )
            FilterChip(
                selected = metric,
                onClick = { metric = true },
                label = { Text("Metric") }
            )
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
                val n = nStr.trim().toIntOrNull()
                val bcd = bcdStr.toDoubleOrNull()
                val startAngle = startAngleStr.toDoubleOrNull() ?: 0.0

                when {
                    n == null || n < 2 -> {
                        error = "Enter N ≥ 2"
                        holes = emptyList()
                        return@Button
                    }
                    bcd == null || bcd <= 0.0 -> {
                        error = "Enter a positive BCD"
                        holes = emptyList()
                        return@Button
                    }
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
        ) {
            Text("Compute")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (holes.isNotEmpty()) {
            HorizontalDivider()

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Hole #",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "X ($unit)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    "Y ($unit)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )
            }

            HorizontalDivider()

            holes.forEach { (holeNum, x, y) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        "$holeNum",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.4f".format(x),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        "%.4f".format(y),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.5f)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
