package com.acalc.ui.screens.cnc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import kotlin.math.abs

// ─── Thread reference data ────────────────────────────────────────────────────

private data class InchThread(val size: String, val tpi: Int, val drill: String, val dec: Double)
private data class MetricThread(val size: String, val pitch: Double, val drill: Double)

private val UNC = listOf(
    InchThread("#4-40",   40, "#43",   0.0890),
    InchThread("#6-32",   32, "#36",   0.1065),
    InchThread("#8-32",   32, "#29",   0.1360),
    InchThread("#10-24",  24, "#25",   0.1495),
    InchThread("1/4-20",  20, "#7",    0.2010),
    InchThread("5/16-18", 18, "F",     0.2570),
    InchThread("3/8-16",  16, "5/16",  0.3125),
    InchThread("7/16-14", 14, "U",     0.3680),
    InchThread("1/2-13",  13, "27/64", 0.4219),
    InchThread("9/16-12", 12, "31/64", 0.4844),
    InchThread("5/8-11",  11, "17/32", 0.5312),
    InchThread("3/4-10",  10, "21/32", 0.6562),
    InchThread("7/8-9",    9, "49/64", 0.7656),
    InchThread("1\"-8",    8, "7/8",   0.8750),
)

private val UNF = listOf(
    InchThread("#4-48",   48, "#42",   0.0935),
    InchThread("#6-40",   40, "#33",   0.1130),
    InchThread("#8-36",   36, "#29",   0.1360),
    InchThread("#10-32",  32, "#21",   0.1590),
    InchThread("1/4-28",  28, "#3",    0.2130),
    InchThread("5/16-24", 24, "I",     0.2720),
    InchThread("3/8-24",  24, "Q",     0.3320),
    InchThread("7/16-20", 20, "25/64", 0.3906),
    InchThread("1/2-20",  20, "29/64", 0.4531),
    InchThread("9/16-18", 18, "33/64", 0.5156),
    InchThread("5/8-18",  18, "37/64", 0.5781),
    InchThread("3/4-16",  16, "11/16", 0.6875),
    InchThread("7/8-14",  14, "13/16", 0.8125),
    InchThread("1\"-14",  14, "15/16", 0.9375),
)

private val METRIC_COARSE = listOf(
    MetricThread("M2",   0.40,  1.60),
    MetricThread("M2.5", 0.45,  2.05),
    MetricThread("M3",   0.50,  2.50),
    MetricThread("M4",   0.70,  3.30),
    MetricThread("M5",   0.80,  4.20),
    MetricThread("M6",   1.00,  5.00),
    MetricThread("M8",   1.25,  6.80),
    MetricThread("M10",  1.50,  8.50),
    MetricThread("M12",  1.75, 10.20),
    MetricThread("M14",  2.00, 12.00),
    MetricThread("M16",  2.00, 14.00),
    MetricThread("M18",  2.50, 15.50),
    MetricThread("M20",  2.50, 17.50),
    MetricThread("M22",  2.50, 19.50),
    MetricThread("M24",  3.00, 21.00),
    MetricThread("M27",  3.00, 24.00),
    MetricThread("M30",  3.50, 26.50),
    MetricThread("M36",  4.00, 32.00),
    MetricThread("M42",  4.50, 37.50),
    MetricThread("M48",  5.00, 43.00),
)

private fun nearestInchDrill(decimal: Double): String? {
    val all = UNC + UNF
    val best = all.minByOrNull { abs(it.dec - decimal) } ?: return null
    return if (abs(best.dec - decimal) <= 0.002) best.drill else null
}

