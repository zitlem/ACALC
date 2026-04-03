package com.acalc.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.acalc.domain.AreaUnit
import com.acalc.domain.LengthUnit
import com.acalc.domain.SpeedUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.UnitCategory
import com.acalc.domain.VolumeUnit
import com.acalc.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

// ── ViewModel stub (RED phase — all methods throw) ──

class ConverterViewModel : ViewModel() {

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state

    fun onTopChanged(raw: String): Unit = throw NotImplementedError("RED: not implemented")
    fun onBottomChanged(raw: String): Unit = throw NotImplementedError("RED: not implemented")
    fun onCategorySelected(category: UnitCategory): Unit = throw NotImplementedError("RED: not implemented")
    fun onTopUnitChanged(unitIndex: Int): Unit = throw NotImplementedError("RED: not implemented")
    fun onBottomUnitChanged(unitIndex: Int): Unit = throw NotImplementedError("RED: not implemented")
    fun getUnitsForCategory(category: UnitCategory): List<Pair<String, String>> = throw NotImplementedError("RED: not implemented")
    fun getTopUnitDisplayName(): String = throw NotImplementedError("RED: not implemented")
    fun getBottomUnitDisplayName(): String = throw NotImplementedError("RED: not implemented")
}
