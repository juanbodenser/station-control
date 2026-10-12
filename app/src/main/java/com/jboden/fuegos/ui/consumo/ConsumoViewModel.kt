package com.jboden.fuegos.ui.consumo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.data.Seccion
import com.jboden.fuegos.domain.CalculadorConsumo
import com.jboden.fuegos.domain.EntradaItem
import com.jboden.fuegos.domain.PuntoRecuento
import com.jboden.fuegos.domain.ResultadoProducto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ResultadoSeccionConsumo(
    val seccion: Seccion,
    val productos: List<ResultadoProducto>
)

class ConsumoViewModel(private val database: FuegosDatabase) : ViewModel() {

    private val calculador = CalculadorConsumo()

    val secciones: StateFlow<List<Seccion>> = database.seccionDao().getSecciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tipoPeriodo = MutableStateFlow("7 días")
    val tipoPeriodo: StateFlow<String> = _tipoPeriodo.asStateFlow()

    private val _customStartMillis = MutableStateFlow<Long?>(null)
    private val _customEndMillis = MutableStateFlow<Long?>(null)

    private val _selectedSeccionId = MutableStateFlow<Long?>(0L) // 0 means "Todas"
    val selectedSeccionId: StateFlow<Long?> = _selectedSeccionId.asStateFlow()

    fun setTipoPeriodo(tipo: String, start: Long? = null, end: Long? = null) {
        _tipoPeriodo.value = tipo
        _customStartMillis.value = start
        _customEndMillis.value = end
    }

    fun setSelectedSeccionId(id: Long?) {
        _selectedSeccionId.value = id ?: 0L
    }

    val resultadosConsumo: StateFlow<List<ResultadoSeccionConsumo>> = combine(
        database.seccionDao().getSecciones(),
        database.productoDao().getAllProductosWithUnit(),
        database.recuentoDao().getRecuentoLineasConDetalle(),
        database.entradaDao().getEntradasConDetalle(),
        _tipoPeriodo
    ) { secciones, todosProductos, recuentoLineas, entradas, tipo ->
        Triple(Triple(secciones, todosProductos, recuentoLineas), entradas, tipo)
    }.combine(
        combine(_customStartMillis, _customEndMillis, _selectedSeccionId) { start, end, seccionFiltro ->
            Triple(start, end, seccionFiltro)
        }
    ) { firstGroup, secondGroup ->
        val secciones = firstGroup.first.first
        val todosProductos = firstGroup.first.second
        val recuentoLineas = firstGroup.first.third
        val entradas = firstGroup.second
        val tipo = firstGroup.third

        val customStart = secondGroup.first
        val customEnd = secondGroup.second
        val seccionFiltro = secondGroup.third

        val (periodoInicio, periodoFin) = getPeriodoMillis(tipo, customStart, customEnd)

        val seccionesAProcesar = if (seccionFiltro != null && seccionFiltro != 0L) {
            secciones.filter { it.id == seccionFiltro }
        } else {
            secciones
        }

        seccionesAProcesar.map { seccion ->
            val productosDeSeccion = todosProductos.filter { it.producto.seccionId == seccion.id }

            val resultadosProductos = productosDeSeccion.map { prodConUnidad ->
                val prod = prodConUnidad.producto
                val lineasProd = recuentoLineas.filter { it.productoId == prod.id }
                val entradasProd = entradas.filter { it.productoId == prod.id }

                val puntosRecuento = lineasProd.map { PuntoRecuento(it.fecha, it.cantidad) }
                val itemsEntrada = entradasProd.map { EntradaItem(it.fecha, it.cantidad) }

                calculador.calcularConsumoProducto(
                    productoId = prod.id,
                    productoNombre = prod.nombre,
                    unidadAbreviatura = prodConUnidad.abreviaturaUnidad,
                    recuentos = puntosRecuento,
                    entradas = itemsEntrada,
                    periodoInicio = periodoInicio,
                    periodoFin = periodoFin
                )
            }

            ResultadoSeccionConsumo(
                seccion = seccion,
                productos = resultadosProductos
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getPeriodoMillis(tipo: String, customStart: Long?, customEnd: Long?): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val startLocalDate = when (tipo) {
            "Hoy" -> now
            "7 días" -> now.minusDays(6)
            "30 días" -> now.minusDays(29)
            "Personalizado" -> {
                if (customStart != null) Instant.ofEpochMilli(customStart).atZone(zone).toLocalDate() else now
            }
            else -> now.minusDays(6)
        }
        val endLocalDate = when (tipo) {
            "Personalizado" -> {
                if (customEnd != null) Instant.ofEpochMilli(customEnd).atZone(zone).toLocalDate() else now
            }
            else -> now
        }

        val startMillis = startLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endLocalDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return Pair(startMillis, endMillis)
    }
}

class ConsumoViewModelFactory(private val database: FuegosDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConsumoViewModel::class.java)) {
            return ConsumoViewModel(database) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
