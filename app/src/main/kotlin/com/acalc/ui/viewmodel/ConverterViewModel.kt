package com.acalc.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acalc.domain.AreaUnit
import com.acalc.domain.ConversionEngine
import com.acalc.domain.ExpressionEvaluator
import com.acalc.domain.LengthUnit
import com.acalc.domain.SpeedUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.UnitCategory
import com.acalc.domain.VolumeUnit
import com.acalc.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.RoundingMode

data class ConverterRow(
    val unitIndex: Int,
    val value: String
)

data class ConverterState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val rows: List<ConverterRow> = defaultRowsFor(UnitCategory.LENGTH),
    val activeRowIndex: Int = 0
)

private fun defaultRowsFor(category: UnitCategory): List<ConverterRow> = when (category) {
    UnitCategory.LENGTH      -> listOf(ConverterRow(LengthUnit.MM.ordinal, ""), ConverterRow(LengthUnit.INCH.ordinal, ""))
    UnitCategory.WEIGHT      -> listOf(ConverterRow(WeightUnit.KG.ordinal, ""), ConverterRow(WeightUnit.LB.ordinal, ""))
    UnitCategory.VOLUME      -> listOf(ConverterRow(VolumeUnit.ML.ordinal, ""), ConverterRow(VolumeUnit.FL_OZ.ordinal, ""))
    UnitCategory.TEMPERATURE -> listOf(ConverterRow(TempUnit.CELSIUS.ordinal, ""), ConverterRow(TempUnit.FAHRENHEIT.ordinal, ""))
    UnitCategory.AREA        -> listOf(ConverterRow(AreaUnit.SQ_M.ordinal, ""), ConverterRow(AreaUnit.SQ_FT.ordinal, ""))
    UnitCategory.SPEED       -> listOf(ConverterRow(SpeedUnit.KM_PER_H.ordinal, ""), ConverterRow(SpeedUnit.MPH.ordinal, ""))
}

class ConverterViewModel : ViewModel() {

    private val evaluator = ExpressionEvaluator()
    private val engine = ConversionEngine()

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state

    private val categoryStateMap: MutableMap<UnitCategory, ConverterState> = mutableMapOf()

    // MARK: — Category switching

    fun onCategorySelected(category: UnitCategory) {
        categoryStateMap[_state.value.selectedCategory] = _state.value
        _state.value = categoryStateMap[category] ?: ConverterState(
            selectedCategory = category,
            rows = defaultRowsFor(category)
        )
    }

    // MARK: — Row activation

    fun onRowActivated(index: Int) {
        _state.value = _state.value.copy(activeRowIndex = index)
    }

    // MARK: — Numpad input

    fun onNumpadKey(key: String) {
        val state = _state.value
        val activeIndex = state.activeRowIndex
        val rows = state.rows.toMutableList()
        val current = rows[activeIndex].value

        val newValue = when (key) {
            "⌫"  -> if (current.isEmpty()) "" else current.dropLast(1)
            "C"   -> ""
            "."   -> when {
                "." in current -> current
                current.isEmpty() -> "0."
                else -> "$current."
            }
            "00"  -> if (current.isEmpty()) current else "$current" + "00"
            else  -> if (current == "0") key else "$current$key"   // digit
        }

        rows[activeIndex] = rows[activeIndex].copy(value = newValue)

        if (newValue.isEmpty()) {
            val cleared = rows.mapIndexed { i, r -> if (i == activeIndex) r else r.copy(value = "") }
            _state.value = state.copy(rows = cleared)
            return
        }

        val evaluated = evaluator.evaluate(newValue) ?: run {
            _state.value = state.copy(rows = rows)
            return
        }

        recomputeFrom(state, rows, activeIndex, BigDecimal(evaluated.toString()))
    }

    // MARK: — Swap top two rows (unit + value together, values stay consistent)

    fun onSwap() {
        val state = _state.value
        if (state.rows.size < 2) return
        val rows = state.rows.toMutableList()
        val tmp = rows[0]; rows[0] = rows[1]; rows[1] = tmp
        val newActive = when (state.activeRowIndex) {
            0    -> 1
            1    -> 0
            else -> state.activeRowIndex
        }
        _state.value = state.copy(rows = rows, activeRowIndex = newActive)
    }

    // MARK: — Unit selection

    fun onUnitChanged(rowIndex: Int, unitIndex: Int) {
        val state = _state.value
        val rows = state.rows.toMutableList()
        rows[rowIndex] = rows[rowIndex].copy(unitIndex = unitIndex)
        _state.value = state.copy(rows = rows)

        val activeIndex = state.activeRowIndex
        val activeValue = rows[activeIndex].value
        if (activeValue.isEmpty()) return
        val evaluated = evaluator.evaluate(activeValue) ?: return
        recomputeFrom(_state.value, rows, activeIndex, BigDecimal(evaluated.toString()))
    }

