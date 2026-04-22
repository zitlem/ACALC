package com.acalc.ui.screens.cnc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// ─── Lookup tables ────────────────────────────────────────────────────────────

private data class KeyRow(val w: Double, val h: Double, val ds: Double, val dh: Double)

// ASME B17.1 — shaft_max (inch, exclusive upper bound) → dimensions (inch)
private val ASME_INCH = listOf(
    0.4375 to KeyRow(3.0 / 32.0, 3.0 / 32.0, 3.0 / 64.0, 3.0 / 64.0),
    0.5625 to KeyRow(1.0 / 8.0,  1.0 / 8.0,  1.0 / 16.0, 1.0 / 16.0),
    0.875  to KeyRow(3.0 / 16.0, 3.0 / 16.0, 3.0 / 32.0, 3.0 / 32.0),
    1.250  to KeyRow(1.0 / 4.0,  1.0 / 4.0,  1.0 / 8.0,  1.0 / 8.0),
    1.375  to KeyRow(5.0 / 16.0, 5.0 / 16.0, 5.0 / 32.0, 5.0 / 32.0),
    1.750  to KeyRow(3.0 / 8.0,  3.0 / 8.0,  3.0 / 16.0, 3.0 / 16.0),
    2.250  to KeyRow(1.0 / 2.0,  1.0 / 2.0,  1.0 / 4.0,  1.0 / 4.0),
    2.750  to KeyRow(5.0 / 8.0,  5.0 / 8.0,  5.0 / 16.0, 5.0 / 16.0),
    3.250  to KeyRow(3.0 / 4.0,  3.0 / 4.0,  3.0 / 8.0,  3.0 / 8.0),
    3.750  to KeyRow(7.0 / 8.0,  7.0 / 8.0,  7.0 / 16.0, 7.0 / 16.0),
    4.500  to KeyRow(1.00,        1.00,        1.0 / 2.0,  1.0 / 2.0),
    5.500  to KeyRow(1.25,        1.25,        5.0 / 8.0,  5.0 / 8.0),
    6.500  to KeyRow(1.50,        1.50,        3.0 / 4.0,  3.0 / 4.0),
)

// ISO 773 — shaft_max (mm, exclusive upper bound) → dimensions (mm)
private val ISO_METRIC = listOf(
    8.0   to KeyRow(2.0,  2.0,  1.2, 1.0),
    10.0  to KeyRow(3.0,  3.0,  1.8, 1.4),
    12.0  to KeyRow(4.0,  4.0,  2.5, 1.8),
    17.0  to KeyRow(5.0,  5.0,  3.0, 2.3),
    22.0  to KeyRow(6.0,  6.0,  3.5, 2.8),
    30.0  to KeyRow(8.0,  7.0,  4.0, 3.3),
    38.0  to KeyRow(10.0, 8.0,  5.0, 3.3),
    44.0  to KeyRow(12.0, 8.0,  5.0, 3.3),
    50.0  to KeyRow(14.0, 9.0,  5.5, 3.8),
    58.0  to KeyRow(16.0, 10.0, 6.0, 4.3),
    65.0  to KeyRow(18.0, 11.0, 7.0, 4.4),
    75.0  to KeyRow(20.0, 12.0, 7.5, 4.9),
    85.0  to KeyRow(22.0, 14.0, 9.0, 5.4),
    95.0  to KeyRow(25.0, 14.0, 9.0, 5.4),
    110.0 to KeyRow(28.0, 16.0, 10.0, 6.4),
    130.0 to KeyRow(32.0, 18.0, 11.0, 7.4),
    150.0 to KeyRow(36.0, 20.0, 12.0, 8.4),
)

private fun lookupInch(shaftIn: Double): KeyRow? =
    if (shaftIn <= 0.0) null
    else ASME_INCH.firstOrNull { (maxEx, _) -> shaftIn < maxEx }?.second

private fun lookupMetric(shaftMm: Double): KeyRow? =
    if (shaftMm <= 6.0) null  // ISO 773 lower bound = 6 mm (exclusive)
    else ISO_METRIC.firstOrNull { (maxEx, _) -> shaftMm < maxEx }?.second

// ─── Composable ───────────────────────────────────────────────────────────────

@Composable
fun KeywayCalculator(modifier: Modifier = Modifier) {
    var metric by remember { mutableStateOf(false) }
    var shaftDia by remember { mutableStateOf("") }

    val shaftVal = shaftDia.toDoubleOrNull()
    val keyRow = shaftVal?.let { if (metric) lookupMetric(it) else lookupInch(it) }
    val outOfRange = shaftVal != null && shaftVal > 0.0 && keyRow == null

    val unit = if (metric) "mm" else "in"
    val spec = if (metric) "ISO 773" else "ASME B17.1"
    val fmt = if (metric) "%.2f" else "%.4f"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !metric,
                onClick = { metric = false; shaftDia = "" },
                label = { Text("Inch (ASME B17.1)") }
            )
            FilterChip(
                selected = metric,
                onClick = { metric = true; shaftDia = "" },
                label = { Text("Metric (ISO 773)") }
            )
        }

        OutlinedTextField(
            value = shaftDia,
            onValueChange = { shaftDia = it },
            label = { Text("Shaft diameter ($unit)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        if (outOfRange) {
            Text(
                "Shaft diameter outside $spec range",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (keyRow != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KeyResultTile("Key Width ($unit)", fmt.format(keyRow.w), Modifier.weight(1f))
                KeyResultTile("Key Height ($unit)", fmt.format(keyRow.h), Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KeyResultTile("Depth in Shaft ($unit)", fmt.format(keyRow.ds), Modifier.weight(1f))
                KeyResultTile("Depth in Hub ($unit)",   fmt.format(keyRow.dh), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyResultTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
