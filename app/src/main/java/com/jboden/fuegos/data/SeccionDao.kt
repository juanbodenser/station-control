package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SeccionDao {
    @Query("SELECT * FROM secciones ORDER BY orden ASC")
    fun getSecciones(): Flow<List<Seccion>>

    @Query("SELECT COUNT(*) FROM recuentos WHERE seccionId = :seccionId")
    suspend fun countRecuentosBySeccion(seccionId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(seccion: Seccion): Long

    @Update
    suspend fun update(seccion: Seccion)

    @Update
    suspend fun updateAll(secciones: List<Seccion>)

    @Delete
    suspend fun delete(seccion: Seccion)
}
