package com.acalc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.acalc.domain.LengthUnit
import com.acalc.domain.UnitCategory
import com.acalc.ui.AcalcTheme
import com.acalc.ui.viewmodel.ConverterState
import com.acalc.ui.viewmodel.ConverterViewModel
import com.acalc.ui.viewmodel.UnitPair

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

    ConverterContent(
        state = state,
        onTopChanged = vm::onTopChanged,
        onBottomChanged = vm::onBottomChanged,
        onCategorySelected = vm::onCategorySelected,
        onTopUnitChanged = vm::onTopUnitChanged,
        onBottomUnitChanged = vm::onBottomUnitChanged,
        unitOptions = vm.getUnitsForCategory(state.selectedCategory),
        topUnitDisplay = vm.getTopUnitDisplayName(),
        bottomUnitDisplay = vm.getBottomUnitDisplayName(),
        modifier = modifier
    )
}

@Composable
private fun ConverterContent(
    state: ConverterState,
    onTopChanged: (String) -> Unit,
    onBottomChanged: (String) -> Unit,
    onCategorySelected: (UnitCategory) -> Unit,
    onTopUnitChanged: (Int) -> Unit,
    onBottomUnitChanged: (Int) -> Unit,
    unitOptions: List<Pair<String, String>>,
    topUnitDisplay: String,
    bottomUnitDisplay: String,
    modifier: Modifier = Modifier
) {
    val categories = UnitCategory.entries
    val selectedIndex = categories.indexOf(state.selectedCategory)

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Category tab row
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onCategorySelected(category) },
                    text = { Text(CATEGORY_LABELS[category] ?: category.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Top conversion row
        ConversionRow(
            inputValue = state.topInput,
            onValueChange = onTopChanged,
            unitDisplay = topUnitDisplay,
            unitOptions = unitOptions,
            onUnitSelected = onTopUnitChanged,
            label = "From",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Bottom conversion row
        ConversionRow(
            inputValue = state.bottomInput,
            onValueChange = onBottomChanged,
            unitDisplay = bottomUnitDisplay,
            unitOptions = unitOptions,
            onUnitSelected = onBottomUnitChanged,
            label = "To",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ConversionRow(
    inputValue: String,
    onValueChange: (String) -> Unit,
    unitDisplay: String,
    unitOptions: List<Pair<String, String>>,
    onUnitSelected: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text("Enter value") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            UnitDropdown(
                selectedDisplay = unitDisplay,
                options = unitOptions,
                onOptionSelected = onUnitSelected,
                modifier = Modifier.width(96.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selectedDisplay: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDisplay,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .width(96.dp),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, (_, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConverterContentPreview() {
    AcalcTheme {
        val defaultState = ConverterState(
            selectedCategory = UnitCategory.LENGTH,
            topInput = "25.4",
            bottomInput = "1",
            units = UnitPair.Length(LengthUnit.MM, LengthUnit.INCH)
        )
        val unitOptions = LengthUnit.entries.map { it.name to it.displayName }
        ConverterContent(
            state = defaultState,
            onTopChanged = {},
            onBottomChanged = {},
            onCategorySelected = {},
            onTopUnitChanged = {},
            onBottomUnitChanged = {},
            unitOptions = unitOptions,
            topUnitDisplay = "mm",
            bottomUnitDisplay = "in"
        )
    }
}
