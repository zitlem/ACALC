package com.acalc.ui.viewmodel

import com.acalc.domain.LengthUnit
import com.acalc.domain.PressureUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.UnitCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory [ConverterStorage] so persistence can be asserted without Android. */
private class FakeConverterStorage(var saved: ConverterSavedState? = null) : ConverterStorage {
    var saveCount = 0
    override fun load(): ConverterSavedState? = saved
    override fun save(saved: ConverterSavedState) {
        this.saved = saved
        saveCount++
    }
}

class ConverterViewModelStateTest {

    private lateinit var storage: FakeConverterStorage
    private lateinit var vm: ConverterViewModel

    @Before
    fun setUp() {
        storage = FakeConverterStorage()
        vm = ConverterViewModel(storage)
    }

    private fun type(keys: String) = keys.forEach { vm.onNumpadKey(it.toString()) }

    private fun rowFor(unitOrdinal: Int) =
        vm.state.value.rows.first { it.unitIndex == unitOrdinal }

    // ── Persistence ──

    @Test
    fun `typing persists state through storage`() {
        type("12")
        assertTrue(storage.saveCount > 0)
        assertEquals(UnitCategory.LENGTH, storage.saved!!.currentCategory)
        assertEquals("12", storage.saved!!.categoryMap[UnitCategory.LENGTH]!!.rows[0].value)
    }

    @Test
    fun `saved state is restored on construction`() {
        type("100")
        val restored = ConverterViewModel(storage)
        assertEquals(UnitCategory.LENGTH, restored.state.value.selectedCategory)
        assertEquals("100", restored.state.value.rows[0].value)
    }

    @Test
    fun `restores the category that was last open`() {
        vm.onCategorySelected(UnitCategory.PRESSURE)
        type("5")
        val restored = ConverterViewModel(storage)
        assertEquals(UnitCategory.PRESSURE, restored.state.value.selectedCategory)
        assertEquals("5", restored.state.value.rows[0].value)
    }

    @Test
    fun `restoring a category with no stored rows falls back to defaults`() {
        storage.saved = ConverterSavedState(UnitCategory.ENERGY, emptyMap())
        val restored = ConverterViewModel(storage)
        assertEquals(UnitCategory.ENERGY, restored.state.value.selectedCategory)
        assertTrue(restored.state.value.rows.isNotEmpty())
        assertTrue(restored.state.value.rows.all { it.value.isEmpty() })
    }

    @Test
    fun `each category keeps its own independent values`() {
        type("50")
        vm.onCategorySelected(UnitCategory.WEIGHT)
        type("7")
        assertEquals("7", vm.state.value.rows[0].value)

        vm.onCategorySelected(UnitCategory.LENGTH)
        assertEquals("50", vm.state.value.rows[0].value)

        vm.onCategorySelected(UnitCategory.WEIGHT)
        assertEquals("7", vm.state.value.rows[0].value)
    }

    // ── Conversion propagation ──

    @Test
    fun `1 inch typed into the inch row yields 25 point 4 mm`() {
        vm.onRowActivated(2) // INCH row by default layout
        assertEquals(LengthUnit.INCH.ordinal, vm.state.value.rows[2].unitIndex)
        type("1")
        assertEquals(25.4, rowFor(LengthUnit.MM.ordinal).value.toDouble(), 1e-9)
    }

    @Test
    fun `changing a row unit reconverts from the active row`() {
        type("1000") // 1000 mm
        vm.onUnitChanged(1, LengthUnit.M.ordinal)
        assertEquals(1.0, vm.state.value.rows[1].value.toDouble(), 1e-9)
    }

    @Test
    fun `changing a unit with an empty active row leaves values empty`() {
        vm.onUnitChanged(1, LengthUnit.M.ordinal)
        assertTrue(vm.state.value.rows.all { it.value.isEmpty() })
    }

