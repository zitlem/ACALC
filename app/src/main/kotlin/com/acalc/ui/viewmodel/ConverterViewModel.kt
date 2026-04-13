package com.acalc.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acalc.domain.AngleUnit
import com.acalc.domain.AreaUnit
import com.acalc.domain.ConversionEngine
import com.acalc.domain.DataUnit
import com.acalc.domain.EnergyUnit
import com.acalc.domain.ExpressionEvaluator
import com.acalc.domain.ForceUnit
import com.acalc.domain.LengthUnit
import com.acalc.domain.PowerUnit
import com.acalc.domain.PressureUnit
import com.acalc.domain.SpeedUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.TimeUnit
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
    UnitCategory.TRIANGLE    -> emptyList()
    UnitCategory.LENGTH      -> listOf(
        ConverterRow(LengthUnit.MM.ordinal, ""),
        ConverterRow(LengthUnit.CM.ordinal, ""),
        ConverterRow(LengthUnit.INCH.ordinal, ""),
        ConverterRow(LengthUnit.FOOT.ordinal, "")
    )
    UnitCategory.WEIGHT      -> listOf(
        ConverterRow(WeightUnit.G.ordinal, ""),
        ConverterRow(WeightUnit.KG.ordinal, ""),
        ConverterRow(WeightUnit.OZ.ordinal, ""),
        ConverterRow(WeightUnit.LB.ordinal, "")
    )
    UnitCategory.VOLUME      -> listOf(
        ConverterRow(VolumeUnit.ML.ordinal, ""),
        ConverterRow(VolumeUnit.L.ordinal, ""),
        ConverterRow(VolumeUnit.FL_OZ.ordinal, ""),
        ConverterRow(VolumeUnit.GALLON.ordinal, "")
    )
    UnitCategory.TEMPERATURE -> listOf(
        ConverterRow(TempUnit.CELSIUS.ordinal, ""),
        ConverterRow(TempUnit.FAHRENHEIT.ordinal, ""),
        ConverterRow(TempUnit.KELVIN.ordinal, "")
    )
    UnitCategory.AREA        -> listOf(
        ConverterRow(AreaUnit.SQ_MM.ordinal, ""),
        ConverterRow(AreaUnit.SQ_M.ordinal, ""),
        ConverterRow(AreaUnit.SQ_IN.ordinal, ""),
        ConverterRow(AreaUnit.SQ_FT.ordinal, "")
    )
    UnitCategory.SPEED       -> listOf(
        ConverterRow(SpeedUnit.M_PER_S.ordinal, ""),
        ConverterRow(SpeedUnit.KM_PER_H.ordinal, ""),
        ConverterRow(SpeedUnit.MPH.ordinal, ""),
        ConverterRow(SpeedUnit.KNOTS.ordinal, "")
    )
    UnitCategory.TIME        -> listOf(
        ConverterRow(TimeUnit.SECOND.ordinal, ""),
        ConverterRow(TimeUnit.MINUTE.ordinal, ""),
        ConverterRow(TimeUnit.HOUR.ordinal, ""),
        ConverterRow(TimeUnit.DAY.ordinal, "")
    )
    UnitCategory.FORCE       -> listOf(
        ConverterRow(ForceUnit.NEWTON.ordinal, ""),
        ConverterRow(ForceUnit.KILONEWTON.ordinal, ""),
        ConverterRow(ForceUnit.KG_FORCE.ordinal, ""),
        ConverterRow(ForceUnit.LB_FORCE.ordinal, "")
    )
    UnitCategory.PRESSURE    -> listOf(
        ConverterRow(PressureUnit.PASCAL.ordinal, ""),
        ConverterRow(PressureUnit.BAR.ordinal, ""),
        ConverterRow(PressureUnit.ATMOSPHERE.ordinal, ""),
        ConverterRow(PressureUnit.PSI.ordinal, "")
    )
    UnitCategory.ENERGY      -> listOf(
        ConverterRow(EnergyUnit.JOULE.ordinal, ""),
        ConverterRow(EnergyUnit.KILOJOULE.ordinal, ""),
        ConverterRow(EnergyUnit.CALORIE.ordinal, ""),
        ConverterRow(EnergyUnit.KILOCALORIE.ordinal, "")
    )
    UnitCategory.POWER       -> listOf(
        ConverterRow(PowerUnit.WATT.ordinal, ""),
        ConverterRow(PowerUnit.KILOWATT.ordinal, ""),
        ConverterRow(PowerUnit.HP_MECH.ordinal, ""),
        ConverterRow(PowerUnit.HP_METRIC.ordinal, "")
    )
    UnitCategory.ANGLE       -> listOf(
        ConverterRow(AngleUnit.DEGREE.ordinal, ""),
        ConverterRow(AngleUnit.RADIAN.ordinal, ""),
        ConverterRow(AngleUnit.GRADIAN.ordinal, "")
    )
    UnitCategory.DATA        -> listOf(
        ConverterRow(DataUnit.BYTE.ordinal, ""),
        ConverterRow(DataUnit.KILOBYTE.ordinal, ""),
        ConverterRow(DataUnit.MEGABYTE.ordinal, ""),
        ConverterRow(DataUnit.GIGABYTE.ordinal, "")
    )
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
                // Only add decimal to the last number in the expression
                current.isEmpty() -> "0."
                current.last().isDigit() && !currentLastNumber(current).contains('.') -> "$current."
                else -> current
            }
            "±"   -> when {
                current.isEmpty() -> "-"
                current.startsWith("-") -> current.drop(1)
                else -> "-$current"
            }
            "×"   -> if (current.isEmpty() || current.last() in "+-*/") current else "$current*"
            "÷"   -> if (current.isEmpty() || current.last() in "+-*/") current else "$current/"
            "+", "-" -> when {
                current.isEmpty() && key == "-" -> "-"
                current.isEmpty() -> current
                current.last() in "+-*/" -> current.dropLast(1) + key
                else -> "$current$key"
            }
            else  -> {
                // digit key
                if (current == "0") key else "$current$key"
            }
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

    fun onEnter() {
        val state = _state.value
        val activeValue = state.rows.getOrNull(state.activeRowIndex)?.value ?: return
        onExprCalcCommit(activeValue)
    }

    fun onFocusPrevRow() {
        val s = _state.value
        if (s.rows.isEmpty()) return
        val prev = (s.activeRowIndex - 1 + s.rows.size) % s.rows.size
        _state.value = s.copy(activeRowIndex = prev)
    }

    fun onFocusNextRow() {
        val s = _state.value
        if (s.rows.isEmpty()) return
        val next = (s.activeRowIndex + 1) % s.rows.size
        _state.value = s.copy(activeRowIndex = next)
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

    // MARK: — UI helpers

    fun getUnitsForCategory(category: UnitCategory): List<Pair<String, String>> = when (category) {
        UnitCategory.TRIANGLE    -> emptyList()
        UnitCategory.LENGTH      -> LengthUnit.entries.map { it.name to it.displayName }
        UnitCategory.WEIGHT      -> WeightUnit.entries.map { it.name to it.displayName }
        UnitCategory.VOLUME      -> VolumeUnit.entries.map { it.name to it.displayName }
        UnitCategory.TEMPERATURE -> TempUnit.entries.map { it.name to it.displayName }
        UnitCategory.AREA        -> AreaUnit.entries.map { it.name to it.displayName }
        UnitCategory.SPEED       -> SpeedUnit.entries.map { it.name to it.displayName }
        UnitCategory.TIME        -> TimeUnit.entries.map { it.name to it.displayName }
        UnitCategory.FORCE       -> ForceUnit.entries.map { it.name to it.displayName }
        UnitCategory.PRESSURE    -> PressureUnit.entries.map { it.name to it.displayName }
        UnitCategory.ENERGY      -> EnergyUnit.entries.map { it.name to it.displayName }
        UnitCategory.POWER       -> PowerUnit.entries.map { it.name to it.displayName }
        UnitCategory.ANGLE       -> AngleUnit.entries.map { it.name to it.displayName }
        UnitCategory.DATA        -> DataUnit.entries.map { it.name to it.displayName }
    }

    fun getConversionHint(state: ConverterState): String {
        if (state.selectedCategory == UnitCategory.TEMPERATURE) return ""
        if (state.rows.size < 2) return ""
        val units = getUnitsForCategory(state.selectedCategory)
        val fromUnitIndex = state.rows[0].unitIndex
        val toUnitIndex   = state.rows[1].unitIndex
        if (fromUnitIndex == toUnitIndex) return ""
        val fromName = units.getOrNull(fromUnitIndex)?.second ?: return ""
        val toName   = units.getOrNull(toUnitIndex)?.second   ?: return ""
        val rate = runCatching {
            convertBetween(BigDecimal.ONE, fromUnitIndex, toUnitIndex, state.selectedCategory)
        }.getOrNull() ?: return ""
        return if (rate >= BigDecimal.ONE) {
            val formatted = rate.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            "1 $fromName = $formatted $toName"
        } else {
            val inverse = runCatching {
                convertBetween(BigDecimal.ONE, toUnitIndex, fromUnitIndex, state.selectedCategory)
            }.getOrNull() ?: return ""
            val formatted = inverse.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            "1 $toName = $formatted $fromName"
        }
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
        UnitCategory.TRIANGLE    -> value
        UnitCategory.LENGTH      -> engine.convert(value, LengthUnit.entries[fromIndex], LengthUnit.entries[toIndex])
        UnitCategory.WEIGHT      -> engine.convert(value, WeightUnit.entries[fromIndex], WeightUnit.entries[toIndex])
        UnitCategory.VOLUME      -> engine.convert(value, VolumeUnit.entries[fromIndex], VolumeUnit.entries[toIndex])
        UnitCategory.TEMPERATURE -> engine.convert(value, TempUnit.entries[fromIndex], TempUnit.entries[toIndex])
        UnitCategory.AREA        -> engine.convert(value, AreaUnit.entries[fromIndex], AreaUnit.entries[toIndex])
        UnitCategory.SPEED       -> engine.convert(value, SpeedUnit.entries[fromIndex], SpeedUnit.entries[toIndex])
        UnitCategory.TIME        -> engine.convert(value, TimeUnit.entries[fromIndex], TimeUnit.entries[toIndex])
        UnitCategory.FORCE       -> engine.convert(value, ForceUnit.entries[fromIndex], ForceUnit.entries[toIndex])
        UnitCategory.PRESSURE    -> engine.convert(value, PressureUnit.entries[fromIndex], PressureUnit.entries[toIndex])
        UnitCategory.ENERGY      -> engine.convert(value, EnergyUnit.entries[fromIndex], EnergyUnit.entries[toIndex])
        UnitCategory.POWER       -> engine.convert(value, PowerUnit.entries[fromIndex], PowerUnit.entries[toIndex])
        UnitCategory.ANGLE       -> engine.convert(value, AngleUnit.entries[fromIndex], AngleUnit.entries[toIndex])
        UnitCategory.DATA        -> engine.convert(value, DataUnit.entries[fromIndex], DataUnit.entries[toIndex])
    }

    private fun formatConverted(value: BigDecimal): String =
        value.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun currentLastNumber(expr: String): String {
        val idx = expr.indexOfLast { it in "+-*/" }
        return if (idx == -1) expr else expr.substring(idx + 1)
    }
}
