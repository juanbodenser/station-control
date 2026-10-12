package com.jboden.fuegos.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

data class PuntoRecuento(
    val fecha: Long,
    val cantidad: Double
)

data class EntradaItem(
    val fecha: Long,
    val cantidad: Double
)

data class ResultadoTramo(
    val fechaInicio: Long,
    val fechaFin: Long,
    val cantidadInicial: Double,
    val entradas: Double,
    val cantidadFinal: Double,
    val consumo: Double,
    val esIncoherente: Boolean
)

data class ResultadoProducto(
    val productoId: Long,
    val productoNombre: String,
    val unidadAbreviatura: String,
    val consumoTotal: Double,
    val consumoDiarioMedio: Double?,
    val rangoFechasStr: String?,
    val tramosIncoherentesCount: Int,
    val tieneDatosSuficientes: Boolean,
    val tramos: List<ResultadoTramo> = emptyList()
)

class CalculadorConsumo {

    fun calcularConsumoProducto(
        productoId: Long,
        productoNombre: String,
        unidadAbreviatura: String,
        recuentos: List<PuntoRecuento>,
        entradas: List<EntradaItem>,
        periodoInicio: Long,
        periodoFin: Long
    ): ResultadoProducto {
        val recuentosOrdenados = recuentos.sortedBy { it.fecha }

        val tramosValidos = mutableListOf<ResultadoTramo>()
        var tramosIncoherentesCount = 0

        for (i in 0 until recuentosOrdenados.size - 1) {
            val anterior = recuentosOrdenados[i]
            val actual = recuentosOrdenados[i + 1]

            val dentroPeriodo = anterior.fecha in periodoInicio..periodoFin &&
                    actual.fecha in periodoInicio..periodoFin

            if (dentroPeriodo) {
                // (fechaAnterior, fechaActual] -> exclusivo inicio, inclusivo final
                val sumaEntradas = entradas
                    .filter { it.fecha > anterior.fecha && it.fecha <= actual.fecha }
                    .sumOf { it.cantidad }

                val consumoTramo = anterior.cantidad + sumaEntradas - actual.cantidad
                val esIncoherente = consumoTramo < 0.0

                if (esIncoherente) {
                    tramosIncoherentesCount++
                }

                tramosValidos.add(
                    ResultadoTramo(
                        fechaInicio = anterior.fecha,
                        fechaFin = actual.fecha,
                        cantidadInicial = anterior.cantidad,
                        entradas = sumaEntradas,
                        cantidadFinal = actual.cantidad,
                        consumo = consumoTramo,
                        esIncoherente = esIncoherente
                    )
                )
            }
        }

        if (tramosValidos.isEmpty()) {
            return ResultadoProducto(
                productoId = productoId,
                productoNombre = productoNombre,
                unidadAbreviatura = unidadAbreviatura,
                consumoTotal = 0.0,
                consumoDiarioMedio = null,
                rangoFechasStr = null,
                tramosIncoherentesCount = tramosIncoherentesCount,
                tieneDatosSuficientes = false,
                tramos = emptyList()
            )
        }

        val tramosCoherentes = tramosValidos.filter { !it.esIncoherente }
        val consumoTotal = tramosCoherentes.sumOf { it.consumo }

        val primerTramo = tramosValidos.first()
        val ultimoTramo = tramosValidos.last()
        val rangoFechasStr = formatearRangoFechas(primerTramo.fechaInicio, ultimoTramo.fechaFin)

        val diasTranscurridos = max(
            1L,
            (ultimoTramo.fechaFin - primerTramo.fechaInicio) / (1000 * 60 * 60 * 24)
        )
        val mismoDia = esMismoDia(primerTramo.fechaInicio, ultimoTramo.fechaFin)
        val consumoDiarioMedio = if (mismoDia || tramosCoherentes.isEmpty()) {
            null
        } else {
            consumoTotal / diasTranscurridos
        }

        return ResultadoProducto(
            productoId = productoId,
            productoNombre = productoNombre,
            unidadAbreviatura = unidadAbreviatura,
            consumoTotal = consumoTotal,
            consumoDiarioMedio = consumoDiarioMedio,
            rangoFechasStr = rangoFechasStr,
            tramosIncoherentesCount = tramosIncoherentesCount,
            tieneDatosSuficientes = true,
            tramos = tramosValidos
        )
    }

    private fun esMismoDia(millis1: Long, millis2: Long): Boolean {
        val d1 = Instant.ofEpochMilli(millis1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(millis2).atZone(ZoneId.systemDefault()).toLocalDate()
        return d1 == d2
    }

    private fun formatearRangoFechas(millisInicio: Long, millisFin: Long): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))
        val d1 = Instant.ofEpochMilli(millisInicio).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(millisFin).atZone(ZoneId.systemDefault()).toLocalDate()
        return "${d1.format(formatter)} – ${d2.format(formatter)}"
    }
}
