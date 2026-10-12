package com.jboden.fuegos.data

data class RecuentoLineaConDetalle(
    val lineaRecuentoId: Long,
    val recuentoId: Long,
    val productoId: Long,
    val productoNombre: String,
    val unidadAbreviatura: String,
    val cantidad: Double,
    val fecha: Long,
    val seccionId: Long,
    val seccionNombre: String
)
