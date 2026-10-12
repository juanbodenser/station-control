package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = Seccion::class,
            parentColumns = ["id"],
            childColumns = ["seccionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Unidad::class,
            parentColumns = ["id"],
            childColumns = ["unidadId"]
        )
    ],
    indices = [
        Index("seccionId"),
        Index("unidadId")
    ]
)
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seccionId: Long,
    val unidadId: Long,
    val nombre: String,
    val orden: Int,
    val activo: Boolean = true
)
