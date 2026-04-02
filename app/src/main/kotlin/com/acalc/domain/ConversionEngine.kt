package com.acalc.domain

import java.math.BigDecimal

class ConversionEngine {
    fun convert(value: BigDecimal, from: LengthUnit, to: LengthUnit): BigDecimal = BigDecimal.ZERO
    fun convert(value: BigDecimal, from: WeightUnit, to: WeightUnit): BigDecimal = BigDecimal.ZERO
    fun convert(value: BigDecimal, from: VolumeUnit, to: VolumeUnit): BigDecimal = BigDecimal.ZERO
    fun convert(value: BigDecimal, from: TempUnit, to: TempUnit): BigDecimal = BigDecimal.ZERO
    fun convert(value: BigDecimal, from: AreaUnit, to: AreaUnit): BigDecimal = BigDecimal.ZERO
    fun convert(value: BigDecimal, from: SpeedUnit, to: SpeedUnit): BigDecimal = BigDecimal.ZERO
}
