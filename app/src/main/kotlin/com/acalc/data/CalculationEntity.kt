package com.acalc.data

import kotlinx.serialization.Serializable

@Serializable
data class CalculationEntity(
    val id: Int = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
