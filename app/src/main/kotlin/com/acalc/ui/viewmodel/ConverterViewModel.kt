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

// ── State model ──

sealed class UnitPair {
    data class Length(val from: LengthUnit, val to: LengthUnit) : UnitPair()
    data class Weight(val from: WeightUnit, val to: WeightUnit) : UnitPair()
    data class Volume(val from: VolumeUnit, val to: VolumeUnit) : UnitPair()
    data class Temperature(val from: TempUnit, val to: TempUnit) : UnitPair()
    data class Area(val from: AreaUnit, val to: AreaUnit) : UnitPair()
    data class Speed(val from: SpeedUnit, val to: SpeedUnit) : UnitPair()
}

data class CategoryState(
    val topInput: String = "",
    val bottomInput: String = "",
    val units: UnitPair
)

enum class ActiveField { TOP, BOTTOM }

data class ConverterState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val activeField: ActiveField = ActiveField.TOP,
    val topInput: String = "",
    val bottomInput: String = "",
    val units: UnitPair = UnitPair.Length(LengthUnit.MM, LengthUnit.INCH)
)

// ── ViewModel ──

class ConverterViewModel : ViewModel() {

    private val evaluator = ExpressionEvaluator()
    private val engine = ConversionEngine()

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state

    // Per-category state map, pre-populated with defaults for all 6 categories
    private val categoryStateMap: MutableMap<UnitCategory, CategoryState> =
        UnitCategory.entries.associateWith { defaultStateFor(it) }.toMutableMap()

    // MARK: — Input handlers

    fun onTopChanged(raw: String) {
        _state.value = _state.value.copy(activeField = ActiveField.TOP, topInput = raw)
        if (raw.isEmpty()) {
            updateAndSave(_state.value.copy(bottomInput = ""))
            return
        }
        // Trailing decimal/operator indicates user is still typing — treat as incomplete
        if (raw.trimEnd().last() in ".,+-*/") return
        val evaluated = evaluator.evaluate(raw) ?: return  // null = incomplete — leave bottomInput unchanged
        val value = BigDecimal(evaluated.toString())
        val converted = convertForCurrentCategory(value, fromTop = true)
        updateAndSave(_state.value.copy(bottomInput = formatConverted(converted)))
    }

    fun onBottomChanged(raw: String) {
        _state.value = _state.value.copy(activeField = ActiveField.BOTTOM, bottomInput = raw)
        if (raw.isEmpty()) {
            updateAndSave(_state.value.copy(topInput = ""))
            return
        }
        // Trailing decimal/operator indicates user is still typing — treat as incomplete
        if (raw.trimEnd().last() in ".,+-*/") return
        val evaluated = evaluator.evaluate(raw) ?: return  // null = incomplete — leave topInput unchanged
        val value = BigDecimal(evaluated.toString())
        val converted = convertForCurrentCategory(value, fromTop = false)
        updateAndSave(_state.value.copy(topInput = formatConverted(converted)))
    }

    fun onCategorySelected(category: UnitCategory) {
        // Save current category state
        val current = _state.value
        categoryStateMap[current.selectedCategory] = CategoryState(
            topInput = current.topInput,
            bottomInput = current.bottomInput,
            units = current.units
        )
        // Load new category state
        val newCatState = categoryStateMap[category] ?: defaultStateFor(category)
        _state.value = ConverterState(
            selectedCategory = category,
            activeField = ActiveField.TOP,
            topInput = newCatState.topInput,
            bottomInput = newCatState.bottomInput,
            units = newCatState.units
        )
    }

    fun onTopUnitChanged(unitIndex: Int) {
        val current = _state.value
        val newUnits = rebuildUnitPairWithTopIndex(current.units, unitIndex)
        val newState = current.copy(units = newUnits)
        _state.value = newState
        // Recompute based on active field
        recomputeAfterUnitChange(newState)
    }

    fun onBottomUnitChanged(unitIndex: Int) {
        val current = _state.value
        val newUnits = rebuildUnitPairWithBottomIndex(current.units, unitIndex)
        val newState = current.copy(units = newUnits)
        _state.value = newState
        recomputeAfterUnitChange(newState)
    }

    // MARK: — UI helpers

    fun getUnitsForCategory(category: UnitCategory): List<Pair<String, String>> {
        return when (category) {
            UnitCategory.LENGTH      -> LengthUnit.entries.map { it.name to it.displayName }
            UnitCategory.WEIGHT      -> WeightUnit.entries.map { it.name to it.displayName }
            UnitCategory.VOLUME      -> VolumeUnit.entries.map { it.name to it.displayName }
            UnitCategory.TEMPERATURE -> TempUnit.entries.map { it.name to it.displayName }
            UnitCategory.AREA        -> AreaUnit.entries.map { it.name to it.displayName }
            UnitCategory.SPEED       -> SpeedUnit.entries.map { it.name to it.displayName }
        }
    }

