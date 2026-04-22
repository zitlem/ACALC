package com.acalc.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.acalc.ui.screens.cnc.BoltCircleCalculator
import com.acalc.ui.screens.cnc.KeywayCalculator
import com.acalc.ui.screens.cnc.SfmRpmCalculator
import com.acalc.ui.screens.cnc.ThreadCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CncScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE) }
    var selectedIndex by remember { mutableStateOf(prefs.getInt("cnc_last_tab", 0).coerceIn(0, 4)) }
    val labels = listOf("Triangle", "SFM/RPM", "Thread", "Bolt Circle", "Keyway")

    Column(modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = i == selectedIndex,
                    onClick = {
                        selectedIndex = i
                        prefs.edit().putInt("cnc_last_tab", i).apply()
                    },
                    text = { Text(label) }
                )
            }
        }
        when (selectedIndex) {
            0 -> TriangleCalculatorContent(modifier = Modifier.fillMaxSize())
            1 -> SfmRpmCalculator(modifier = Modifier.fillMaxSize())
            2 -> ThreadCalculator(modifier = Modifier.fillMaxSize())
            3 -> BoltCircleCalculator(modifier = Modifier.fillMaxSize())
            4 -> KeywayCalculator(modifier = Modifier.fillMaxSize())
        }
    }
}