    @Test
    fun `negative temperature propagates to the other scales`() {
        vm.onCategorySelected(UnitCategory.TEMPERATURE)
        vm.onNumpadKey("±")
        type("40")
        assertEquals(-40.0, rowFor(TempUnit.FAHRENHEIT.ordinal).value.toDouble(), 1e-9)
        assertEquals(233.15, rowFor(TempUnit.KELVIN.ordinal).value.toDouble(), 1e-9)
    }

    @Test
    fun `pressure conversion bar to psi`() {
        vm.onCategorySelected(UnitCategory.PRESSURE)
        vm.onUnitChanged(0, PressureUnit.BAR.ordinal)
        type("1")
        assertEquals(14.5037738, rowFor(PressureUnit.PSI.ordinal).value.toDouble(), 1e-6)
    }

    // ── Expression entry ──

    @Test
    fun `getLiveResult previews an unevaluated expression`() {
        type("2")
        vm.onNumpadKey("+")
        type("3")
        assertEquals("5", vm.getLiveResult(vm.state.value))
    }

    @Test
    fun `getLiveResult is null for a plain number`() {
        type("42")
        assertNull(vm.getLiveResult(vm.state.value))
    }

    @Test
    fun `getLiveResult is null for an empty row`() {
        assertNull(vm.getLiveResult(vm.state.value))
    }

    @Test
    fun `getLiveResult is null for an incomplete expression`() {
        type("2")
        vm.onNumpadKey("+")
        assertNull(vm.getLiveResult(vm.state.value))
    }

    @Test
    fun `onEnter collapses the expression to its numeric value`() {
        type("2")
        vm.onNumpadKey("+")
        type("3")
        vm.onEnter()
        assertEquals("5", vm.state.value.rows[0].value)
        assertNull(vm.getLiveResult(vm.state.value))
    }

    @Test
    fun `onExprCalcCommit accepts display operators`() {
        vm.onExprCalcCommit("6×7")
        assertEquals("42", vm.state.value.rows[0].value)
    }

    @Test
    fun `onExprCalcCommit ignores an unparseable expression`() {
        type("9")
        vm.onExprCalcCommit("not a number")
        assertEquals("9", vm.state.value.rows[0].value)
    }

    @Test
    fun `activating another row commits the pending expression`() {
        type("2")
        vm.onNumpadKey("+")
        type("3")
        vm.onRowActivated(1)
        assertEquals("5", vm.state.value.rows[0].value)
        assertEquals(1, vm.state.value.activeRowIndex)
    }

    @Test
    fun `activating the row that is already active is a no-op`() {
        type("7")
        val before = vm.state.value
        vm.onRowActivated(0)
        assertEquals(before, vm.state.value)
    }

    // ── Numpad guards ──

    @Test
    fun `leading zero is replaced by the next digit`() {
        type("0")
        type("5")
        assertEquals("5", vm.state.value.rows[0].value)
    }

    @Test
    fun `operator is not accepted on an empty row`() {
        vm.onNumpadKey("×")
        assertEquals("", vm.state.value.rows[0].value)
        vm.onNumpadKey("+")
        assertEquals("", vm.state.value.rows[0].value)
    }

    @Test
    fun `duplicate operators are collapsed`() {
        type("5")
        vm.onNumpadKey("+")
        vm.onNumpadKey("-")
        assertEquals("5-", vm.state.value.rows[0].value)
    }

    @Test
    fun `consecutive multiply presses are ignored`() {
        type("5")
        vm.onNumpadKey("×")
        vm.onNumpadKey("×")
        assertEquals("5*", vm.state.value.rows[0].value)
    }

    @Test
    fun `sign toggle adds and removes the leading minus`() {
        type("8")
        vm.onNumpadKey("±")
        assertEquals("-8", vm.state.value.rows[0].value)
        vm.onNumpadKey("±")
        assertEquals("8", vm.state.value.rows[0].value)
    }

    @Test
    fun `second decimal point in the same token is rejected`() {
        type("1")
        vm.onNumpadKey(".")
        vm.onNumpadKey(".")
        assertEquals("1.", vm.state.value.rows[0].value)
    }

