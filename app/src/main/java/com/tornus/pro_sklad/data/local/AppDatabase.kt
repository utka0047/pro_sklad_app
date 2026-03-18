package com.tornus.pro_sklad.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tornus.pro_sklad.data.local.dao.MovementDao
import com.tornus.pro_sklad.data.local.dao.ProductDao
import com.tornus.pro_sklad.data.local.entity.MovementEntity
import com.tornus.pro_sklad.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, MovementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun movementDao(): MovementDao
}
