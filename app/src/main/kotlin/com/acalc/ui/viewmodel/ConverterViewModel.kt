package com.acalc.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class ConverterRow(
    val unitIndex: Int,
    val value: String,
    val cursorPos: Int = 0
)

@Serializable
data class ConverterState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val rows: List<ConverterRow> = defaultRowsFor(UnitCategory.LENGTH),
    val activeRowIndex: Int = 0
)

// ── Persistence ──────────────────────────────────────────────────────────────

@Serializable
data class ConverterSavedState(
    val currentCategory: UnitCategory,
    val categoryMap: Map<UnitCategory, ConverterState>
)

interface ConverterStorage {
    fun load(): ConverterSavedState?
    fun save(saved: ConverterSavedState)
}

private class SharedPrefsConverterStorage(prefs: SharedPreferences) : ConverterStorage {
    private val sp = prefs
    override fun load(): ConverterSavedState? {
        val json = sp.getString("converter_state", null) ?: return null
        return try { Json.decodeFromString(json) } catch (_: Exception) { null }
    }
    override fun save(saved: ConverterSavedState) {
        sp.edit().putString("converter_state", Json.encodeToString(saved)).apply()
    }
}

private object NoOpConverterStorage : ConverterStorage {
    override fun load(): ConverterSavedState? = null
    override fun save(saved: ConverterSavedState) {}
}

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

