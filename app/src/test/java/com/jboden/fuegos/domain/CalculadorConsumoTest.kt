package com.jboden.fuegos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalculadorConsumoTest {

    private val calculador = CalculadorConsumo()

    private fun millisDe(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testTramoSimple() {
        val inicioPeriodo = millisDe(2026, 10, 1, 0)
        val finPeriodo = millisDe(2026, 10, 10, 23)

        val recuentos = listOf(
            PuntoRecuento(millisDe(2026, 10, 2), 10.0),
            PuntoRecuento(millisDe(2026, 10, 5), 4.0)
        )
        val entradas = listOf(
            EntradaItem(millisDe(2026, 10, 3), 2.0)
        )

        val resultado = calculador.calcularConsumoProducto(
            1L, "Pulpo", "kg",
            recuentos, entradas,
            inicioPeriodo, finPeriodo
        )

        assertTrue(resultado.tieneDatosSuficientes)
        // Consumo = 10.0 (inicial) + 2.0 (entradas) - 4.0 (final) = 8.0
        assertEquals(8.0, resultado.consumoTotal, 0.001)
        assertEquals(0, resultado.tramosIncoherentesCount)
        assertEquals(1, resultado.tramos.size)
    }

    @Test
    fun testVariosTramos() {
        val inicioPeriodo = millisDe(2026, 10, 1, 0)
        val finPeriodo = millisDe(2026, 10, 15, 23)

        val recuentos = listOf(
            PuntoRecuento(millisDe(2026, 10, 2), 10.0),
            PuntoRecuento(millisDe(2026, 10, 5), 6.0),
            PuntoRecuento(millisDe(2026, 10, 10), 2.0)
        )
        val entradas = emptyList<EntradaItem>()

        val resultado = calculador.calcularConsumoProducto(
            1L, "Pulpo", "kg",
            recuentos, entradas,
            inicioPeriodo, finPeriodo
        )

        assertTrue(resultado.tieneDatosSuficientes)
        // Tramo 1: 10 - 6 = 4
        // Tramo 2: 6 - 2 = 4
        // Total = 8.0
        assertEquals(8.0, resultado.consumoTotal, 0.001)
        assertEquals(2, resultado.tramos.size)
    }

    @Test
    fun testEntradaMismaFechaRecuentoFinal() {
        val fechaFin = millisDe(2026, 10, 5, 15)
        val inicioPeriodo = millisDe(2026, 10, 1, 0)
        val finPeriodo = millisDe(2026, 10, 10, 23)

        val recuentos = listOf(
            PuntoRecuento(millisDe(2026, 10, 2), 10.0),
            PuntoRecuento(fechaFin, 4.0)
        )
        // Entrada con EXACTAMENTE la misma fecha que el recuento final (debe incluirse en (fechaAnterior, fechaActual])
        val entradas = listOf(
            EntradaItem(fechaFin, 5.0)
        )

        val resultado = calculador.calcularConsumoProducto(
            1L, "Pulpo", "kg",
            recuentos, entradas,
            inicioPeriodo, finPeriodo
        )

        assertTrue(resultado.tieneDatosSuficientes)
        // Consumo = 10.0 + 5.0 - 4.0 = 11.0
        assertEquals(11.0, resultado.consumoTotal, 0.001)
    }

    @Test
    fun testTramoNegativoIncoherente() {
        val inicioPeriodo = millisDe(2026, 10, 1, 0)
        val finPeriodo = millisDe(2026, 10, 10, 23)

        val recuentos = listOf(
            PuntoRecuento(millisDe(2026, 10, 2), 5.0),
            PuntoRecuento(millisDe(2026, 10, 5), 10.0) // subió sin entradas -> consumo negativo (-5.0)
        )
        val entradas = emptyList<EntradaItem>()

        val resultado = calculador.calcularConsumoProducto(
            1L, "Pulpo", "kg",
            recuentos, entradas,
            inicioPeriodo, finPeriodo
        )

        assertTrue(resultado.tieneDatosSuficientes)
        assertEquals(0.0, resultado.consumoTotal, 0.001)
        assertEquals(1, resultado.tramosIncoherentesCount)
        assertTrue(resultado.tramos[0].esIncoherente)
    }

    @Test
    fun testUnicoRecuentoSinDatosSuficientes() {
        val inicioPeriodo = millisDe(2026, 10, 1, 0)
        val finPeriodo = millisDe(2026, 10, 10, 23)

        val recuentos = listOf(
            PuntoRecuento(millisDe(2026, 10, 2), 10.0)
        )
        val entradas = emptyList<EntradaItem>()

        val resultado = calculador.calcularConsumoProducto(
            1L, "Pulpo", "kg",
            recuentos, entradas,
            inicioPeriodo, finPeriodo
        )

        assertFalse(resultado.tieneDatosSuficientes)
        assertEquals(0.0, resultado.consumoTotal, 0.001)
    }
}
