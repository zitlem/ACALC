package com.acalc.domain

import java.math.BigDecimal
import java.math.MathContext

class ConversionEngine {

    // Use MathContext.DECIMAL128 (34 significant digits) for ALL divide operations
    private val mc = MathContext.DECIMAL128

    // ── LENGTH (base: 1 meter) ──
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

    // ── WEIGHT (base: 1 gram) ──
    private val weightFactors = mapOf(
        WeightUnit.MG  to BigDecimal("0.001"),
        WeightUnit.G   to BigDecimal("1"),
        WeightUnit.KG  to BigDecimal("1000"),
        WeightUnit.OZ  to BigDecimal("28.349523125"),
        WeightUnit.LB  to BigDecimal("453.59237"),
        WeightUnit.TON to BigDecimal("1000000"),
    )

    // ── VOLUME (base: 1 milliliter) ──
    private val volumeFactors = mapOf(
        VolumeUnit.ML     to BigDecimal("1"),
        VolumeUnit.L      to BigDecimal("1000"),
        VolumeUnit.TSP    to BigDecimal("4.92892159375"),
        VolumeUnit.TBSP   to BigDecimal("14.78676478125"),
        VolumeUnit.CUP    to BigDecimal("236.5882365"),
        VolumeUnit.FL_OZ  to BigDecimal("29.5735295625"),
        VolumeUnit.GALLON to BigDecimal("3785.411784"),
    )

    // ── AREA (base: 1 square meter) ──
    private val areaFactors = mapOf(
        AreaUnit.SQ_MM to BigDecimal("0.000001"),
        AreaUnit.SQ_CM to BigDecimal("0.0001"),
        AreaUnit.SQ_M  to BigDecimal("1"),
        AreaUnit.SQ_KM to BigDecimal("1000000"),
        AreaUnit.SQ_IN to BigDecimal("0.00064516"),
        AreaUnit.SQ_FT to BigDecimal("0.09290304"),
        AreaUnit.ACRE  to BigDecimal("4046.8564224"),
    )

    // ── SPEED (base: 1 m/s) ──
    // km/h and knots factors are COMPUTED (non-terminating decimals)
    private val speedFactors = mapOf(
        SpeedUnit.M_PER_S  to BigDecimal("1"),
        SpeedUnit.KM_PER_H to BigDecimal("1000").divide(BigDecimal("3600"), MathContext.DECIMAL128),
        SpeedUnit.MPH      to BigDecimal("0.44704"),
        SpeedUnit.KNOTS    to BigDecimal("1852").divide(BigDecimal("3600"), MathContext.DECIMAL128),
    )

    // ── Generic multiplicative conversion ──
    private fun <T> convertMultiplicative(
        value: BigDecimal, from: T, to: T, factors: Map<T, BigDecimal>
    ): BigDecimal {
        if (from == to) return value
        val inBase = value.multiply(factors[from]!!)
        return inBase.divide(factors[to]!!, mc)
    }

    fun convert(value: BigDecimal, from: LengthUnit, to: LengthUnit): BigDecimal =
        convertMultiplicative(value, from, to, lengthFactors)

    fun convert(value: BigDecimal, from: WeightUnit, to: WeightUnit): BigDecimal =
        convertMultiplicative(value, from, to, weightFactors)

    fun convert(value: BigDecimal, from: VolumeUnit, to: VolumeUnit): BigDecimal =
        convertMultiplicative(value, from, to, volumeFactors)

    fun convert(value: BigDecimal, from: AreaUnit, to: AreaUnit): BigDecimal =
        convertMultiplicative(value, from, to, areaFactors)

    fun convert(value: BigDecimal, from: SpeedUnit, to: SpeedUnit): BigDecimal =
        convertMultiplicative(value, from, to, speedFactors)

    // ── TEMPERATURE (offset-aware, through Celsius intermediate) ──
    fun convert(value: BigDecimal, from: TempUnit, to: TempUnit): BigDecimal {
        if (from == to) return value

        // Step 1: convert to Celsius
        val celsius = when (from) {
            TempUnit.CELSIUS    -> value
            TempUnit.FAHRENHEIT -> (value - BigDecimal("32"))
                .multiply(BigDecimal("5"))
                .divide(BigDecimal("9"), mc)
            TempUnit.KELVIN     -> value - BigDecimal("273.15")
        }

        // Step 2: convert Celsius to target
        return when (to) {
            TempUnit.CELSIUS    -> celsius
            TempUnit.FAHRENHEIT -> celsius
                .multiply(BigDecimal("9"))
                .divide(BigDecimal("5"), mc)
                .add(BigDecimal("32"))
            TempUnit.KELVIN     -> celsius.add(BigDecimal("273.15"))
        }
    }
}
