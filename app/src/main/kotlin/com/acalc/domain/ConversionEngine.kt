package com.acalc.domain

import java.math.BigDecimal
import java.math.MathContext

class ConversionEngine {

    private val mc = MathContext.DECIMAL128

    // ── LENGTH (base: meter) ──
    private val lengthFactors = mapOf(
        LengthUnit.MM   to BigDecimal("0.001"),
        LengthUnit.CM   to BigDecimal("0.01"),
        LengthUnit.M    to BigDecimal("1"),
        LengthUnit.KM   to BigDecimal("1000"),
        LengthUnit.INCH to BigDecimal("0.0254"),
        LengthUnit.FOOT to BigDecimal("0.3048"),
        LengthUnit.YARD to BigDecimal("0.9144"),
        LengthUnit.MILE to BigDecimal("1609.344"),
    )

    // ── WEIGHT (base: gram) ──
    private val weightFactors = mapOf(
        WeightUnit.MG  to BigDecimal("0.001"),
        WeightUnit.G   to BigDecimal("1"),
        WeightUnit.KG  to BigDecimal("1000"),
        WeightUnit.OZ  to BigDecimal("28.349523125"),
        WeightUnit.LB  to BigDecimal("453.59237"),
        WeightUnit.TON to BigDecimal("1000000"),
    )

    // ── VOLUME (base: milliliter) ──
    private val volumeFactors = mapOf(
        VolumeUnit.ML     to BigDecimal("1"),
        VolumeUnit.L      to BigDecimal("1000"),
        VolumeUnit.TSP    to BigDecimal("4.92892159375"),
        VolumeUnit.TBSP   to BigDecimal("14.78676478125"),
        VolumeUnit.CUP    to BigDecimal("236.5882365"),
        VolumeUnit.FL_OZ  to BigDecimal("29.5735295625"),
        VolumeUnit.GALLON to BigDecimal("3785.411784"),
    )

    // ── AREA (base: square meter) ──
    private val areaFactors = mapOf(
        AreaUnit.SQ_MM   to BigDecimal("0.000001"),
        AreaUnit.SQ_CM   to BigDecimal("0.0001"),
        AreaUnit.SQ_M    to BigDecimal("1"),
        AreaUnit.SQ_KM   to BigDecimal("1000000"),
        AreaUnit.SQ_IN   to BigDecimal("0.00064516"),
        AreaUnit.SQ_FT   to BigDecimal("0.09290304"),
        AreaUnit.ACRE    to BigDecimal("4046.8564224"),
        AreaUnit.HECTARE to BigDecimal("10000"),
    )

    // ── SPEED (base: m/s) ──
    private val speedFactors = mapOf(
        SpeedUnit.M_PER_S  to BigDecimal("1"),
        SpeedUnit.KM_PER_H to BigDecimal("1000").divide(BigDecimal("3600"), mc),
        SpeedUnit.MPH      to BigDecimal("0.44704"),
        SpeedUnit.KNOTS    to BigDecimal("1852").divide(BigDecimal("3600"), mc),
        SpeedUnit.MACH     to BigDecimal("343"),  // speed of sound at 20°C
    )

    // ── TIME (base: second) ──
    private val timeFactors = mapOf(
        TimeUnit.MILLISECOND to BigDecimal("0.001"),
        TimeUnit.SECOND      to BigDecimal("1"),
        TimeUnit.MINUTE      to BigDecimal("60"),
        TimeUnit.HOUR        to BigDecimal("3600"),
        TimeUnit.DAY         to BigDecimal("86400"),
        TimeUnit.WEEK        to BigDecimal("604800"),
        TimeUnit.MONTH       to BigDecimal("2629746"),   // average month
        TimeUnit.YEAR        to BigDecimal("31556952"),  // average year
    )

    // ── FORCE (base: Newton) ──
    private val forceFactors = mapOf(
        ForceUnit.NEWTON     to BigDecimal("1"),
        ForceUnit.KILONEWTON to BigDecimal("1000"),
        ForceUnit.DYNE       to BigDecimal("0.00001"),
        ForceUnit.GRAM_FORCE to BigDecimal("0.00980665"),
        ForceUnit.KG_FORCE   to BigDecimal("9.80665"),
        ForceUnit.LB_FORCE   to BigDecimal("4.4482216152605"),
        ForceUnit.POUNDAL    to BigDecimal("0.138254954376"),
    )

    // ── PRESSURE (base: Pascal) ──
    private val pressureFactors = mapOf(
        PressureUnit.PASCAL      to BigDecimal("1"),
        PressureUnit.HECTOPASCAL to BigDecimal("100"),
        PressureUnit.KILOPASCAL  to BigDecimal("1000"),
        PressureUnit.MEGAPASCAL  to BigDecimal("1000000"),
        PressureUnit.BAR         to BigDecimal("100000"),
        PressureUnit.MILLIBAR    to BigDecimal("100"),
        PressureUnit.ATMOSPHERE  to BigDecimal("101325"),
        PressureUnit.PSI         to BigDecimal("6894.757293168"),
        PressureUnit.MMHG        to BigDecimal("133.322387415"),
        PressureUnit.INHG        to BigDecimal("3386.389"),
        PressureUnit.TORR        to BigDecimal("133.322368421053"),
    )

