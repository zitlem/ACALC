package com.acalc.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.acalc.ui.screens.CalculatorScreen
import com.acalc.ui.screens.ConverterScreen
import kotlinx.serialization.Serializable

sealed interface TabRoute : NavKey

@Serializable
data object CalculatorRoute : TabRoute

@Serializable
data object ConverterRoute : TabRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE) }
    val startRoute: TabRoute = remember {
        if (prefs.getBoolean("last_tab_converter", false)) ConverterRoute else CalculatorRoute
    }

    val backStack = rememberNavBackStack(startRoute)
    val currentRoute = backStack.last()
    val selectedTabIndex = if (currentRoute is ConverterRoute) 1 else 0

    Scaffold(
        topBar = {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        if (currentRoute !is CalculatorRoute) {
                            prefs.edit().putBoolean("last_tab_converter", false).apply()
                            backStack.clear()
                            backStack.add(CalculatorRoute)
                        }
                    },
                    text = { Text("Calculator") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        if (currentRoute !is ConverterRoute) {
                            prefs.edit().putBoolean("last_tab_converter", true).apply()
                            backStack.clear()
                            backStack.add(ConverterRoute)
                        }
                    },
                    text = { Text("Converter") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {},
            entryProvider = entryProvider {
                entry<CalculatorRoute> {
                    CalculatorScreen(modifier = Modifier.padding(innerPadding))
                }
                entry<ConverterRoute> {
                    ConverterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        )
    }
}
