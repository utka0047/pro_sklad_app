package com.tornus.pro_sklad.data.local.dao

import androidx.room.*
import com.tornus.pro_sklad.data.local.entity.MovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {

    @Query("SELECT * FROM movements ORDER BY createdAt DESC")
    fun getAllMovements(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(productId: Int): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMovements(limit: Int = 20): Flow<List<MovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<MovementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: MovementEntity)

    @Query("DELETE FROM movements")
    suspend fun deleteAll()
}