class ConverterViewModel(
    private val storage: ConverterStorage = NoOpConverterStorage
) : ViewModel() {

    companion object {
        fun create(app: Application): ConverterViewModel =
            ConverterViewModel(
                SharedPrefsConverterStorage(
                    app.getSharedPreferences("acalc_prefs", Context.MODE_PRIVATE)
                )
            )
    }

    private val evaluator = ExpressionEvaluator()
    private val engine = ConversionEngine()

    private val categoryStateMap: MutableMap<UnitCategory, ConverterState>

    private val _state: MutableStateFlow<ConverterState>
    val state: StateFlow<ConverterState> get() = _state

    init {
        val saved = storage.load()
        if (saved != null) {
            categoryStateMap = saved.categoryMap.toMutableMap()
            _state = MutableStateFlow(
                saved.categoryMap[saved.currentCategory]
                    ?: ConverterState(selectedCategory = saved.currentCategory,
                                      rows = defaultRowsFor(saved.currentCategory))
            )
        } else {
            categoryStateMap = mutableMapOf()
            _state = MutableStateFlow(ConverterState())
        }
    }

    /** Sets state, syncs the category map, and persists. */
    private fun setState(newState: ConverterState) {
        categoryStateMap[newState.selectedCategory] = newState
        _state.value = newState
        storage.save(ConverterSavedState(newState.selectedCategory, categoryStateMap.toMap()))
    }

    // MARK: — Category switching

    fun onCategorySelected(category: UnitCategory) {
        categoryStateMap[_state.value.selectedCategory] = _state.value
        val next = categoryStateMap[category] ?: ConverterState(
            selectedCategory = category,
            rows = defaultRowsFor(category)
        )
        setState(next)
    }

    // MARK: — Row activation

    fun onRowActivated(index: Int) {
        setState(_state.value.copy(activeRowIndex = index))
    }

    /** Called by the UI when the user taps to reposition the cursor in any row.
     *  Also activates that row so numpad input follows the tapped field. */
    fun onCursorMoved(rowIndex: Int, newPos: Int) {
        val state = _state.value
        val rows = state.rows.toMutableList()
        if (rowIndex !in rows.indices) return
        val value = rows[rowIndex].value
        val clamped = newPos.coerceIn(0, value.length)
        rows[rowIndex] = rows[rowIndex].copy(cursorPos = clamped)
        setState(state.copy(rows = rows, activeRowIndex = rowIndex))
    }

    // MARK: — Numpad input

    fun onNumpadKey(key: String) {
        val state = _state.value
        val activeIndex = state.activeRowIndex
        val rows = state.rows.toMutableList()
        val current = rows[activeIndex].value
        val cursor = rows[activeIndex].cursorPos.coerceIn(0, current.length)

        var newCursor = cursor
        val newValue = when (key) {
            "⌫"  -> {
                if (cursor == 0 || current.isEmpty()) {
                    current
                } else {
                    newCursor = cursor - 1
                    current.removeRange(cursor - 1, cursor)
                }
            }
            "C"   -> {
                newCursor = 0
                ""
            }
            "."   -> {
                // Determine the token at cursor position
                val tokenAtCursor = currentLastNumberBefore(current, cursor)
                if (tokenAtCursor.contains('.')) {
                    current
                } else {
                    val charBefore = if (cursor > 0) current[cursor - 1] else null
                    val insertion = if (charBefore == null || charBefore in "+-*/") "0." else "."
                    newCursor = cursor + insertion.length
                    current.insert(cursor, insertion)
                }
            }
            "±"   -> when {
                current.isEmpty() -> { newCursor = 1; "-" }
                current.startsWith("-") -> { newCursor = (cursor - 1).coerceAtLeast(0); current.drop(1) }
                else -> { newCursor = cursor + 1; "-$current" }
            }
            "×"   -> {
                val charBefore = if (cursor > 0) current[cursor - 1] else null
                if (current.isEmpty() || charBefore != null && charBefore in "+-*/") current
                else { newCursor = cursor + 1; current.insert(cursor, "*") }
            }
            "÷"   -> {
                val charBefore = if (cursor > 0) current[cursor - 1] else null
                if (current.isEmpty() || charBefore != null && charBefore in "+-*/") current
                else { newCursor = cursor + 1; current.insert(cursor, "/") }
            }
            "+", "-" -> {
                val charBefore = if (cursor > 0) current[cursor - 1] else null
                when {
                    current.isEmpty() && key == "-" -> { newCursor = 1; "-" }
                    current.isEmpty() -> current
                    charBefore != null && charBefore in "+-*/" -> {
                        // Replace the operator immediately before cursor
                        newCursor = cursor // cursor stays (replaced 1 char with 1 char)
                        current.removeRange(cursor - 1, cursor).insert(cursor - 1, key)
                    }
                    else -> { newCursor = cursor + 1; current.insert(cursor, key) }
                }
            }
            else  -> {
                // digit key
                val insertion = if (current == "0" && cursor == 1) {
                    newCursor = 1
                    key  // replace the lone "0"
                } else {
                    newCursor = cursor + key.length
                    key
                }
                if (current == "0" && cursor == 1) insertion
                else current.insert(cursor, insertion)
            }
        }

        rows[activeIndex] = rows[activeIndex].copy(value = newValue, cursorPos = newCursor)

        if (newValue.isEmpty()) {
            val cleared = rows.mapIndexed { i, r -> if (i == activeIndex) r else r.copy(value = "") }
            setState(state.copy(rows = cleared))
            return
        }

        val evaluated = evaluator.evaluate(newValue) ?: run {
            setState(state.copy(rows = rows))
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
        setState(state.copy(rows = rows, activeRowIndex = newActive))
    }

    // MARK: — Unit selection

    fun onUnitChanged(rowIndex: Int, unitIndex: Int) {
        val state = _state.value
        val rows = state.rows.toMutableList()
        rows[rowIndex] = rows[rowIndex].copy(unitIndex = unitIndex)
        setState(state.copy(rows = rows))

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
        setState(s.copy(activeRowIndex = prev))
    }

    fun onFocusNextRow() {
        val s = _state.value
        if (s.rows.isEmpty()) return
        val next = (s.activeRowIndex + 1) % s.rows.size
        setState(s.copy(activeRowIndex = next))
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
        rows[activeIndex] = rows[activeIndex].copy(value = displayValue, cursorPos = displayValue.length)
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

    fun getLiveResult(state: ConverterState): String? {
        val value = state.rows.getOrNull(state.activeRowIndex)?.value ?: return null
        if (value.isEmpty()) return null
        val evaluated = evaluator.evaluate(value) ?: return null
        val result = formatConverted(BigDecimal(evaluated.toString()))
        return if (result == value) null else result
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

        val rateForward = runCatching {
            convertBetween(BigDecimal.ONE, fromUnitIndex, toUnitIndex, state.selectedCategory)
        }.getOrNull() ?: return ""
        val rateBackward = runCatching {
            convertBetween(BigDecimal.ONE, toUnitIndex, fromUnitIndex, state.selectedCategory)
        }.getOrNull() ?: return ""

        // Use the larger factor as the canonical multiplier so both lines show a clean number:
        // forward uses × if factor >= 1, otherwise shows ÷ canonical; backward is the mirror.
        return if (rateForward >= BigDecimal.ONE) {
            val fwd = rateForward.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            "$fromName → $toName: × $fwd  |  $toName → $fromName: ÷ $fwd"
        } else {
            val bwd = rateBackward.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            "$fromName → $toName: ÷ $bwd  |  $toName → $fromName: × $bwd"
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
                val newValue = formatConverted(converted)
                r.copy(value = newValue, cursorPos = newValue.length)
            }
        }
        setState(state.copy(rows = finalRows))
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

    /** Returns the numeric token in [expr] that ends at [pos] (for decimal-guard). */
    private fun currentLastNumberBefore(expr: String, pos: Int): String {
        val sub = expr.substring(0, pos)
        val idx = sub.indexOfLast { it in "+-*/" }
        return if (idx == -1) sub else sub.substring(idx + 1)
    }
}

/** Inserts [s] into this string at position [index]. */
private fun String.insert(index: Int, s: String): String =
    substring(0, index) + s + substring(index)
