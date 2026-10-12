package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaProduccionDao {
    @Query("SELECT * FROM tareas_produccion ORDER BY orden ASC")
    fun getTareas(): Flow<List<TareaProduccion>>

    @Query("SELECT * FROM tareas_produccion")
    fun getAllTareas(): Flow<List<TareaProduccion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tarea: TareaProduccion): Long

    @Update
    suspend fun update(tarea: TareaProduccion)

    @Update
    suspend fun updateAll(tareas: List<TareaProduccion>)

    @Delete
    suspend fun delete(tarea: TareaProduccion)

    @Query("UPDATE tareas_produccion SET marcada = 0")
    suspend fun desmarcarTodas()
}
