package com.acalc.ui.viewmodel

import com.acalc.domain.LengthUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.UnitCategory
import com.acalc.domain.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConverterViewModelTest {

    private lateinit var viewModel: ConverterViewModel

    @Before
    fun setUp() {
        viewModel = ConverterViewModel()
    }

    // Default state — LENGTH category, first row active
    @Test
    fun test_defaultState_isLength() {
        val state = viewModel.state.value
        assertEquals(UnitCategory.LENGTH, state.selectedCategory)
        assertEquals(0, state.activeRowIndex)
        assertTrue("rows should not be empty", state.rows.isNotEmpty())
        assertTrue("all rows empty initially", state.rows.all { it.value.isEmpty() })
    }

    // Typing a number into active row drives conversion into other rows
    @Test
    fun test_numpadDigit_triggerConversion() {
        // Default: first row is MM, third is INCH; 25.4mm = 1 inch
        // Type 2, 5, ., 4 into active row (MM)
        "2544".forEach { c -> viewModel.onNumpadKey(c.toString()) }
        // Undo extra digits and get 25.4: reset and retype
        viewModel.onNumpadKey("C")
        viewModel.onNumpadKey("2")
        viewModel.onNumpadKey("5")
        viewModel.onNumpadKey(".")
        viewModel.onNumpadKey("4")
        val state = viewModel.state.value
        val mmRow = state.rows[0]
        assertEquals("25.4", mmRow.value)
        // The INCH row (index 2 by default) should be non-empty
        val inchRow = state.rows[2]
        assertTrue("INCH row should be converted", inchRow.value.isNotEmpty())
        val inchValue = inchRow.value.toDouble()
        assertEquals(1.0, inchValue, 1e-6)
    }

    // Decimal after operator — the core decimal bug fix
    @Test
    fun test_decimalAfterOperator_isAllowed() {
        // Type "0.2-0.12" via numpad and verify it evaluates to ~0.08
        viewModel.onNumpadKey(".")  // → "0."
        viewModel.onNumpadKey("2")  // → "0.2"
        viewModel.onNumpadKey("-")  // → "0.2-"
        viewModel.onNumpadKey(".")  // → "0.2-0." (was blocked before fix)
        viewModel.onNumpadKey("1")  // → "0.2-0.1"
        viewModel.onNumpadKey("2")  // → "0.2-0.12"
        val state = viewModel.state.value
        val activeValue = state.rows[state.activeRowIndex].value
        assertEquals("0.2-0.12", activeValue)
        // All other rows should have converted values (non-empty)
        val otherRows = state.rows.filterIndexed { i, _ -> i != state.activeRowIndex }
        assertTrue("Other rows should be non-empty after evaluation", otherRows.any { it.value.isNotEmpty() })
    }

    // Dot on empty → "0."
    @Test
    fun test_dotOnEmpty_givesZeroDot() {
        viewModel.onNumpadKey(".")
        val value = viewModel.state.value.rows[0].value
        assertEquals("0.", value)
    }

    // Backspace
    @Test
    fun test_backspace_removesLastChar() {
        viewModel.onNumpadKey("5")
        viewModel.onNumpadKey("3")
        viewModel.onNumpadKey("⌫")
        assertEquals("5", viewModel.state.value.rows[0].value)
    }

    // Clear
    @Test
    fun test_clear_emptiesAllRows() {
        viewModel.onNumpadKey("5")
        viewModel.onNumpadKey("C")
        val state = viewModel.state.value
        assertTrue("All rows cleared", state.rows.all { it.value.isEmpty() })
    }

    // Category switch preserves previous state and shows fresh state
    @Test
    fun test_categorySwitchSavesAndRestores() {
        viewModel.onNumpadKey("1")
        viewModel.onNumpadKey("0")
        viewModel.onNumpadKey("0")
        val lengthRows = viewModel.state.value.rows.map { it.value }

        viewModel.onCategorySelected(UnitCategory.WEIGHT)
        assertEquals(UnitCategory.WEIGHT, viewModel.state.value.selectedCategory)

        viewModel.onCategorySelected(UnitCategory.LENGTH)
        assertEquals(UnitCategory.LENGTH, viewModel.state.value.selectedCategory)
        assertEquals(lengthRows, viewModel.state.value.rows.map { it.value })
    }

    // Temperature conversion: 100 C → 212 F
    @Test
    fun test_temperature_100CtoF_is212() {
        viewModel.onCategorySelected(UnitCategory.TEMPERATURE)
        // First row is CELSIUS by default; third row (index 0) active
        viewModel.onNumpadKey("1")
        viewModel.onNumpadKey("0")
        viewModel.onNumpadKey("0")
        val state = viewModel.state.value
        // FAHRENHEIT row
        val fahrenheitRowIndex = state.rows.indexOfFirst {
            it.unitIndex == TempUnit.FAHRENHEIT.ordinal
        }
        assertTrue("Fahrenheit row exists", fahrenheitRowIndex >= 0)
        val fValue = state.rows[fahrenheitRowIndex].value
        assertEquals(212.0, fValue.toDouble(), 1e-6)
    }

    // Row activation
    @Test
    fun test_onRowActivated_changesActiveIndex() {
        viewModel.onRowActivated(2)
        assertEquals(2, viewModel.state.value.activeRowIndex)
    }

    // Unary minus on empty → "-"
    @Test
    fun test_unaryMinus_onEmpty() {
        viewModel.onNumpadKey("±")
        assertEquals("-", viewModel.state.value.rows[0].value)
    }

    // Swap top two rows
    @Test
    fun test_swap_swapsFirstTwoRows() {
        val state = viewModel.state.value
        val unit0 = state.rows[0].unitIndex
        val unit1 = state.rows[1].unitIndex
        viewModel.onSwap()
        val after = viewModel.state.value
        assertEquals(unit1, after.rows[0].unitIndex)
        assertEquals(unit0, after.rows[1].unitIndex)
    }

    // getUnitsForCategory returns correct counts
    @Test
    fun test_getUnitsForCategory_lengthCount() {
        val units = viewModel.getUnitsForCategory(UnitCategory.LENGTH)
        assertTrue("LENGTH should have multiple units", units.size >= 4)
    }
}
