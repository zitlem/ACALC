package com.acalc.ui

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.acalc.ui.screens.CalculatorScreen
import com.acalc.ui.screens.CncScreen
import com.acalc.ui.screens.ConverterScreen
import com.acalc.ui.viewmodel.CalculatorViewModel
import kotlinx.serialization.Serializable

sealed interface TabRoute : NavKey

@Serializable
data object CalculatorRoute : TabRoute

@Serializable
data object ConverterRoute : TabRoute

@Serializable
data object CncRoute : TabRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE) }
    val app = context.applicationContext as android.app.Application
    val calcVm = viewModel<CalculatorViewModel> { CalculatorViewModel.create(app) }
    val startIndex = prefs.getInt("last_tab_index", 0)
    val startRoute: TabRoute = remember {
        when (startIndex) {
            1 -> ConverterRoute
            2 -> CncRoute
            else -> CalculatorRoute
        }
    }

    val backStack = rememberNavBackStack(startRoute)
    val currentRoute = backStack.last()
    val selectedTabIndex = when (currentRoute) {
        is ConverterRoute -> 1
        is CncRoute -> 2
        else -> 0
    }

    Scaffold(
        // Prevent Scaffold from double-counting the status bar — PrimaryTabRow handles it below
        contentWindowInsets = WindowInsets(0),
        topBar = {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.statusBarsPadding()   // push tabs below status bar
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        if (currentRoute !is CalculatorRoute) {
                            prefs.edit().putInt("last_tab_index", 0).apply()
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
                            calcVm.onTabLeave()
                            prefs.edit().putInt("last_tab_index", 1).apply()
                            backStack.clear()
                            backStack.add(ConverterRoute)
                        }
                    },
                    text = { Text("Converter") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        if (currentRoute !is CncRoute) {
                            calcVm.onTabLeave()
                            prefs.edit().putInt("last_tab_index", 2).apply()
                            backStack.clear()
                            backStack.add(CncRoute)
                        }
                    },
                    text = { Text("CNC") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {},
            entryProvider = entryProvider {
                entry<CalculatorRoute> {
                    CalculatorScreen(modifier = Modifier.padding(innerPadding).navigationBarsPadding())
                }
                entry<ConverterRoute> {
                    ConverterScreen(modifier = Modifier.padding(innerPadding).navigationBarsPadding())
                }
                entry<CncRoute> {
                    CncScreen(modifier = Modifier.padding(innerPadding).navigationBarsPadding())
                }
            }
        )
    }
}