    // ── ENERGY (base: Joule) ──
    private val energyFactors = mapOf(
        EnergyUnit.JOULE         to BigDecimal("1"),
        EnergyUnit.KILOJOULE     to BigDecimal("1000"),
        EnergyUnit.MEGAJOULE     to BigDecimal("1000000"),
        EnergyUnit.CALORIE       to BigDecimal("4.184"),
        EnergyUnit.KILOCALORIE   to BigDecimal("4184"),
        EnergyUnit.WATT_HOUR     to BigDecimal("3600"),
        EnergyUnit.KILOWATT_HOUR to BigDecimal("3600000"),
        EnergyUnit.BTU           to BigDecimal("1055.05585262"),
    )

    // ── POWER (base: Watt) ──
    private val powerFactors = mapOf(
        PowerUnit.MILLIWATT  to BigDecimal("0.001"),
        PowerUnit.WATT       to BigDecimal("1"),
        PowerUnit.KILOWATT   to BigDecimal("1000"),
        PowerUnit.MEGAWATT   to BigDecimal("1000000"),
        PowerUnit.HP_MECH    to BigDecimal("745.69987158227022"),
        PowerUnit.HP_METRIC  to BigDecimal("735.49875"),
    )

    // ── ANGLE (base: degree) ──
    private val angleFactors = mapOf(
        AngleUnit.DEGREE  to BigDecimal("1"),
        AngleUnit.RADIAN  to BigDecimal("57.29577951308232"),  // 180/π
        AngleUnit.GRADIAN to BigDecimal("0.9"),
    )

    // ── DATA (base: bit) ──
    private val dataFactors = mapOf(
        DataUnit.BIT      to BigDecimal("1"),
        DataUnit.BYTE     to BigDecimal("8"),
        DataUnit.KILOBYTE to BigDecimal("8000"),
        DataUnit.MEGABYTE to BigDecimal("8000000"),
        DataUnit.GIGABYTE to BigDecimal("8000000000"),
        DataUnit.TERABYTE to BigDecimal("8000000000000"),
        DataUnit.PETABYTE to BigDecimal("8000000000000000"),
    )

    // ── Generic multiplicative conversion ──
    private fun <T> convertMultiplicative(
        value: BigDecimal, from: T, to: T, factors: Map<T, BigDecimal>
    ): BigDecimal {
        if (from == to) return value
        val inBase = value.multiply(factors[from]!!)
        return inBase.divide(factors[to]!!, mc)
    }

    fun convert(value: BigDecimal, from: LengthUnit,   to: LengthUnit)   = convertMultiplicative(value, from, to, lengthFactors)
    fun convert(value: BigDecimal, from: WeightUnit,   to: WeightUnit)   = convertMultiplicative(value, from, to, weightFactors)
    fun convert(value: BigDecimal, from: VolumeUnit,   to: VolumeUnit)   = convertMultiplicative(value, from, to, volumeFactors)
    fun convert(value: BigDecimal, from: AreaUnit,     to: AreaUnit)     = convertMultiplicative(value, from, to, areaFactors)
    fun convert(value: BigDecimal, from: SpeedUnit,    to: SpeedUnit)    = convertMultiplicative(value, from, to, speedFactors)
    fun convert(value: BigDecimal, from: TimeUnit,     to: TimeUnit)     = convertMultiplicative(value, from, to, timeFactors)
    fun convert(value: BigDecimal, from: ForceUnit,    to: ForceUnit)    = convertMultiplicative(value, from, to, forceFactors)
    fun convert(value: BigDecimal, from: PressureUnit, to: PressureUnit) = convertMultiplicative(value, from, to, pressureFactors)
    fun convert(value: BigDecimal, from: EnergyUnit,   to: EnergyUnit)   = convertMultiplicative(value, from, to, energyFactors)
    fun convert(value: BigDecimal, from: PowerUnit,    to: PowerUnit)    = convertMultiplicative(value, from, to, powerFactors)
    fun convert(value: BigDecimal, from: AngleUnit,    to: AngleUnit)    = convertMultiplicative(value, from, to, angleFactors)
    fun convert(value: BigDecimal, from: DataUnit,     to: DataUnit)     = convertMultiplicative(value, from, to, dataFactors)

    // ── TEMPERATURE (affine, through Celsius) ──
    fun convert(value: BigDecimal, from: TempUnit, to: TempUnit): BigDecimal {
        if (from == to) return value
        val celsius = when (from) {
            TempUnit.CELSIUS    -> value
            TempUnit.FAHRENHEIT -> (value - BigDecimal("32")).multiply(BigDecimal("5")).divide(BigDecimal("9"), mc)
            TempUnit.KELVIN     -> value - BigDecimal("273.15")
        }
        return when (to) {
            TempUnit.CELSIUS    -> celsius
            TempUnit.FAHRENHEIT -> celsius.multiply(BigDecimal("9")).divide(BigDecimal("5"), mc).add(BigDecimal("32"))
            TempUnit.KELVIN     -> celsius.add(BigDecimal("273.15"))
        }
    }
}
