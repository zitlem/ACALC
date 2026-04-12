package com.acalc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acalc.data.CalculationEntity
import com.acalc.ui.AcalcTheme
import com.acalc.ui.viewmodel.CalculatorState
import com.acalc.ui.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val vm = viewModel<CalculatorViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    CalculatorContent(
        state = state,
        onDigit = vm::onDigit,
        onOperator = vm::onOperator,
        onAdvanced = vm::onAdvanced,
        onParen = vm::onParen,
        onDecimal = vm::onDecimal,
        onClear = vm::onClear,
        onBackspace = vm::onBackspace,
        onPercent = vm::onPercent,
        onEquals = vm::onEquals,
        onShowHistory = { showHistory = true },
        modifier = modifier
    )

    if (showHistory) {
        HistorySheet(
            items = history,
            onDismiss = { showHistory = false },
            onClearAll = { vm.clearHistory() }
        )
    }
}

@Composable
private fun CalculatorContent(
    state: CalculatorState,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onAdvanced: (String) -> Unit,
    onParen: () -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DisplayArea(
            expression = state.expression,
            result = state.result,
            isError = state.isError,
            modifier = Modifier.weight(1f)
        )
        ButtonGrid(
            onDigit = onDigit,
            onOperator = onOperator,
            onAdvanced = onAdvanced,
            onParen = onParen,
            onDecimal = onDecimal,
            onClear = onClear,
            onBackspace = onBackspace,
            onPercent = onPercent,
            onEquals = onEquals,
            onShowHistory = onShowHistory,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DisplayArea(
    expression: String,
    result: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = expression,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = result,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ButtonGrid(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onAdvanced: (String) -> Unit,
    onParen: () -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    var showAdvancedMenu by remember { mutableStateOf(false) }

    if (showAdvancedMenu) {
        AdvancedFunctionsSheet(
            onKey = { key ->
                onAdvanced(key)
                showAdvancedMenu = false
            },
            onDismiss = { showAdvancedMenu = false }
        )
    }

    Column(modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        // Compact strip: •••  ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StripBtn(label = "•••", onClick = { showAdvancedMenu = true })
            StripBtn(label = "⌫",  onClick = onBackspace)
        }

        // Row 1: C, ( ), %, /
        Row(modifier = Modifier.fillMaxWidth()) {
            ActionBtn(label = "C", shape = shape, onClick = onClear, modifier = Modifier.weight(1f))
            ActionBtn(label = "( )", shape = shape, onClick = onParen, modifier = Modifier.weight(1f))
            ActionBtn(label = "%", shape = shape, onClick = onPercent, modifier = Modifier.weight(1f))
            OperatorBtn(label = "/", shape = shape, onClick = { onOperator("/") }, modifier = Modifier.weight(1f))
        }

        // Row 2: 7, 8, 9, x
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "7", shape = shape, onClick = { onDigit("7") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "8", shape = shape, onClick = { onDigit("8") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "9", shape = shape, onClick = { onDigit("9") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "x", shape = shape, onClick = { onOperator("x") }, modifier = Modifier.weight(1f))
        }

        // Row 3: 4, 5, 6, -
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "4", shape = shape, onClick = { onDigit("4") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "5", shape = shape, onClick = { onDigit("5") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "6", shape = shape, onClick = { onDigit("6") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "-", shape = shape, onClick = { onOperator("-") }, modifier = Modifier.weight(1f))
        }

        // Row 4: 1, 2, 3, +
        Row(modifier = Modifier.fillMaxWidth()) {
            DigitBtn(label = "1", shape = shape, onClick = { onDigit("1") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "2", shape = shape, onClick = { onDigit("2") }, modifier = Modifier.weight(1f))
            DigitBtn(label = "3", shape = shape, onClick = { onDigit("3") }, modifier = Modifier.weight(1f))
            OperatorBtn(label = "+", shape = shape, onClick = { onOperator("+") }, modifier = Modifier.weight(1f))
        }

        // Row 5: history, 0, ., =
        Row(modifier = Modifier.fillMaxWidth()) {
            HistoryBtn(shape = shape, onClick = onShowHistory, modifier = Modifier.weight(1f))
            DigitBtn(label = "0", shape = shape, onClick = { onDigit("0") }, modifier = Modifier.weight(1f))
            DigitBtn(label = ".", shape = shape, onClick = onDecimal, modifier = Modifier.weight(1f))
            EqualsBtn(shape = shape, onClick = onEquals, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFunctionsSheet(
    onKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val btnShape = RoundedCornerShape(10.dp)

    // (display label, expression string inserted)
    val rows = listOf(
        listOf("π"      to "π",     "e"      to "e",     "φ"      to "φ",     "^"   to "^"),
        listOf("log"    to "log(",   "ln"     to "ln(",   "log₂"   to "log2("),
        listOf("√"      to "√",     "³√"     to "cbrt(", "|x|"    to "abs("),
        listOf("sin"    to "sin(",   "cos"    to "cos(",  "tan"    to "tan("),
        listOf("sin⁻¹"  to "asin(", "cos⁻¹"  to "acos(", "tan⁻¹"  to "atan("),
        listOf("sinh"   to "sinh(",  "cosh"   to "cosh(", "tanh"   to "tanh("),
        listOf("sinh⁻¹" to "asinh(", "cosh⁻¹" to "acosh(", "tanh⁻¹" to "atanh(")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Scientific functions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            items(rows.size) { rowIndex ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rows[rowIndex].forEach { (label, value) ->
                        OutlinedButton(
                            onClick = { onKey(value) },
                            shape = btnShape,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .height(48.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StripBtn(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(36.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}


@Composable
private fun HistoryBtn(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        Icon(Icons.Default.History, contentDescription = "History")
    }
}

@Composable
private fun DigitBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun OperatorBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ActionBtn(
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EqualsBtn(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        Text(text = "=", style = MaterialTheme.typography.titleLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    items: List<CalculationEntity>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClearAll) { Text("Clear all") }
        }
        HorizontalDivider()
        if (items.isEmpty()) {
            Text(
                text = "No history yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.expression) },
                        supportingContent = {
                            Text(
                                text = "= ${item.result}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatorContentPreview() {
    AcalcTheme {
        CalculatorContent(
            state = CalculatorState(expression = "12+34x2", result = "80"),
            onDigit = {},
            onOperator = {},
            onAdvanced = {},
            onParen = {},
            onDecimal = {},
            onClear = {},
            onBackspace = {},
            onPercent = {},
            onEquals = {},
            onShowHistory = {}
        )
    }
}
