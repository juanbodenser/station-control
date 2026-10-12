package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lineas_recuento",
    foreignKeys = [
        ForeignKey(
            entity = Recuento::class,
            parentColumns = ["id"],
            childColumns = ["recuentoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"]
        )
    ],
    indices = [
        Index("recuentoId"),
        Index("productoId")
    ]
)
data class LineaRecuento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recuentoId: Long,
    val productoId: Long,
    val cantidad: Double
)
