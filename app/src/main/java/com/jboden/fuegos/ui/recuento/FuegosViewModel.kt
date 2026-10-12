package com.jboden.fuegos.ui.recuento

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.Entrada
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.data.LineaRecuento
import com.jboden.fuegos.data.ProductoConUnidad
import com.jboden.fuegos.data.Recuento
import com.jboden.fuegos.data.Seccion
import com.jboden.fuegos.data.UltimoDato
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeccionUiState(
    val seccion: Seccion,
    val productos: List<ProductoConUnidad> = emptyList(),
    val ultimasCantidades: Map<Long, UltimoDato> = emptyMap(),
    val entradasPosteriores: Map<Long, Double> = emptyMap(),
)

data class RecuentoUiState(
    val secciones: List<SeccionUiState> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
)

class FuegosViewModel(private val database: FuegosDatabase) : ViewModel() {

    private val _cantidadesIngresadas = mutableStateMapOf<Long, String>()
    val cantidadesIngresadas: Map<Long, String> get() = _cantidadesIngresadas

    private val _entradasIngresadas = mutableStateMapOf<Long, String>()
    val entradasIngresadas: Map<Long, String> get() = _entradasIngresadas

    private val _userMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val seccionesUiState: Flow<List<SeccionUiState>> =
        database.seccionDao().getSecciones().flatMapLatest { secciones ->
            if (secciones.isEmpty()) {
                flowOf(emptyList())
            } else {
                val seccionFlows = secciones.map { seccion ->
                    val productosFlow = database.productoDao().getProductosConUnidadPorSeccion(seccion.id)
                    val ultimasCantidadesFlow = database.recuentoDao().getUltimasCantidadesPorSeccion(seccion.id)
                        .map { lista ->
                            lista.associateBy(
                                { it.productoId },
                                { UltimoDato(it.cantidad, it.fecha) }
                            )
                        }
                    val entradasPosterioresFlow = database.entradaDao().getSumaEntradasPosterioresPorSeccion(seccion.id)
                        .map { lista ->
                            lista.filter { it.sumaEntradas > 0.0 }
                                .associateBy({ it.productoId }, { it.sumaEntradas })
                        }

                    combine(
                        productosFlow,
                        ultimasCantidadesFlow,
                        entradasPosterioresFlow
                    ) { productos, ultimas, entradas ->
                        SeccionUiState(
                            seccion = seccion,
                            productos = productos,
                            ultimasCantidades = ultimas,
                            entradasPosteriores = entradas
                        )
                    }
                }
                combine(seccionFlows) { array -> array.toList() }
            }
        }

    val uiState: StateFlow<RecuentoUiState> = combine(
        seccionesUiState,
        _userMessage
    ) { secciones, message ->
        RecuentoUiState(
            secciones = secciones,
            isLoading = false,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecuentoUiState(isLoading = true)
    )

    fun tieneCambiosSinGuardar(seccionUiState: SeccionUiState): Boolean {
        val prodIds = seccionUiState.productos.map { it.producto.id }.toSet()
        return prodIds.any { id ->
            !_cantidadesIngresadas[id].isNullOrBlank() || !_entradasIngresadas[id].isNullOrBlank()
        }
    }

    fun onCantidadChanged(productoId: Long, text: String) {
        val normalized = text.replace(',', '.')
        if (normalized.isEmpty() || normalized.matches(Regex("""^\d+(\.\d*)?$"""))) {
            if (normalized.isEmpty()) {
                _cantidadesIngresadas.remove(productoId)
            } else {
                _cantidadesIngresadas[productoId] = normalized
            }
        }
    }

    fun onEntradaChanged(productoId: Long, text: String) {
        val normalized = text.replace(',', '.')
        if (normalized.isEmpty() || normalized.matches(Regex("""^\d+(\.\d*)?$"""))) {
            if (normalized.isEmpty()) {
                _entradasIngresadas.remove(productoId)
            } else {
                _entradasIngresadas[productoId] = normalized
            }
        }
    }

    fun guardarRecuento(seccionUiState: SeccionUiState) {
        val lineasAInsertar = mutableListOf<LineaRecuento>()
        val entradasAInsertar = mutableListOf<Pair<Long, Double>>()

        for (prod in seccionUiState.productos) {
            val prodId = prod.producto.id

            val textHay = _cantidadesIngresadas[prodId]?.trim()
            if (!textHay.isNullOrEmpty()) {
                val cant = textHay.toDoubleOrNull()
                if (cant != null) {
                    lineasAInsertar.add(
                        LineaRecuento(
                            recuentoId = 0,
                            productoId = prodId,
                            cantidad = cant
                        )
                    )
                }
            }

            val textEntro = _entradasIngresadas[prodId]?.trim()
            if (!textEntro.isNullOrEmpty()) {
                val cant = textEntro.toDoubleOrNull()
                if (cant != null) {
                    entradasAInsertar.add(prodId to cant)
                }
            }
        }

        if (lineasAInsertar.isEmpty() && entradasAInsertar.isEmpty()) {
            _userMessage.value = "Introduce al menos una cantidad para guardar"
        } else {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                var countRecuentosGuardados = 0
                var countEntradasGuardadas = 0

                if (lineasAInsertar.isNotEmpty()) {
                    val recuento = Recuento(
                        seccionId = seccionUiState.seccion.id,
                        fecha = now
                    )
                    database.recuentoDao().insertRecuentoConLineas(recuento, lineasAInsertar)
                    countRecuentosGuardados = 1
                }

                if (entradasAInsertar.isNotEmpty()) {
                    val origenSeccion = seccionUiState.seccion.origen
                    val entradas = entradasAInsertar.map { (prodId, cant) ->
                        Entrada(
                            productoId = prodId,
                            fecha = now,
                            cantidad = cant,
                            origen = origenSeccion
                        )
                    }
                    database.entradaDao().insertAll(entradas)
                    countEntradasGuardadas = entradas.size
                }

                // Limpiar campos de la sección guardada del mapa
                for (prod in seccionUiState.productos) {
                    _cantidadesIngresadas.remove(prod.producto.id)
                    _entradasIngresadas.remove(prod.producto.id)
                }

                val parts = mutableListOf<String>()
                if (countRecuentosGuardados > 0) {
                    parts.add("$countRecuentosGuardados recuento")
                }
                if (countEntradasGuardadas > 0) {
                    parts.add("$countEntradasGuardadas entrada${if (countEntradasGuardadas > 1) "s" else ""}")
                }
                _userMessage.value = "Guardado: ${parts.joinToString(" y ")}"
            }
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }
}

class FuegosViewModelFactory(private val database: FuegosDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuegosViewModel::class.java)) {
            return FuegosViewModel(database) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