    // MARK: — Row management

    fun onAddRow() {
        val state = _state.value
        val newRow = ConverterRow(unitIndex = 0, value = "")
        val rows = (state.rows + newRow).toMutableList()
        _state.value = state.copy(rows = rows)

        val activeIndex = state.activeRowIndex
        val activeValue = state.rows[activeIndex].value
        if (activeValue.isEmpty()) return
        val evaluated = evaluator.evaluate(activeValue) ?: return
        recomputeFrom(_state.value, rows, activeIndex, BigDecimal(evaluated.toString()))
    }

    fun onExprCalcCommit(expression: String) {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("x", "*")
        val evaluated = evaluator.evaluate(sanitized) ?: return
        val state = _state.value
        val activeIndex = state.activeRowIndex
        val rows = state.rows.toMutableList()
        val displayValue = formatConverted(BigDecimal(evaluated.toString()))
        rows[activeIndex] = rows[activeIndex].copy(value = displayValue)
        recomputeFrom(state, rows, activeIndex, BigDecimal(evaluated.toString()))
    }

    fun onRemoveRow(rowIndex: Int) {
        val state = _state.value
        if (state.rows.size <= 2) return
        val newRows = state.rows.toMutableList()
        newRows.removeAt(rowIndex)
        val newActive = when {
            state.activeRowIndex == rowIndex -> 0
            state.activeRowIndex > rowIndex  -> state.activeRowIndex - 1
            else                             -> state.activeRowIndex
        }
        _state.value = state.copy(rows = newRows, activeRowIndex = newActive)
    }

    // MARK: — UI helpers

    fun getUnitsForCategory(category: UnitCategory): List<Pair<String, String>> = when (category) {
        UnitCategory.LENGTH      -> LengthUnit.entries.map { it.name to it.displayName }
        UnitCategory.WEIGHT      -> WeightUnit.entries.map { it.name to it.displayName }
        UnitCategory.VOLUME      -> VolumeUnit.entries.map { it.name to it.displayName }
        UnitCategory.TEMPERATURE -> TempUnit.entries.map { it.name to it.displayName }
        UnitCategory.AREA        -> AreaUnit.entries.map { it.name to it.displayName }
        UnitCategory.SPEED       -> SpeedUnit.entries.map { it.name to it.displayName }
    }

    fun getConversionHint(state: ConverterState): String {
        if (state.rows.size < 2) return ""
        val units = getUnitsForCategory(state.selectedCategory)
        val fromName = units.getOrNull(state.rows[0].unitIndex)?.second ?: return ""
        val toName   = units.getOrNull(state.rows[1].unitIndex)?.second ?: return ""
        val rate = runCatching {
            convertBetween(BigDecimal.ONE, state.rows[0].unitIndex, state.rows[1].unitIndex, state.selectedCategory)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }.getOrNull() ?: return ""
        return "* 1 $fromName = $rate $toName"
    }

    // MARK: — Private helpers

    private fun recomputeFrom(
        state: ConverterState,
        rows: MutableList<ConverterRow>,
        sourceIndex: Int,
        value: BigDecimal
    ) {
        val sourceUnitIndex = rows[sourceIndex].unitIndex
        val finalRows = rows.mapIndexed { i, r ->
            if (i == sourceIndex) r
            else {
                val converted = convertBetween(value, sourceUnitIndex, r.unitIndex, state.selectedCategory)
                r.copy(value = formatConverted(converted))
            }
        }
        _state.value = state.copy(rows = finalRows)
    }

    private fun convertBetween(
        value: BigDecimal,
        fromIndex: Int,
        toIndex: Int,
        category: UnitCategory
    ): BigDecimal = when (category) {
        UnitCategory.LENGTH      -> engine.convert(value, LengthUnit.entries[fromIndex], LengthUnit.entries[toIndex])
        UnitCategory.WEIGHT      -> engine.convert(value, WeightUnit.entries[fromIndex], WeightUnit.entries[toIndex])
        UnitCategory.VOLUME      -> engine.convert(value, VolumeUnit.entries[fromIndex], VolumeUnit.entries[toIndex])
        UnitCategory.TEMPERATURE -> engine.convert(value, TempUnit.entries[fromIndex], TempUnit.entries[toIndex])
        UnitCategory.AREA        -> engine.convert(value, AreaUnit.entries[fromIndex], AreaUnit.entries[toIndex])
        UnitCategory.SPEED       -> engine.convert(value, SpeedUnit.entries[fromIndex], SpeedUnit.entries[toIndex])
    }

    private fun formatConverted(value: BigDecimal): String =
        value.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
