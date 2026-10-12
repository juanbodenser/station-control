package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Origen {
    PROVEEDOR,
    TALLER,
    COCINA
}

@Entity(
    tableName = "entradas",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"]
        )
    ],
    indices = [
        Index("productoId")
    ]
)
data class Entrada(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productoId: Long,
    val fecha: Long,
    val cantidad: Double,
    val origen: Origen
)
