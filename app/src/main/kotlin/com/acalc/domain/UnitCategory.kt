package com.acalc.domain

import kotlinx.serialization.Serializable

@Serializable
enum class UnitCategory {
    TRIANGLE,
    LENGTH, WEIGHT, VOLUME, TEMPERATURE, AREA, SPEED,
    TIME, FORCE, PRESSURE, ENERGY, POWER, ANGLE, DATA
}
