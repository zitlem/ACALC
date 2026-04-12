package com.acalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acalc.domain.UnitCategory
import com.acalc.ui.viewmodel.ConverterViewModel

private val CATEGORY_LABELS = mapOf(
    UnitCategory.LENGTH      to "Length",
    UnitCategory.WEIGHT      to "Weight",
    UnitCategory.VOLUME      to "Volume",
    UnitCategory.TEMPERATURE to "Temp",
    UnitCategory.AREA        to "Area",
    UnitCategory.SPEED       to "Speed"
)

@Composable
fun ConverterScreen(modifier: Modifier = Modifier) {
    val vm = viewModel<ConverterViewModel> { ConverterViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val unitOptions = vm.getUnitsForCategory(state.selectedCategory)
    var unitPickerRowIndex by remember { mutableStateOf<Int?>(null) }
    var showExprCalc by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        // Category tabs
        val categories = UnitCategory.entries
        val selectedIndex = categories.indexOf(state.selectedCategory)
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, modifier = Modifier.fillMaxWidth()) {
            categories.forEachIndexed { i, cat ->
                Tab(
                    selected = i == selectedIndex,
                    onClick = { vm.onCategorySelected(cat) },
                    text = { Text(CATEGORY_LABELS[cat] ?: cat.name) }
                )
            }
        }

        // Rows + hint + add-unit button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            state.rows.forEachIndexed { index, row ->
                ConverterRowItem(
                    unitName = unitOptions.getOrNull(row.unitIndex)?.second ?: "",
                    value = row.value,
                    isActive = index == state.activeRowIndex,
                    onTap = { vm.onRowActivated(index) },
                    onUnitTap = { unitPickerRowIndex = index }
                )
                HorizontalDivider()
            }

            // Conversion hint
            val hint = vm.getConversionHint(state)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Add unit row button
            TextButton(
                onClick = vm::onAddRow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add unit")
            }
        }

        HorizontalDivider()

        // Custom numpad
        ConverterNumpad(
            onKey = vm::onNumpadKey,
            onSwap = vm::onSwap,
            onExprCalc = { showExprCalc = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Unit picker bottom sheet
    unitPickerRowIndex?.let { rowIndex ->
        UnitPickerSheet(
            options = unitOptions,
            currentIndex = state.rows.getOrNull(rowIndex)?.unitIndex ?: 0,
            onSelect = { unitIndex ->
                vm.onUnitChanged(rowIndex, unitIndex)
                unitPickerRowIndex = null
            },
            onDismiss = { unitPickerRowIndex = null }
        )
    }

    // Expression calculator dialog
    if (showExprCalc) {
        val activeRow = state.rows.getOrNull(state.activeRowIndex)
        val activeUnitName = activeRow?.let {
            unitOptions.getOrNull(it.unitIndex)?.second
        } ?: ""
        ExpressionCalcDialog(
            unitName = activeUnitName,
            initialValue = activeRow?.value ?: "",
            onCommit = { expr -> vm.onExprCalcCommit(expr); showExprCalc = false },
            onDismiss = { showExprCalc = false }
        )
    }
}

@Composable
private fun ConverterRowItem(
    unitName: String,
    value: String,
    isActive: Boolean,
    onTap: () -> Unit,
    onUnitTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val bgColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unit selector button
        OutlinedButton(
            onClick = onUnitTap,
            modifier = Modifier.width(140.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = unitName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.width(10.dp))

        // Value display
        Box(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) activeColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (value.isEmpty() && isActive) "0" else value,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.End,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConverterNumpad(
    onKey: (String) -> Unit,
    onSwap: () -> Unit,
    onExprCalc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Column(modifier = modifier.padding(4.dp)) {
        // Row 1: 7  8  9  ⌫
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("7", shape, { onKey("7") }, Modifier.weight(1f))
            NumpadDigit("8", shape, { onKey("8") }, Modifier.weight(1f))
            NumpadDigit("9", shape, { onKey("9") }, Modifier.weight(1f))
            NumpadSpecial(shape, onClick = { onKey("⌫") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(22.dp))
            }
        }
        // Row 2: 4  5  6  ↕
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("4", shape, { onKey("4") }, Modifier.weight(1f))
            NumpadDigit("5", shape, { onKey("5") }, Modifier.weight(1f))
            NumpadDigit("6", shape, { onKey("6") }, Modifier.weight(1f))
            NumpadSpecial(shape, onClick = onSwap, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap", modifier = Modifier.size(22.dp))
            }
        }
        // Row 3: 1  2  3  expression-calc
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("1", shape, { onKey("1") }, Modifier.weight(1f))
            NumpadDigit("2", shape, { onKey("2") }, Modifier.weight(1f))
            NumpadDigit("3", shape, { onKey("3") }, Modifier.weight(1f))
            NumpadSpecial(shape, onClick = onExprCalc, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Calculate, contentDescription = "Expression calculator", modifier = Modifier.size(22.dp))
            }
        }
        // Row 4: 0  00  .  C
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("0",  shape, { onKey("0")  }, Modifier.weight(1f))
            NumpadDigit("00", shape, { onKey("00") }, Modifier.weight(1f))
            NumpadDigit(".",  shape, { onKey(".")  }, Modifier.weight(1f))
            NumpadSpecial(shape, onClick = { onKey("C") }, modifier = Modifier.weight(1f)) {
                Text("C", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun NumpadDigit(
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
            .height(56.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NumpadSpecial(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(56.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressionCalcDialog(
    unitName: String,
    initialValue: String,
    onCommit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf(initialValue) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title: unit name
            Text(unitName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Expression display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Full calculator numpad
            val btnShape = RoundedCornerShape(10.dp)

            val onKey: (String) -> Unit = { key ->
                when (key) {
                    "C" -> expression = ""
                    "DEL" -> expression = if (expression.isNotEmpty()) expression.dropLast(1) else ""
                    "±" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
                    "=" -> {
                        onCommit(expression)
                        onDismiss()
                    }
                    else -> expression = expression + key
                }
            }

            // Row 1: C  ⌫  %  ÷
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("C", btnShape, { onKey("C") }, Modifier.weight(1f), isOperator = true)
                ExprCalcBtn(null, btnShape, { onKey("DEL") }, Modifier.weight(1f), isOperator = true) {
                    Icon(Icons.Default.Backspace, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                }
                ExprCalcBtn("%", btnShape, { onKey("%") }, Modifier.weight(1f), isOperator = true)
                ExprCalcBtn("\u00F7", btnShape, { onKey("\u00F7") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 2: 7  8  9  ×
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("7", btnShape, { onKey("7") }, Modifier.weight(1f))
                ExprCalcBtn("8", btnShape, { onKey("8") }, Modifier.weight(1f))
                ExprCalcBtn("9", btnShape, { onKey("9") }, Modifier.weight(1f))
                ExprCalcBtn("\u00D7", btnShape, { onKey("\u00D7") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 3: 4  5  6  −
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("4", btnShape, { onKey("4") }, Modifier.weight(1f))
                ExprCalcBtn("5", btnShape, { onKey("5") }, Modifier.weight(1f))
                ExprCalcBtn("6", btnShape, { onKey("6") }, Modifier.weight(1f))
                ExprCalcBtn("-", btnShape, { onKey("-") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 4: 1  2  3  +
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("1", btnShape, { onKey("1") }, Modifier.weight(1f))
                ExprCalcBtn("2", btnShape, { onKey("2") }, Modifier.weight(1f))
                ExprCalcBtn("3", btnShape, { onKey("3") }, Modifier.weight(1f))
                ExprCalcBtn("+", btnShape, { onKey("+") }, Modifier.weight(1f), isOperator = true)
            }
            // Row 5: ±  0  .  =
            Row(Modifier.fillMaxWidth()) {
                ExprCalcBtn("±", btnShape, { onKey("±") }, Modifier.weight(1f), isOperator = true)
                ExprCalcBtn("0", btnShape, { onKey("0") }, Modifier.weight(1.5f))
                ExprCalcBtn(".", btnShape, { onKey(".") }, Modifier.weight(1f))
                ExprCalcBtn("=", btnShape, { onKey("=") }, Modifier.weight(1f), isOperator = true)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExprCalcBtn(
    label: String?,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    if (isOperator) {
        Button(
            onClick = onClick,
            shape = shape,
            modifier = modifier.padding(3.dp).height(52.dp)
        ) {
            content?.invoke() ?: Text(label ?: "", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            shape = shape,
            modifier = modifier.padding(3.dp).height(52.dp)
        ) {
            content?.invoke() ?: Text(label ?: "", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPickerSheet(
    options: List<Pair<String, String>>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Select Unit",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(bottom = 48.dp)) {
            items(options.size) { index ->
                val (_, displayName) = options[index]
                ListItem(
                    headlineContent = { Text(displayName) },
                    trailingContent = if (index == currentIndex) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    modifier = Modifier.clickable { onSelect(index) }
                )
                HorizontalDivider()
            }
        }
    }
}
