package com.acalc.ui.viewmodel

import com.acalc.domain.AreaUnit
import com.acalc.domain.LengthUnit
import com.acalc.domain.SpeedUnit
import com.acalc.domain.TempUnit
import com.acalc.domain.UnitCategory
import com.acalc.domain.VolumeUnit
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

    // CONV-13 — Default state
    @Test
    fun test_defaultState_isLength_mmToInch() {
        val state = viewModel.state.value
        assertEquals(UnitCategory.LENGTH, state.selectedCategory)
        assertEquals("", state.topInput)
        assertEquals("", state.bottomInput)
        val units = state.units
        assertTrue("Expected UnitPair.Length", units is UnitPair.Length)
        val lengthUnits = units as UnitPair.Length
        assertEquals(LengthUnit.MM, lengthUnits.from)
        assertEquals(LengthUnit.INCH, lengthUnits.to)
    }

    // CONV-13 — All categories have defaults
    @Test
    fun test_allCategoriesHaveDefaults() {
        // LENGTH: MM->INCH
        viewModel.onCategorySelected(UnitCategory.LENGTH)
        val lengthUnits = viewModel.state.value.units as UnitPair.Length
        assertEquals(LengthUnit.MM, lengthUnits.from)
        assertEquals(LengthUnit.INCH, lengthUnits.to)

        // WEIGHT: KG->LB
        viewModel.onCategorySelected(UnitCategory.WEIGHT)
        val weightUnits = viewModel.state.value.units as UnitPair.Weight
        assertEquals(WeightUnit.KG, weightUnits.from)
        assertEquals(WeightUnit.LB, weightUnits.to)

        // VOLUME: ML->FL_OZ
        viewModel.onCategorySelected(UnitCategory.VOLUME)
        val volumeUnits = viewModel.state.value.units as UnitPair.Volume
        assertEquals(VolumeUnit.ML, volumeUnits.from)
        assertEquals(VolumeUnit.FL_OZ, volumeUnits.to)

        // TEMPERATURE: CELSIUS->FAHRENHEIT
        viewModel.onCategorySelected(UnitCategory.TEMPERATURE)
        val tempUnits = viewModel.state.value.units as UnitPair.Temperature
        assertEquals(TempUnit.CELSIUS, tempUnits.from)
        assertEquals(TempUnit.FAHRENHEIT, tempUnits.to)

        // AREA: SQ_M->SQ_FT
        viewModel.onCategorySelected(UnitCategory.AREA)
        val areaUnits = viewModel.state.value.units as UnitPair.Area
        assertEquals(AreaUnit.SQ_M, areaUnits.from)
        assertEquals(AreaUnit.SQ_FT, areaUnits.to)

        // SPEED: KM_PER_H->MPH
        viewModel.onCategorySelected(UnitCategory.SPEED)
        val speedUnits = viewModel.state.value.units as UnitPair.Speed
        assertEquals(SpeedUnit.KM_PER_H, speedUnits.from)
        assertEquals(SpeedUnit.MPH, speedUnits.to)
    }

    // CONV-01 — Top field drives bottom
    @Test
    fun test_onTopChanged_convertsToBottom() {
        // Default: MM -> INCH, 25.4mm = 1 inch
        viewModel.onTopChanged("25.4")
        assertEquals("1", viewModel.state.value.bottomInput)
    }

    // CONV-02 — Bottom field drives top
    @Test
    fun test_onBottomChanged_convertsToTop() {
        // Default: MM -> INCH, 1 inch = 25.4mm
        viewModel.onBottomChanged("1")
        assertEquals("25.4", viewModel.state.value.topInput)
    }

    // CONV-03 — Expression input in top field
    @Test
    fun test_expressionInput_evaluatesThenConverts() {
        // 25.4 + 10 = 35.4 mm, convert to inches
        viewModel.onTopChanged("25.4 + 10")
        val expected = viewModel.state.value.bottomInput
        // 35.4mm / 25.4mm per inch ≈ 1.3937...
        assertTrue("bottomInput should not be empty", expected.isNotEmpty())
        // verify it's the conversion of 35.4, not 25.4
        val bottomDouble = expected.toDouble()
        assertTrue("Expected ~1.39 inches for 35.4mm", bottomDouble > 1.3 && bottomDouble < 1.5)
    }

    // Pitfall 6 — Incomplete expression should not clear other field
    @Test
    fun test_incompleteExpression_doesNotClearOtherField() {
        // First set a valid conversion
        viewModel.onTopChanged("25.4")
        val firstBottom = viewModel.state.value.bottomInput
        assertTrue("firstBottom should not be empty", firstBottom.isNotEmpty())

        // Now type incomplete expression
        viewModel.onTopChanged("25.")
        // bottomInput should remain unchanged
        assertEquals(firstBottom, viewModel.state.value.bottomInput)
    }

    // CONV-10 — Category switch saves and restores
    @Test
    fun test_categorySwitchSavesAndRestores() {
        // Type in LENGTH
        viewModel.onTopChanged("100")
        val bottomBeforeSwitch = viewModel.state.value.bottomInput

        // Switch to WEIGHT
        viewModel.onCategorySelected(UnitCategory.WEIGHT)
        assertEquals(UnitCategory.WEIGHT, viewModel.state.value.selectedCategory)

        // Switch back to LENGTH
        viewModel.onCategorySelected(UnitCategory.LENGTH)
        assertEquals(UnitCategory.LENGTH, viewModel.state.value.selectedCategory)
        assertEquals("100", viewModel.state.value.topInput)
        assertEquals(bottomBeforeSwitch, viewModel.state.value.bottomInput)
    }

    // CONV-10 — Switching to new category shows defaults
    @Test
    fun test_categorySwitchToWeight_showsDefaults() {
        viewModel.onCategorySelected(UnitCategory.WEIGHT)
        val units = viewModel.state.value.units as UnitPair.Weight
        assertEquals(WeightUnit.KG, units.from)
        assertEquals(WeightUnit.LB, units.to)
    }

    // CONV-07 — Temperature conversion
    @Test
    fun test_temperature_100CtoF_is212() {
        viewModel.onCategorySelected(UnitCategory.TEMPERATURE)
        viewModel.onTopChanged("100")
        assertEquals("212", viewModel.state.value.bottomInput)
    }

    // Unit change recomputes from top
    @Test
    fun test_onTopUnitChanged_recomputes() {
        // Start with MM->INCH, top="25.4"
        viewModel.onTopChanged("25.4")
        val inchesForMM = viewModel.state.value.bottomInput

        // Change top unit to CM
        viewModel.onTopUnitChanged(LengthUnit.CM.ordinal)
        val inchesForCM = viewModel.state.value.bottomInput

        // 25.4 CM is larger than 25.4 MM, so result in inches should be larger
        assertTrue("CM->INCH result should differ from MM->INCH", inchesForMM != inchesForCM)
        val cmInches = inchesForCM.toDouble()
        assertTrue("25.4 cm ≈ 10 inches", cmInches > 9.5 && cmInches < 10.5)
    }

    // Unit change recomputes from bottom
    @Test
    fun test_onBottomUnitChanged_recomputes() {
        // Start with MM->INCH, bottom="1"
        viewModel.onBottomChanged("1")
        val mmForInch = viewModel.state.value.topInput  // should be 25.4

        // Change bottom unit to FOOT
        viewModel.onBottomUnitChanged(LengthUnit.FOOT.ordinal)
        val mmForFoot = viewModel.state.value.topInput

        // 1 foot is larger than 1 inch, so topInput (mm) should be larger
        assertTrue("MM for 1 FOOT should differ from MM for 1 INCH", mmForInch != mmForFoot)
        val footInMM = mmForFoot.toDouble()
        assertTrue("1 foot ≈ 304.8 mm", footInMM > 300 && footInMM < 310)
    }

    // Formatting — trailing zeros stripped
    @Test
    fun test_formatConverted_stripsTrailingZeros() {
        // 25.4mm = exactly 1 inch
        viewModel.onTopChanged("25.4")
        val bottom = viewModel.state.value.bottomInput
        assertEquals("1", bottom)
        assertTrue("Should not end with .0", !bottom.contains("."))
    }

    // Empty input — no conversion
    @Test
    fun test_emptyInput_noConversion() {
        viewModel.onTopChanged("25.4")
        // Now clear top
        viewModel.onTopChanged("")
        // bottomInput should be empty (cleared)
        assertEquals("", viewModel.state.value.bottomInput)
    }

    // CONV-04 — Length has all 8 units
    @Test
    fun test_lengthCategory_has8Units() {
        val units = viewModel.getUnitsForCategory(UnitCategory.LENGTH)
        assertEquals(8, units.size)
    }

    // CONV-05 — Weight has all 6 units
    @Test
    fun test_weightCategory_has6Units() {
        val units = viewModel.getUnitsForCategory(UnitCategory.WEIGHT)
        assertEquals(6, units.size)
    }

    // CONV-06 — Volume has all 7 units
    @Test
    fun test_volumeCategory_has7Units() {
        val units = viewModel.getUnitsForCategory(UnitCategory.VOLUME)
        assertEquals(7, units.size)
    }

    // CONV-08 — Area has all 7 units
    @Test
    fun test_areaCategory_has7Units() {
        val units = viewModel.getUnitsForCategory(UnitCategory.AREA)
        assertEquals(7, units.size)
    }

    // CONV-09 — Speed has all 4 units
    @Test
    fun test_speedCategory_has4Units() {
        val units = viewModel.getUnitsForCategory(UnitCategory.SPEED)
        assertEquals(4, units.size)
    }
}
