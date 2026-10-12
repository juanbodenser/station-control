package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnidadDao {
    @Query("SELECT * FROM unidades")
    fun getUnidades(): Flow<List<Unidad>>

    @Query("SELECT COUNT(*) FROM productos WHERE unidadId = :unidadId")
    suspend fun countProductosByUnidad(unidadId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unidad: Unidad): Long

    @Update
    suspend fun update(unidad: Unidad)

    @Delete
    suspend fun delete(unidad: Unidad)
}
