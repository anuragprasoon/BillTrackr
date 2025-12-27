package com.example.billtrackr

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Bill::class], version = 1)
abstract class BillDatabase : RoomDatabase() {

    abstract fun BillDao(): BillDao
    companion object {
        fun getDatabase(context: Context): BillDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BillDatabase::class.java,
                "bill_db"
            ).build()
        }
    }
}