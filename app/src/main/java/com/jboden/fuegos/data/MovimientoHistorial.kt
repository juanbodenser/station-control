package com.jboden.fuegos.data

sealed class MovimientoHistorial {
    abstract val id: Long
    abstract val productoId: Long
    abstract val productoNombre: String
    abstract val unidadAbreviatura: String
    abstract val cantidad: Double
    abstract val fechaMillis: Long
    abstract val seccionId: Long
    abstract val seccionNombre: String

    data class RecuentoMovimiento(
        val lineaRecuentoId: Long,
        val recuentoId: Long,
        override val productoId: Long,
        override val productoNombre: String,
        override val unidadAbreviatura: String,
        override val cantidad: Double,
        override val fechaMillis: Long,
        override val seccionId: Long,
        override val seccionNombre: String
    ) : MovimientoHistorial() {
        override val id: Long get() = lineaRecuentoId
    }

    data class EntradaMovimiento(
        val entradaId: Long,
        override val productoId: Long,
        override val productoNombre: String,
        override val unidadAbreviatura: String,
        override val cantidad: Double,
        override val fechaMillis: Long,
        override val seccionId: Long,
        override val seccionNombre: String
    ) : MovimientoHistorial() {
        override val id: Long get() = entradaId
    }
}
