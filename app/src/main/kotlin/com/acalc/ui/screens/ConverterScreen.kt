package com.acalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
    UnitCategory.TRIANGLE    to "Triangle",
    UnitCategory.LENGTH      to "Length",
    UnitCategory.WEIGHT      to "Weight",
    UnitCategory.VOLUME      to "Volume",
    UnitCategory.TEMPERATURE to "Temp",
    UnitCategory.AREA        to "Area",
    UnitCategory.SPEED       to "Speed",
    UnitCategory.TIME        to "Time",
    UnitCategory.FORCE       to "Force",
    UnitCategory.PRESSURE    to "Pressure",
    UnitCategory.ENERGY      to "Energy",
    UnitCategory.POWER       to "Power",
    UnitCategory.ANGLE       to "Angle",
    UnitCategory.DATA        to "Data"
)

@Composable
fun ConverterScreen(modifier: Modifier = Modifier) {
    val vm = viewModel<ConverterViewModel> { ConverterViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val unitOptions = vm.getUnitsForCategory(state.selectedCategory)
    var unitPickerRowIndex by remember { mutableStateOf<Int?>(null) }

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

        if (state.selectedCategory == UnitCategory.TRIANGLE) {
            TriangleCalculatorContent(modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            val liveResult = vm.getLiveResult(state)
            // Rows — each gets equal vertical space, all always visible
            state.rows.forEachIndexed { index, row ->
                ConverterRowItem(
                    unitName = unitOptions.getOrNull(row.unitIndex)?.second ?: "",
                    value = row.value,
                    isActive = index == state.activeRowIndex,
                    liveResult = if (index == state.activeRowIndex) liveResult else null,
                    onTap = { vm.onRowActivated(index) },
                    onUnitTap = { unitPickerRowIndex = index },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(max = 80.dp)
                )
                HorizontalDivider()
            }

            // Conversion hint
            val hint = vm.getConversionHint(state)
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider()

            // Calculator-style numpad
            ConverterNumpad(
                onKey = vm::onNumpadKey,
                onEnter = vm::onEnter,
                onFocusNext = vm::onFocusNextRow,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
}

@Composable
private fun ConverterRowItem(
    unitName: String,
    value: String,
    isActive: Boolean,
    liveResult: String?,
    onTap: () -> Unit,
    onUnitTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val bgColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

    Row(
        modifier = modifier
            .background(bgColor)
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unit selector button
        OutlinedButton(
            onClick = onUnitTap,
            modifier = Modifier.width(110.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = unitName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.width(8.dp))

        // Value display — fills remaining width and all available row height
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) activeColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (liveResult != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = liveResult,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = if (value.isEmpty() && isActive) "0" else value,
                    style = MaterialTheme.typography.titleLarge,
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
}

@Composable
private fun ConverterNumpad(
    onKey: (String) -> Unit,
    onEnter: () -> Unit,
    onFocusNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Column(modifier = modifier.padding(4.dp)) {
        // Row 1: C  ⌫  ±  ÷
        Row(Modifier.fillMaxWidth()) {
            NumpadAction("C",  shape, { onKey("C") },  Modifier.weight(1f))
            NumpadAction(null, shape, { onKey("⌫") }, Modifier.weight(1f)) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(20.dp))
            }
            NumpadAction("±", shape, { onKey("±") }, Modifier.weight(1f))
            NumpadOperator("÷", shape, { onKey("÷") }, Modifier.weight(1f))
        }
        // Row 2: 7  8  9  ×
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("7", shape, { onKey("7") }, Modifier.weight(1f))
            NumpadDigit("8", shape, { onKey("8") }, Modifier.weight(1f))
            NumpadDigit("9", shape, { onKey("9") }, Modifier.weight(1f))
            NumpadOperator("×", shape, { onKey("×") }, Modifier.weight(1f))
        }
        // Row 3: 4  5  6  -
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("4", shape, { onKey("4") }, Modifier.weight(1f))
            NumpadDigit("5", shape, { onKey("5") }, Modifier.weight(1f))
            NumpadDigit("6", shape, { onKey("6") }, Modifier.weight(1f))
            NumpadOperator("-", shape, { onKey("-") }, Modifier.weight(1f))
        }
        // Row 4: 1  2  3  +
        Row(Modifier.fillMaxWidth()) {
            NumpadDigit("1", shape, { onKey("1") }, Modifier.weight(1f))
            NumpadDigit("2", shape, { onKey("2") }, Modifier.weight(1f))
            NumpadDigit("3", shape, { onKey("3") }, Modifier.weight(1f))
            NumpadOperator("+", shape, { onKey("+") }, Modifier.weight(1f))
        }
        // Row 5: ↓  0  .  =
        Row(Modifier.fillMaxWidth()) {
            NumpadAction(null, shape, onFocusNext, Modifier.weight(1f)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Focus next row", modifier = Modifier.size(20.dp))
            }
            NumpadDigit("0", shape, { onKey("0") }, Modifier.weight(1f))
            NumpadDigit(".", shape, { onKey(".") }, Modifier.weight(1f))
            NumpadEquals(shape, onEnter, Modifier.weight(1f))
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
            .height(52.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NumpadOperator(
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
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NumpadAction(
    label: String?,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .padding(3.dp)
            .height(52.dp)
    ) {
        content?.invoke() ?: Text(label ?: "", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun NumpadEquals(
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
        Text("=", style = MaterialTheme.typography.titleLarge)
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
