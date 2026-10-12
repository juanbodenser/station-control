package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secciones")
data class Seccion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val orden: Int,
    val origen: Origen = Origen.PROVEEDOR
)
