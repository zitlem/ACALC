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
import kotlin.math.PI

@Composable
fun SfmRpmCalculator(modifier: Modifier = Modifier) {
    var metric by remember { mutableStateOf(false) }
    var sfm by remember { mutableStateOf("") }
    var rpm by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var solved by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
                onClick = {
                    metric = false
                    sfm = ""; rpm = ""; dia = ""
                    solved = null; error = null
                },
                label = { Text("Inch") }
            )
            FilterChip(
                selected = metric,
                onClick = {
                    metric = true
                    sfm = ""; rpm = ""; dia = ""
                    solved = null; error = null
                },
                label = { Text("Metric") }
            )
        }

        val sfmLabel = if (metric) "SMM (Surface m/min)" else "SFM (Surface ft/min)"
        val diaLabel = if (metric) "Diameter (mm)" else "Diameter (in)"

        OutlinedTextField(
            value = sfm,
            onValueChange = { sfm = it; solved = null; error = null },
            label = { Text(sfmLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (solved == "sfm") {
                { Text("solved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            } else null
        )

        OutlinedTextField(
            value = rpm,
            onValueChange = { rpm = it; solved = null; error = null },
            label = { Text("RPM") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (solved == "rpm") {
                { Text("solved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            } else null
        )

        OutlinedTextField(
            value = dia,
            onValueChange = { dia = it; solved = null; error = null },
            label = { Text(diaLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (solved == "dia") {
                { Text("solved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            } else null
        )

        Button(
            onClick = {
                val sfmVal = sfm.toDoubleOrNull()
                val rpmVal = rpm.toDoubleOrNull()
                val diaVal = dia.toDoubleOrNull()

                val count = listOfNotNull(sfmVal, rpmVal, diaVal).size
                if (count != 2) {
                    error = "Enter exactly 2 values, leave the third blank"
                    solved = null
                    return@Button
                }

                // Inch: SFM = (RPM * PI * D_in) / 12
                // Metric: SMM = (RPM * PI * D_mm) / 1000
                val divisor = if (metric) 1000.0 else 12.0

                try {
                    when {
                        sfmVal == null -> {
                            // solve SFM
                            if (rpmVal == 0.0 || diaVal == 0.0) {
                                error = "Cannot solve: divide by zero"; solved = null; return@Button
                            }
                            val result = rpmVal!! * PI * diaVal!! / divisor
                            sfm = "%.3f".format(result)
                            solved = "sfm"; error = null
                        }
                        rpmVal == null -> {
                            // solve RPM
                            if (diaVal == 0.0) {
                                error = "Cannot solve: divide by zero"; solved = null; return@Button
                            }
                            val result = divisor * sfmVal / (PI * diaVal!!)
                            rpm = "%.3f".format(result)
                            solved = "rpm"; error = null
                        }
                        diaVal == null -> {
                            // solve Diameter
                            if (rpmVal == 0.0) {
                                error = "Cannot solve: divide by zero"; solved = null; return@Button
                            }
                            val result = divisor * sfmVal / (PI * rpmVal!!)
                            dia = "%.3f".format(result)
                            solved = "dia"; error = null
                        }
                    }
                } catch (e: Exception) {
                    error = "Calculation error"
                    solved = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
