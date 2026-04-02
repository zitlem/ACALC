package com.acalc.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CalculatorState(
    val expression: String = "",
    val result: String = "",
    val isError: Boolean = false
)

class CalculatorViewModel {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state

    fun onDigit(digit: String) {}
    fun onOperator(op: String) {}
    fun onDecimal() {}
    fun onClear() {}
    fun onBackspace() {}
    fun onPercent() {}
    fun onEquals() {}
}
