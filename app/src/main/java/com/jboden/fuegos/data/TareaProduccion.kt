package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tareas_produccion")
data class TareaProduccion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val texto: String,
    val orden: Int,
    val marcada: Boolean = false
)
