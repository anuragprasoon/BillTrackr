package com.example.billtrackr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills ORDER BY id DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Insert
    suspend fun insertBill(bill: Bill)
}