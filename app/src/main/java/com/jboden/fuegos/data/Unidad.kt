package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unidades")
data class Unidad(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val abreviatura: String
)
