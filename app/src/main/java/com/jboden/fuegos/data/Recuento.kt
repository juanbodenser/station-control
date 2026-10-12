package com.jboden.fuegos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recuentos",
    foreignKeys = [
        ForeignKey(
            entity = Seccion::class,
            parentColumns = ["id"],
            childColumns = ["seccionId"]
        )
    ],
    indices = [
        Index("seccionId")
    ]
)
data class Recuento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seccionId: Long,
    val fecha: Long
)
