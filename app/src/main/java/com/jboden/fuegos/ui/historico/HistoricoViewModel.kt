package com.jboden.fuegos.ui.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.data.MovimientoHistorial
import com.jboden.fuegos.data.Producto
import com.jboden.fuegos.data.Seccion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoricoViewModel(private val database: FuegosDatabase) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val secciones: StateFlow<List<Seccion>> = database.seccionDao().getSecciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSeccionId = MutableStateFlow<Long?>(null)
    val selectedSeccionId: StateFlow<Long?> = _selectedSeccionId.asStateFlow()

    private val _selectedProductoId = MutableStateFlow<Long?>(0L) // 0 means "Todos"
    val selectedProductoId: StateFlow<Long?> = _selectedProductoId.asStateFlow()

    fun setSelectedSeccionId(id: Long) {
        _selectedSeccionId.value = id
        _selectedProductoId.value = 0L
    }

    fun setSelectedProductoId(id: Long?) {
        _selectedProductoId.value = id ?: 0L
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val productosSeccion: StateFlow<List<Producto>> = _selectedSeccionId
        .flatMapLatest { seccionId ->
            if (seccionId != null) {
                database.productoDao().getProductosConUnidadTodosPorSeccion(seccionId)
                    .map { list -> list.map { it.producto } }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recuentosFlow = database.recuentoDao().getRecuentoLineasConDetalle()
        .map { list ->
            list.map {
                MovimientoHistorial.RecuentoMovimiento(
                    lineaRecuentoId = it.lineaRecuentoId,
                    recuentoId = it.recuentoId,
                    productoId = it.productoId,
                    productoNombre = it.productoNombre,
                    unidadAbreviatura = it.unidadAbreviatura,
                    cantidad = it.cantidad,
                    fechaMillis = it.fecha,
                    seccionId = it.seccionId,
                    seccionNombre = it.seccionNombre
                )
            }
        }

    private val entradasFlow = database.entradaDao().getEntradasConDetalle()
        .map { list ->
            list.map {
                MovimientoHistorial.EntradaMovimiento(
                    entradaId = it.entradaId,
                    productoId = it.productoId,
                    productoNombre = it.productoNombre,
                    unidadAbreviatura = it.unidadAbreviatura,
                    cantidad = it.cantidad,
                    fechaMillis = it.fecha,
                    seccionId = it.seccionId,
                    seccionNombre = it.seccionNombre
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val movimientos: StateFlow<List<MovimientoHistorial>> = combine(
        recuentosFlow,
        entradasFlow,
        _selectedSeccionId,
        _selectedProductoId
    ) { recuentos, entradas, seccionId, productoId ->
        val all = (recuentos + entradas).sortedByDescending { it.fechaMillis }
        all.filter { mov ->
            val matchSeccion = seccionId == null || mov.seccionId == seccionId
            val matchProducto = productoId == null || productoId == 0L || mov.productoId == productoId
            matchSeccion && matchProducto
        }.take(500)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateMovimiento(movimiento: MovimientoHistorial, nuevaCantidad: Double) {
        viewModelScope.launch {
            when (movimiento) {
                is MovimientoHistorial.RecuentoMovimiento -> {
                    database.recuentoDao().updateLineaRecuentoCantidad(movimiento.lineaRecuentoId, nuevaCantidad)
                    _userMessage.value = "Recuento actualizado"
                }
                is MovimientoHistorial.EntradaMovimiento -> {
                    database.entradaDao().updateEntradaCantidad(movimiento.entradaId, nuevaCantidad)
                    _userMessage.value = "Entrada actualizada"
                }
            }
        }
    }

    fun deleteMovimiento(movimiento: MovimientoHistorial) {
        viewModelScope.launch {
            when (movimiento) {
                is MovimientoHistorial.RecuentoMovimiento -> {
                    database.recuentoDao().deleteLineaRecuento(movimiento.lineaRecuentoId)
                    val remainingLines = database.recuentoDao().countLineasByRecuento(movimiento.recuentoId)
                    if (remainingLines == 0) {
                        database.recuentoDao().deleteRecuento(movimiento.recuentoId)
                    }
                    _userMessage.value = "Movimiento borrado"
                }
                is MovimientoHistorial.EntradaMovimiento -> {
                    database.entradaDao().deleteEntrada(movimiento.entradaId)
                    _userMessage.value = "Movimiento borrado"
                }
            }
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }
}

class HistoricoViewModelFactory(private val database: FuegosDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricoViewModel::class.java)) {
            return HistoricoViewModel(database) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
