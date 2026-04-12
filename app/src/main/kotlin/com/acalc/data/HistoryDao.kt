package com.acalc.data

import kotlinx.coroutines.flow.Flow

// Phase 5 placeholder — Room integration pending KSP setup
interface HistoryDao {
    fun getAll(): Flow<List<CalculationEntity>>
    suspend fun insert(entity: CalculationEntity)
    suspend fun clearAll()
}