    @Test
    fun `backspace at the start of a row is a no-op`() {
        vm.onNumpadKey("⌫")
        assertEquals("", vm.state.value.rows[0].value)
    }

    @Test
    fun `clearing the active row clears every other row`() {
        type("123")
        assertTrue(vm.state.value.rows.drop(1).all { it.value.isNotEmpty() })
        vm.onNumpadKey("C")
        assertTrue(vm.state.value.rows.all { it.value.isEmpty() })
    }

    // ── Cursor ──

    @Test
    fun `onCursorMoved clamps to the row value length`() {
        type("12")
        vm.onCursorMoved(0, 99)
        assertEquals(2, vm.state.value.rows[0].cursorPos)
        vm.onCursorMoved(0, -3)
        assertEquals(0, vm.state.value.rows[0].cursorPos)
    }

    @Test
    fun `onCursorMoved on another row activates it`() {
        vm.onCursorMoved(2, 0)
        assertEquals(2, vm.state.value.activeRowIndex)
    }

    @Test
    fun `onCursorMoved on an out-of-range row is ignored`() {
        vm.onCursorMoved(99, 0)
        assertEquals(0, vm.state.value.activeRowIndex)
    }

    @Test
    fun `digits are inserted at the cursor`() {
        type("13")
        vm.onCursorMoved(0, 1)
        vm.onNumpadKey("2")
        assertEquals("123", vm.state.value.rows[0].value)
    }

    // ── Focus movement ──

    @Test
    fun `focus next steps through each row in turn`() {
        val count = vm.state.value.rows.size
        for (expected in 1 until count) {
            vm.onFocusNextRow()
            assertEquals(expected, vm.state.value.activeRowIndex)
        }
    }

    /** Wrapping is what makes a separate "previous row" key unnecessary. */
    @Test
    fun `focus next wraps around to the first row`() {
        val count = vm.state.value.rows.size
        repeat(count) { vm.onFocusNextRow() }
        assertEquals(0, vm.state.value.activeRowIndex)

        // And the last row is reachable by going forwards.
        repeat(count - 1) { vm.onFocusNextRow() }
        assertEquals(count - 1, vm.state.value.activeRowIndex)
    }

    // ── Hints and unit lists ──

    @Test
    fun `conversion hint names both directions`() {
        val hint = vm.getConversionHint(vm.state.value)
        assertTrue(hint, hint.contains("Millimeter"))
        assertTrue(hint, hint.contains("Centimeter"))
        assertTrue(hint, hint.contains("10"))
    }

    @Test
    fun `conversion hint is empty for temperature`() {
        vm.onCategorySelected(UnitCategory.TEMPERATURE)
        assertEquals("", vm.getConversionHint(vm.state.value))
    }

    @Test
    fun `conversion hint is empty when both rows use the same unit`() {
        vm.onUnitChanged(1, LengthUnit.MM.ordinal)
        assertEquals("", vm.getConversionHint(vm.state.value))
    }

    @Test
    fun `every category exposes a non-empty unit list and default rows`() {
        UnitCategory.entries.forEach { category ->
            val units = vm.getUnitsForCategory(category)
            assertTrue("$category has no units", units.isNotEmpty())
            vm.onCategorySelected(category)
            val rows = vm.state.value.rows
            assertTrue("$category has no rows", rows.isNotEmpty())
            rows.forEach { row ->
                assertNotNull(
                    "$category row unit index ${row.unitIndex} is out of range",
                    units.getOrNull(row.unitIndex)
                )
            }
        }
    }

    @Test
    fun `every category converts a typed value into all of its rows`() {
        UnitCategory.entries.forEach { category ->
            val fresh = ConverterViewModel()
            fresh.onCategorySelected(category)
            fresh.onNumpadKey("1")
            val rows = fresh.state.value.rows
            rows.forEach { row ->
                assertTrue(
                    "$category left a row empty: $rows",
                    row.value.isNotEmpty()
                )
            }
        }
    }
}
