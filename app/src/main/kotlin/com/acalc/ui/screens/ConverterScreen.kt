package com.acalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
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
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val vm = viewModel<ConverterViewModel> { ConverterViewModel.create(app) }
    val state by vm.state.collectAsStateWithLifecycle()
    val unitOptions = vm.getUnitsForCategory(state.selectedCategory)
    var unitPickerRowIndex by remember { mutableStateOf<Int?>(null) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = modifier.fillMaxSize()) {

        // Category tabs — always at top full width
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

        val liveResult = vm.getLiveResult(state)
        val hint = vm.getConversionHint(state)

        if (isLandscape) {
            // Landscape: rows + hint on left, numpad on right
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    state.rows.forEachIndexed { index, row ->
                        ConverterRowItem(
                            unitName = unitOptions.getOrNull(row.unitIndex)?.second ?: "",
                            value = row.value,
                            cursorPos = row.cursorPos,
                            isActive = index == state.activeRowIndex,
                            liveResult = if (index == state.activeRowIndex) liveResult else null,
                            onTap = { vm.onRowActivated(index) },
                            onUnitTap = { unitPickerRowIndex = index },
                            onCursorMoved = { newPos -> vm.onCursorMoved(index, newPos) },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                        HorizontalDivider()
                    }
                    if (hint.isNotEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }
                androidx.compose.material3.VerticalDivider()
                ConverterNumpad(
                    onKey = vm::onNumpadKey,
                    onEnter = vm::onEnter,
                    onFocusNext = vm::onFocusNextRow,
                    fillHeight = true,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            // Portrait: rows stacked, hint, numpad below
            state.rows.forEachIndexed { index, row ->
                ConverterRowItem(
                    unitName = unitOptions.getOrNull(row.unitIndex)?.second ?: "",
                    value = row.value,
                    cursorPos = row.cursorPos,
                    isActive = index == state.activeRowIndex,
                    liveResult = if (index == state.activeRowIndex) liveResult else null,
                    onTap = { vm.onRowActivated(index) },
                    onUnitTap = { unitPickerRowIndex = index },
                    onCursorMoved = { newPos -> vm.onCursorMoved(index, newPos) },
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(max = 80.dp)
                )
                HorizontalDivider()
            }
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            HorizontalDivider()
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
    cursorPos: Int,
    isActive: Boolean,
    liveResult: String?,
    onTap: () -> Unit,
    onUnitTap: () -> Unit,
    onCursorMoved: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val bgColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val keyboardController = LocalSoftwareKeyboardController.current

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
            // Shared interaction state for expression field (needed in both layout branches)
            val interactionSourceExpr = remember { MutableInteractionSource() }
            LaunchedEffect(interactionSourceExpr) {
                interactionSourceExpr.interactions.collect { keyboardController?.hide() }
            }
            val tfvExpr = if (liveResult != null && liveResult != value) TextFieldValue(
                text = value,
                selection = TextRange(cursorPos.coerceIn(0, value.length))
            ) else null

            if (liveResult != null && liveResult != value) {
                // Expression mode — choose 2-line or compact 1-line based on available height.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Mode 1 — full:          ≥ 52dp  → 2 lines, full-size text
                    // Mode 2 — scaled 2-line: ≥ 34dp  → 2 lines, smaller text
                    // Mode 3 — single line:   < 34dp  → 1 line, smallest text
                    when {
                        maxHeight >= 52.dp -> {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicTextField(
                                    value = tfvExpr!!,
                                    onValueChange = { newTfv ->
                                        if (newTfv.text == value) onCursorMoved(newTfv.selection.start)
                                        keyboardController?.hide()
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        textAlign = TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    interactionSource = interactionSourceExpr,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "= $liveResult",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        maxHeight >= 34.dp -> {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicTextField(
                                    value = tfvExpr!!,
                                    onValueChange = { newTfv ->
                                        if (newTfv.text == value) onCursorMoved(newTfv.selection.start)
                                        keyboardController?.hide()
                                    },
                                    textStyle = MaterialTheme.typography.labelSmall.copy(
                                        textAlign = TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    interactionSource = interactionSourceExpr,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "= $liveResult",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicTextField(
                                    value = tfvExpr!!,
                                    onValueChange = { newTfv ->
                                        if (newTfv.text == value) onCursorMoved(newTfv.selection.start)
                                        keyboardController?.hide()
                                    },
                                    textStyle = MaterialTheme.typography.labelSmall.copy(
                                        textAlign = TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    interactionSource = interactionSourceExpr,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = " = $liveResult",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Single-line value display (active or inactive).
                val displayValue = if (value.isEmpty() && isActive) "" else value
                val tfv = TextFieldValue(
                    text = displayValue,
                    selection = TextRange(cursorPos.coerceIn(0, displayValue.length))
                )
                val interactionSourceVal = remember { MutableInteractionSource() }
                LaunchedEffect(interactionSourceVal) {
                    interactionSourceVal.interactions.collect { keyboardController?.hide() }
                }
                BasicTextField(
                    value = tfv,
                    onValueChange = { newTfv ->
                        if (newTfv.text == displayValue) {
                            onCursorMoved(newTfv.selection.start)
                        }
                        keyboardController?.hide()
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.End,
                        color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    interactionSource = interactionSourceVal,
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
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Column(modifier = modifier.padding(4.dp)) {
        val rowMod = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()
        // Row 1: C  ⌫  ±  ÷
        Row(rowMod) {
            NumpadAction("C",  shape, { onKey("C") },  Modifier.weight(1f))
            NumpadAction(null, shape, { onKey("⌫") }, Modifier.weight(1f)) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(20.dp))
            }
            NumpadAction("±", shape, { onKey("±") }, Modifier.weight(1f))
            NumpadOperator("÷", shape, { onKey("÷") }, Modifier.weight(1f))
        }
        // Row 2: 7  8  9  ×
        Row(rowMod) {
            NumpadDigit("7", shape, { onKey("7") }, Modifier.weight(1f))
            NumpadDigit("8", shape, { onKey("8") }, Modifier.weight(1f))
            NumpadDigit("9", shape, { onKey("9") }, Modifier.weight(1f))
            NumpadOperator("×", shape, { onKey("×") }, Modifier.weight(1f))
        }
        // Row 3: 4  5  6  -
        Row(rowMod) {
            NumpadDigit("4", shape, { onKey("4") }, Modifier.weight(1f))
            NumpadDigit("5", shape, { onKey("5") }, Modifier.weight(1f))
            NumpadDigit("6", shape, { onKey("6") }, Modifier.weight(1f))
            NumpadOperator("-", shape, { onKey("-") }, Modifier.weight(1f))
        }
        // Row 4: 1  2  3  +
        Row(rowMod) {
            NumpadDigit("1", shape, { onKey("1") }, Modifier.weight(1f))
            NumpadDigit("2", shape, { onKey("2") }, Modifier.weight(1f))
            NumpadDigit("3", shape, { onKey("3") }, Modifier.weight(1f))
            NumpadOperator("+", shape, { onKey("+") }, Modifier.weight(1f))
        }
        // Row 5: ↓  0  .  =
        Row(rowMod) {
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
