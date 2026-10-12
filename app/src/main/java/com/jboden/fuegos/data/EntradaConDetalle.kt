package com.jboden.fuegos.data

data class EntradaConDetalle(
    val entradaId: Long,
    val productoId: Long,
    val productoNombre: String,
    val unidadAbreviatura: String,
    val cantidad: Double,
    val fecha: Long,
    val seccionId: Long,
    val seccionNombre: String
)
