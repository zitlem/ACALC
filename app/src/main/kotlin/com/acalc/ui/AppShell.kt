package com.acalc.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.acalc.ui.screens.CalculatorScreen
import com.acalc.ui.screens.ConverterScreen

// Route keys for Navigation3 back stack — must implement NavKey
sealed interface TabRoute : NavKey
data object CalculatorRoute : TabRoute
data object ConverterRoute : TabRoute

@Composable
fun AppShell() {
    val backStack = rememberNavBackStack(CalculatorRoute)
    val currentRoute = backStack.last()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute is CalculatorRoute,
                    onClick = {
                        if (currentRoute !is CalculatorRoute) {
                            backStack.clear()
                            backStack.add(CalculatorRoute)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Calculator") },
                    label = { Text("Calculator") }
                )
                NavigationBarItem(
                    selected = currentRoute is ConverterRoute,
                    onClick = {
                        if (currentRoute !is ConverterRoute) {
                            backStack.clear()
                            backStack.add(ConverterRoute)
                        }
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Converter") },
                    label = { Text("Converter") }
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
