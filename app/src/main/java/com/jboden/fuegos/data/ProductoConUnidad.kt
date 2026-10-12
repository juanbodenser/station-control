package com.jboden.fuegos.data

import androidx.room.Embedded

data class ProductoConUnidad(
    @Embedded
    val producto: Producto,
    val abreviaturaUnidad: String
)
