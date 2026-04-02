package com.acalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConversionEngineTest {

    private val engine = ConversionEngine()

    // ── LENGTH ──

    @Test
    fun `mm to inch and back is lossless`() {
        val original = BigDecimal("25")
        val inInches = engine.convert(original, LengthUnit.MM, LengthUnit.INCH)
        val backToMm = engine.convert(inInches, LengthUnit.INCH, LengthUnit.MM)
        assertEquals(0, original.compareTo(backToMm))
    }

    @Test
    fun `1 km equals 1000 m`() {
        val result = engine.convert(BigDecimal("1"), LengthUnit.KM, LengthUnit.M)
        assertEquals(0, BigDecimal("1000").compareTo(result))
    }

    @Test
    fun `1 mile equals 5280 feet`() {
        val result = engine.convert(BigDecimal("1"), LengthUnit.MILE, LengthUnit.FOOT)
        assertEquals(0, BigDecimal("5280").compareTo(result))
    }

    @Test
    fun `1 inch equals 25 point 4 mm`() {
        val result = engine.convert(BigDecimal("1"), LengthUnit.INCH, LengthUnit.MM)
        assertEquals(0, BigDecimal("25.4").compareTo(result))
    }

    @Test
    fun `length identity`() {
        val result = engine.convert(BigDecimal("42"), LengthUnit.MM, LengthUnit.MM)
        assertEquals(0, BigDecimal("42").compareTo(result))
    }

    // ── WEIGHT ──

    @Test
    fun `1 kg equals 1000 g`() {
        val result = engine.convert(BigDecimal("1"), WeightUnit.KG, WeightUnit.G)
        assertEquals(0, BigDecimal("1000").compareTo(result))
    }

    @Test
    fun `1 lb equals 16 oz`() {
        val result = engine.convert(BigDecimal("1"), WeightUnit.LB, WeightUnit.OZ)
        assertEquals(0, BigDecimal("16").compareTo(result))
    }

    @Test
    fun `1 metric ton equals 1000 kg`() {
        val result = engine.convert(BigDecimal("1"), WeightUnit.TON, WeightUnit.KG)
        assertEquals(0, BigDecimal("1000").compareTo(result))
    }

    // ── VOLUME ──

    @Test
    fun `1 L equals 1000 mL`() {
        val result = engine.convert(BigDecimal("1"), VolumeUnit.L, VolumeUnit.ML)
        assertEquals(0, BigDecimal("1000").compareTo(result))
    }

    @Test
    fun `1 gallon equals 128 fl oz`() {
        val result = engine.convert(BigDecimal("1"), VolumeUnit.GALLON, VolumeUnit.FL_OZ)
        assertEquals(0, BigDecimal("128").compareTo(result))
    }

    // ── TEMPERATURE ──

    @Test
    fun `32 Fahrenheit equals 0 Celsius`() {
        val result = engine.convert(BigDecimal("32"), TempUnit.FAHRENHEIT, TempUnit.CELSIUS)
        assertEquals(0, BigDecimal("0").compareTo(result))
    }

    @Test
    fun `0 Celsius equals 273 point 15 Kelvin`() {
        val result = engine.convert(BigDecimal("0"), TempUnit.CELSIUS, TempUnit.KELVIN)
        assertEquals(0, BigDecimal("273.15").compareTo(result))
    }

    @Test
    fun `212 Fahrenheit equals 100 Celsius`() {
        val result = engine.convert(BigDecimal("212"), TempUnit.FAHRENHEIT, TempUnit.CELSIUS)
        assertEquals(0, BigDecimal("100").compareTo(result))
    }

    @Test
    fun `absolute zero Kelvin equals minus 273 point 15 Celsius`() {
        val result = engine.convert(BigDecimal("0"), TempUnit.KELVIN, TempUnit.CELSIUS)
        assertEquals(0, BigDecimal("-273.15").compareTo(result))
    }

    @Test
    fun `32 Fahrenheit equals 273 point 15 Kelvin`() {
        val result = engine.convert(BigDecimal("32"), TempUnit.FAHRENHEIT, TempUnit.KELVIN)
        assertEquals(0, BigDecimal("273.15").compareTo(result))
    }

    @Test
    fun `temperature identity`() {
        val result = engine.convert(BigDecimal("25"), TempUnit.CELSIUS, TempUnit.CELSIUS)
        assertEquals(0, BigDecimal("25").compareTo(result))
    }

    // ── AREA ──

    @Test
    fun `1 sq m equals 10000 sq cm`() {
        val result = engine.convert(BigDecimal("1"), AreaUnit.SQ_M, AreaUnit.SQ_CM)
        assertEquals(0, BigDecimal("10000").compareTo(result))
    }

    @Test
    fun `1 sq km equals 1000000 sq m`() {
        val result = engine.convert(BigDecimal("1"), AreaUnit.SQ_KM, AreaUnit.SQ_M)
        assertEquals(0, BigDecimal("1000000").compareTo(result))
    }

    @Test
    fun `1 acre equals 43560 sq ft`() {
        val result = engine.convert(BigDecimal("1"), AreaUnit.ACRE, AreaUnit.SQ_FT)
        assertEquals(0, BigDecimal("43560").compareTo(result))
    }

    // ── SPEED ──

    @Test
    fun `1 m per s equals 3 point 6 km per h`() {
        val result = engine.convert(BigDecimal("1"), SpeedUnit.M_PER_S, SpeedUnit.KM_PER_H)
        val expected = BigDecimal("3.6")
        assertTrue(expected.subtract(result).abs() < BigDecimal("0.0001"))
    }

    @Test
    fun `speed identity`() {
        val result = engine.convert(BigDecimal("100"), SpeedUnit.MPH, SpeedUnit.MPH)
        assertEquals(0, BigDecimal("100").compareTo(result))
    }
}
