package com.acalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Covers the [ConversionEngine] categories not exercised by [ConversionEngineTest]:
 * time, force, pressure, energy, power, angle and data — plus round-trip stability.
 */
class ConversionEngineExtendedTest {

    private val engine = ConversionEngine()

    private fun assertClose(expected: String, actual: BigDecimal, tolerance: String = "0.000001") {
        val diff = BigDecimal(expected).subtract(actual).abs()
        assertTrue(
            "expected ~$expected but was $actual",
            diff < BigDecimal(tolerance)
        )
    }

    // ── TIME ──

    @Test
    fun `1 hour equals 3600 seconds`() {
        assertEquals(0, BigDecimal("3600").compareTo(
            engine.convert(BigDecimal("1"), TimeUnit.HOUR, TimeUnit.SECOND)))
    }

    @Test
    fun `1 day equals 24 hours`() {
        assertEquals(0, BigDecimal("24").compareTo(
            engine.convert(BigDecimal("1"), TimeUnit.DAY, TimeUnit.HOUR)))
    }

    @Test
    fun `1 week equals 7 days`() {
        assertEquals(0, BigDecimal("7").compareTo(
            engine.convert(BigDecimal("1"), TimeUnit.WEEK, TimeUnit.DAY)))
    }

    @Test
    fun `1 second equals 1000 milliseconds`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), TimeUnit.SECOND, TimeUnit.MILLISECOND)))
    }

    @Test
    fun `average year is 12 average months`() {
        assertClose("12", engine.convert(BigDecimal("1"), TimeUnit.YEAR, TimeUnit.MONTH))
    }

    // ── FORCE ──

    @Test
    fun `1 kilonewton equals 1000 newtons`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), ForceUnit.KILONEWTON, ForceUnit.NEWTON)))
    }

    @Test
    fun `1 kilogram force equals standard gravity in newtons`() {
        assertClose("9.80665", engine.convert(BigDecimal("1"), ForceUnit.KG_FORCE, ForceUnit.NEWTON))
    }

    @Test
    fun `1 pound force is about 4 point 448 newtons`() {
        assertClose("4.4482216", engine.convert(BigDecimal("1"), ForceUnit.LB_FORCE, ForceUnit.NEWTON), "0.0001")
    }

    @Test
    fun `1 newton equals 100000 dyne`() {
        assertEquals(0, BigDecimal("100000").compareTo(
            engine.convert(BigDecimal("1"), ForceUnit.NEWTON, ForceUnit.DYNE)))
    }

    @Test
    fun `1 kilogram force equals 1000 gram force`() {
        assertClose("1000", engine.convert(BigDecimal("1"), ForceUnit.KG_FORCE, ForceUnit.GRAM_FORCE), "0.0001")
    }

    // ── PRESSURE ──

    @Test
    fun `1 bar equals 100000 pascal`() {
        assertEquals(0, BigDecimal("100000").compareTo(
            engine.convert(BigDecimal("1"), PressureUnit.BAR, PressureUnit.PASCAL)))
    }

    @Test
    fun `1 atmosphere equals 101325 pascal`() {
        assertEquals(0, BigDecimal("101325").compareTo(
            engine.convert(BigDecimal("1"), PressureUnit.ATMOSPHERE, PressureUnit.PASCAL)))
    }

    @Test
    fun `1 bar equals 1000 millibar`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), PressureUnit.BAR, PressureUnit.MILLIBAR)))
    }

    @Test
    fun `1 atmosphere is about 14 point 6959 psi`() {
        assertClose("14.6959488", engine.convert(BigDecimal("1"), PressureUnit.ATMOSPHERE, PressureUnit.PSI), "0.0001")
    }

    @Test
    fun `1 atmosphere is 760 mmHg`() {
        assertClose("760", engine.convert(BigDecimal("1"), PressureUnit.ATMOSPHERE, PressureUnit.MMHG), "0.001")
    }

    @Test
    fun `1 megapascal equals 1000 kilopascal`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), PressureUnit.MEGAPASCAL, PressureUnit.KILOPASCAL)))
    }

    // ── ENERGY ──

    @Test
    fun `1 kilojoule equals 1000 joule`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), EnergyUnit.KILOJOULE, EnergyUnit.JOULE)))
    }

    @Test
    fun `1 kilocalorie equals 1000 calorie`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), EnergyUnit.KILOCALORIE, EnergyUnit.CALORIE)))
    }

    @Test
    fun `1 kilowatt hour equals 3 point 6 megajoule`() {
        assertClose("3.6", engine.convert(BigDecimal("1"), EnergyUnit.KILOWATT_HOUR, EnergyUnit.MEGAJOULE))
    }

    @Test
    fun `1 watt hour equals 3600 joule`() {
        assertEquals(0, BigDecimal("3600").compareTo(
            engine.convert(BigDecimal("1"), EnergyUnit.WATT_HOUR, EnergyUnit.JOULE)))
    }

    @Test
    fun `1 BTU is about 1055 joule`() {
        assertClose("1055.05585262", engine.convert(BigDecimal("1"), EnergyUnit.BTU, EnergyUnit.JOULE), "0.0001")
    }

    // ── POWER ──

    @Test
    fun `1 kilowatt equals 1000 watt`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), PowerUnit.KILOWATT, PowerUnit.WATT)))
    }

    @Test
    fun `1 megawatt equals 1000 kilowatt`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), PowerUnit.MEGAWATT, PowerUnit.KILOWATT)))
    }

    @Test
    fun `1 mechanical horsepower is about 745 point 7 watt`() {
        assertClose("745.6998716", engine.convert(BigDecimal("1"), PowerUnit.HP_MECH, PowerUnit.WATT), "0.0001")
    }

    @Test
    fun `1 metric horsepower is 735 point 49875 watt`() {
        assertClose("735.49875", engine.convert(BigDecimal("1"), PowerUnit.HP_METRIC, PowerUnit.WATT))
    }

    @Test
    fun `1 watt equals 1000 milliwatt`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), PowerUnit.WATT, PowerUnit.MILLIWATT)))
    }

    // ── ANGLE ──

    @Test
    fun `180 degrees is pi radians`() {
        assertClose("3.14159265", engine.convert(BigDecimal("180"), AngleUnit.DEGREE, AngleUnit.RADIAN), "0.00001")
    }

    @Test
    fun `400 gradians is 360 degrees`() {
        assertClose("360", engine.convert(BigDecimal("400"), AngleUnit.GRADIAN, AngleUnit.DEGREE))
    }

    @Test
    fun `90 degrees is 100 gradians`() {
        assertClose("100", engine.convert(BigDecimal("90"), AngleUnit.DEGREE, AngleUnit.GRADIAN))
    }

    // ── DATA ──

    @Test
    fun `1 byte equals 8 bits`() {
        assertEquals(0, BigDecimal("8").compareTo(
            engine.convert(BigDecimal("1"), DataUnit.BYTE, DataUnit.BIT)))
    }

    @Test
    fun `1 kilobyte equals 1000 bytes (decimal SI)`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), DataUnit.KILOBYTE, DataUnit.BYTE)))
    }

    @Test
    fun `1 gigabyte equals 1000 megabytes`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), DataUnit.GIGABYTE, DataUnit.MEGABYTE)))
    }

    @Test
    fun `1 petabyte equals 1000 terabytes`() {
        assertEquals(0, BigDecimal("1000").compareTo(
            engine.convert(BigDecimal("1"), DataUnit.PETABYTE, DataUnit.TERABYTE)))
    }

    // ── AREA / VOLUME / SPEED extras ──

    @Test
    fun `1 hectare equals 10000 square meters`() {
        assertEquals(0, BigDecimal("10000").compareTo(
            engine.convert(BigDecimal("1"), AreaUnit.HECTARE, AreaUnit.SQ_M)))
    }

    @Test
    fun `1 square foot equals 144 square inches`() {
        assertClose("144", engine.convert(BigDecimal("1"), AreaUnit.SQ_FT, AreaUnit.SQ_IN), "0.000001")
    }

    @Test
    fun `1 tablespoon equals 3 teaspoons`() {
        assertClose("3", engine.convert(BigDecimal("1"), VolumeUnit.TBSP, VolumeUnit.TSP))
    }

    @Test
    fun `1 cup equals 8 fluid ounces`() {
        assertClose("8", engine.convert(BigDecimal("1"), VolumeUnit.CUP, VolumeUnit.FL_OZ))
    }

    @Test
    fun `1 knot equals 1 point 852 km per h`() {
        assertClose("1.852", engine.convert(BigDecimal("1"), SpeedUnit.KNOTS, SpeedUnit.KM_PER_H), "0.00001")
    }

    @Test
    fun `1 mph is about 1 point 609 km per h`() {
        assertClose("1.609344", engine.convert(BigDecimal("1"), SpeedUnit.MPH, SpeedUnit.KM_PER_H), "0.00001")
    }

    // ── Round-trips ──

    @Test
    fun `pressure round trip returns original`() {
        val original = BigDecimal("42.5")
        val psi = engine.convert(original, PressureUnit.BAR, PressureUnit.PSI)
        val back = engine.convert(psi, PressureUnit.PSI, PressureUnit.BAR)
        assertClose("42.5", back, "0.0000001")
    }

    @Test
    fun `temperature round trip returns original`() {
        val original = BigDecimal("36.6")
        val f = engine.convert(original, TempUnit.CELSIUS, TempUnit.FAHRENHEIT)
        val back = engine.convert(f, TempUnit.FAHRENHEIT, TempUnit.CELSIUS)
        assertClose("36.6", back, "0.0000001")
    }

    @Test
    fun `angle round trip returns original`() {
        val original = BigDecimal("57.3")
        val rad = engine.convert(original, AngleUnit.DEGREE, AngleUnit.RADIAN)
        val back = engine.convert(rad, AngleUnit.RADIAN, AngleUnit.DEGREE)
        assertClose("57.3", back, "0.0000001")
    }

    // ── Identity for every category ──

    @Test
    fun `identity conversion is exact for all remaining categories`() {
        val v = BigDecimal("7.25")
        assertEquals(0, v.compareTo(engine.convert(v, TimeUnit.HOUR, TimeUnit.HOUR)))
        assertEquals(0, v.compareTo(engine.convert(v, ForceUnit.NEWTON, ForceUnit.NEWTON)))
        assertEquals(0, v.compareTo(engine.convert(v, PressureUnit.BAR, PressureUnit.BAR)))
        assertEquals(0, v.compareTo(engine.convert(v, EnergyUnit.JOULE, EnergyUnit.JOULE)))
        assertEquals(0, v.compareTo(engine.convert(v, PowerUnit.WATT, PowerUnit.WATT)))
        assertEquals(0, v.compareTo(engine.convert(v, AngleUnit.DEGREE, AngleUnit.DEGREE)))
        assertEquals(0, v.compareTo(engine.convert(v, DataUnit.BYTE, DataUnit.BYTE)))
        assertEquals(0, v.compareTo(engine.convert(v, AreaUnit.SQ_M, AreaUnit.SQ_M)))
        assertEquals(0, v.compareTo(engine.convert(v, VolumeUnit.L, VolumeUnit.L)))
        assertEquals(0, v.compareTo(engine.convert(v, WeightUnit.KG, WeightUnit.KG)))
    }

    // ── Negative and zero values ──

    @Test
    fun `zero converts to zero`() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
            engine.convert(BigDecimal.ZERO, LengthUnit.MM, LengthUnit.INCH)))
    }

    @Test
    fun `negative values convert with sign preserved`() {
        val result = engine.convert(BigDecimal("-40"), TempUnit.CELSIUS, TempUnit.FAHRENHEIT)
        assertClose("-40", result)
    }
}