// ─── Main composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadCalculator(modifier: Modifier = Modifier) {
    var subTab by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Calculate") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Chart") })
        }
        when (subTab) {
            0 -> ThreadCalculate(modifier = Modifier.fillMaxWidth())
            1 -> ThreadChart(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─── Calculate sub-tab ────────────────────────────────────────────────────────

@Composable
private fun ThreadCalculate(modifier: Modifier = Modifier) {
    var metric by remember { mutableStateOf(false) }
    var major by remember { mutableStateOf("") }
    var tpiOrPitch by remember { mutableStateOf("") }

    val d = major.toDoubleOrNull()
    val tpiPitch = tpiOrPitch.toDoubleOrNull()

    data class Results(
        val tapDrill: Double,
        val pitchDia: Double,
        val minorDia: Double,
        val wire: Double,
        val measOverWires: Double,
        val tapDrillLabel: String
    )

    val results: Results? = if (d != null && tpiPitch != null && tpiPitch > 0) {
        if (!metric) {
            val tpi = tpiPitch
            val pitch = 1.0 / tpi
            val tapDrill = d - 0.9743 / tpi
            val pitchDia = d - 0.6495 / tpi
            val minorDia = d - 1.2990 / tpi
            val wire = 0.57735 / tpi
            val mow = pitchDia + 3.0 * wire - 0.866 * pitch
            val drillName = nearestInchDrill(tapDrill)
            val tapDrillLabel = if (drillName != null) "%.4f ($drillName)".format(tapDrill) else "%.4f".format(tapDrill)
            Results(tapDrill, pitchDia, minorDia, wire, mow, tapDrillLabel)
        } else {
            val pitch = tpiPitch
            val tapDrill = d - pitch
            val pitchDia = d - 0.6495 * pitch
            val minorDia = d - 1.2990 * pitch
            val wire = 0.57735 * pitch
            val mow = pitchDia + 3.0 * wire - 0.866 * pitch
            Results(tapDrill, pitchDia, minorDia, wire, mow, "%.4f".format(tapDrill))
        }
    } else null

    val unit = if (metric) "mm" else "in"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !metric,
                onClick = { metric = false; major = ""; tpiOrPitch = "" },
                label = { Text("Inch (TPI)") }
            )
            FilterChip(
                selected = metric,
                onClick = { metric = true; major = ""; tpiOrPitch = "" },
                label = { Text("Metric (pitch)") }
            )
        }

        OutlinedTextField(
            value = major,
            onValueChange = { major = it },
            label = { Text("Major Diameter ($unit)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tpiOrPitch,
            onValueChange = { tpiOrPitch = it },
            label = { Text(if (metric) "Pitch (mm)" else "TPI (threads per inch)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        if (results != null) {
            HorizontalDivider()
            // Tap Drill — label already formatted with drill name
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("Tap Drill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(results.tapDrillLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThreadResultTile("Pitch Diameter ($unit)", "%.4f".format(results.pitchDia), Modifier.weight(1f))
                ThreadResultTile("Minor Diameter ($unit)", "%.4f".format(results.minorDia), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThreadResultTile("Best Wire ($unit)", "%.4f".format(results.wire), Modifier.weight(1f))
                ThreadResultTile("Meas. Over Wires ($unit)", "%.4f".format(results.measOverWires), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThreadResultTile(label: String, value: String, modifier: Modifier = Modifier) {
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

// ─── Chart sub-tab ────────────────────────────────────────────────────────────

@Composable
private fun ThreadChart(modifier: Modifier = Modifier) {
    var chart by remember { mutableStateOf("UNC") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chart type toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = chart == "UNC",    onClick = { chart = "UNC" },    label = { Text("UNC") })
            FilterChip(selected = chart == "UNF",    onClick = { chart = "UNF" },    label = { Text("UNF") })
            FilterChip(selected = chart == "Metric", onClick = { chart = "Metric" }, label = { Text("Metric") })
        }

        HorizontalDivider()

        if (chart == "UNC" || chart == "UNF") {
            val data = if (chart == "UNC") UNC else UNF
            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Size",       style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("TPI",        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Tap Drill",  style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Dec. (in)",  style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
            data.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(row.size,              style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                    Text("${row.tpi}",          style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(row.drill,             style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("%.4f".format(row.dec), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
        } else {
            // Metric
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Size",         style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Pitch (mm)",   style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Tap Drill (mm)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
            METRIC_COARSE.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(row.size,                  style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("%.2f".format(row.pitch),  style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("%.2f".format(row.drill),  style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
        }
    }
}