    fun getTopUnitDisplayName(): String = displayNameOf(_state.value.units, fromTop = true)

    fun getBottomUnitDisplayName(): String = displayNameOf(_state.value.units, fromTop = false)

    // MARK: — Private helpers

    private fun updateAndSave(newState: ConverterState) {
        _state.value = newState
        // Keep categoryStateMap in sync so switches preserve current inputs
        categoryStateMap[newState.selectedCategory] = CategoryState(
            topInput = newState.topInput,
            bottomInput = newState.bottomInput,
            units = newState.units
        )
    }

    private fun recomputeAfterUnitChange(state: ConverterState) {
        when (state.activeField) {
            ActiveField.TOP -> {
                val raw = state.topInput
                if (raw.isEmpty()) return
                val evaluated = evaluator.evaluate(raw) ?: return
                val value = BigDecimal(evaluated.toString())
                val converted = convertForCurrentCategory(value, fromTop = true)
                updateAndSave(state.copy(bottomInput = formatConverted(converted)))
            }
            ActiveField.BOTTOM -> {
                val raw = state.bottomInput
                if (raw.isEmpty()) return
                val evaluated = evaluator.evaluate(raw) ?: return
                val value = BigDecimal(evaluated.toString())
                val converted = convertForCurrentCategory(value, fromTop = false)
                updateAndSave(state.copy(topInput = formatConverted(converted)))
            }
        }
    }

    private fun convertForCurrentCategory(value: BigDecimal, fromTop: Boolean): BigDecimal {
        return when (val units = _state.value.units) {
            is UnitPair.Length ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
            is UnitPair.Weight ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
            is UnitPair.Volume ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
            is UnitPair.Temperature ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
            is UnitPair.Area ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
            is UnitPair.Speed ->
                if (fromTop) engine.convert(value, units.from, units.to)
                else engine.convert(value, units.to, units.from)
        }
    }

    private fun formatConverted(value: BigDecimal): String =
        value.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun rebuildUnitPairWithTopIndex(current: UnitPair, index: Int): UnitPair {
        return when (current) {
            is UnitPair.Length      -> current.copy(from = LengthUnit.entries[index])
            is UnitPair.Weight      -> current.copy(from = WeightUnit.entries[index])
            is UnitPair.Volume      -> current.copy(from = VolumeUnit.entries[index])
            is UnitPair.Temperature -> current.copy(from = TempUnit.entries[index])
            is UnitPair.Area        -> current.copy(from = AreaUnit.entries[index])
            is UnitPair.Speed       -> current.copy(from = SpeedUnit.entries[index])
        }
    }

    private fun rebuildUnitPairWithBottomIndex(current: UnitPair, index: Int): UnitPair {
        return when (current) {
            is UnitPair.Length      -> current.copy(to = LengthUnit.entries[index])
            is UnitPair.Weight      -> current.copy(to = WeightUnit.entries[index])
            is UnitPair.Volume      -> current.copy(to = VolumeUnit.entries[index])
            is UnitPair.Temperature -> current.copy(to = TempUnit.entries[index])
            is UnitPair.Area        -> current.copy(to = AreaUnit.entries[index])
            is UnitPair.Speed       -> current.copy(to = SpeedUnit.entries[index])
        }
    }

    private fun displayNameOf(units: UnitPair, fromTop: Boolean): String {
        return when (units) {
            is UnitPair.Length      -> if (fromTop) units.from.displayName else units.to.displayName
            is UnitPair.Weight      -> if (fromTop) units.from.displayName else units.to.displayName
            is UnitPair.Volume      -> if (fromTop) units.from.displayName else units.to.displayName
            is UnitPair.Temperature -> if (fromTop) units.from.displayName else units.to.displayName
            is UnitPair.Area        -> if (fromTop) units.from.displayName else units.to.displayName
            is UnitPair.Speed       -> if (fromTop) units.from.displayName else units.to.displayName
        }
    }

    companion object {
        fun defaultStateFor(category: UnitCategory): CategoryState = when (category) {
            UnitCategory.LENGTH      -> CategoryState(units = UnitPair.Length(LengthUnit.MM, LengthUnit.INCH))
            UnitCategory.WEIGHT      -> CategoryState(units = UnitPair.Weight(WeightUnit.KG, WeightUnit.LB))
            UnitCategory.VOLUME      -> CategoryState(units = UnitPair.Volume(VolumeUnit.ML, VolumeUnit.FL_OZ))
            UnitCategory.TEMPERATURE -> CategoryState(units = UnitPair.Temperature(TempUnit.CELSIUS, TempUnit.FAHRENHEIT))
            UnitCategory.AREA        -> CategoryState(units = UnitPair.Area(AreaUnit.SQ_M, AreaUnit.SQ_FT))
            UnitCategory.SPEED       -> CategoryState(units = UnitPair.Speed(SpeedUnit.KM_PER_H, SpeedUnit.MPH))
        }
    }
}
