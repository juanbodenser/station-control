package com.jboden.fuegos.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.data.Origen
import com.jboden.fuegos.data.Producto
import com.jboden.fuegos.data.ProductoConUnidad
import com.jboden.fuegos.data.Seccion
import com.jboden.fuegos.data.Unidad
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AjustesViewModel(private val database: FuegosDatabase) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val secciones: StateFlow<List<Seccion>> = database.seccionDao().getSecciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unidades: StateFlow<List<Unidad>> = database.unidadDao().getUnidades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSeccionId = MutableStateFlow<Long?>(null)
    val selectedSeccionId: StateFlow<Long?> = _selectedSeccionId.asStateFlow()

    fun setSelectedSeccionId(id: Long) {
        _selectedSeccionId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val productosSeccion: StateFlow<List<ProductoConUnidad>> = _selectedSeccionId
        .flatMapLatest { seccionId ->
            if (seccionId != null) {
                database.productoDao().getProductosConUnidadTodosPorSeccion(seccionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    // --- SECCIONES ---
    fun addSeccion(nombre: String, origen: Origen, onResult: (Boolean) -> Unit) {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El nombre no puede estar vacío"
            onResult(false)
            return
        }
        if (secciones.value.any { it.nombre.equals(trimmed, ignoreCase = true) }) {
            _userMessage.value = "Ya existe una sección con ese nombre"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val nuevoOrden = (secciones.value.maxOfOrNull { it.orden } ?: 0) + 1
            database.seccionDao().insert(Seccion(nombre = trimmed, orden = nuevoOrden, origen = origen))
            onResult(true)
        }
    }

    fun updateSeccion(seccion: Seccion, nuevoNombre: String, nuevoOrigen: Origen, onResult: (Boolean) -> Unit) {
        val trimmed = nuevoNombre.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El nombre no puede estar vacío"
            onResult(false)
            return
        }
        if (secciones.value.any { it.id != seccion.id && it.nombre.equals(trimmed, ignoreCase = true) }) {
            _userMessage.value = "Ya existe otra sección con ese nombre"
            onResult(false)
            return
        }
        viewModelScope.launch {
            database.seccionDao().update(seccion.copy(nombre = trimmed, origen = nuevoOrigen))
            onResult(true)
        }
    }

    fun deleteSeccion(seccion: Seccion, onHasCounts: () -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val count = database.seccionDao().countRecuentosBySeccion(seccion.id)
            if (count > 0) {
                onHasCounts()
            } else {
                database.seccionDao().delete(seccion)
                reorderSecciones()
                onSuccess()
            }
        }
    }

    fun moveSeccion(seccionId: Long, direction: Int) {
        viewModelScope.launch {
            val mutableList = secciones.value.toMutableList()
            val index = mutableList.indexOfFirst { it.id == seccionId }
            if (index < 0) return@launch
            val targetIndex = index + direction
            if (targetIndex < 0 || targetIndex >= mutableList.size) return@launch

            val item = mutableList.removeAt(index)
            mutableList.add(targetIndex, item)

            val updated = mutableList.mapIndexed { idx, s -> s.copy(orden = idx + 1) }
            database.seccionDao().updateAll(updated)
        }
    }

    private suspend fun reorderSecciones() {
        val list = secciones.value.sortedBy { it.orden }
        val updated = list.mapIndexed { idx, s -> s.copy(orden = idx + 1) }
        database.seccionDao().updateAll(updated)
    }

    // --- PRODUCTOS ---
    fun addProducto(seccionId: Long, unidadId: Long, nombre: String, onResult: (Boolean) -> Unit) {
        val trimmed = nombre.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El nombre no puede estar vacío"
            onResult(false)
            return
        }
        val currentProducts = productosSeccion.value
        if (currentProducts.any { it.producto.nombre.equals(trimmed, ignoreCase = true) }) {
            _userMessage.value = "Ya existe un producto con ese nombre en esta sección"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val nuevoOrden = (currentProducts.maxOfOrNull { it.producto.orden } ?: 0) + 1
            database.productoDao().insert(
                Producto(
                    seccionId = seccionId,
                    unidadId = unidadId,
                    nombre = trimmed,
                    orden = nuevoOrden,
                    activo = true
                )
            )
            onResult(true)
        }
    }

    fun updateProducto(
        producto: Producto,
        nuevoNombre: String,
        nuevaSeccionId: Long,
        nuevaUnidadId: Long,
        onResult: (Boolean) -> Unit
    ) {
        val trimmed = nuevoNombre.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El nombre no puede estar vacío"
            onResult(false)
            return
        }
        viewModelScope.launch {
            database.productoDao().update(
                producto.copy(
                    nombre = trimmed,
                    seccionId = nuevaSeccionId,
                    unidadId = nuevaUnidadId
                )
            )
            onResult(true)
        }
    }

    fun toggleProductoActivo(producto: Producto) {
        viewModelScope.launch {
            database.productoDao().update(producto.copy(activo = !producto.activo))
        }
    }

    fun ocultarProducto(producto: Producto) {
        viewModelScope.launch {
            database.productoDao().update(producto.copy(activo = false))
        }
    }

    fun deleteProducto(producto: Producto, onHasHistory: () -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val lineasCount = database.productoDao().countLineasRecuentoByProducto(producto.id)
            val entradasCount = database.productoDao().countEntradasByProducto(producto.id)
            if (lineasCount > 0 || entradasCount > 0) {
                onHasHistory()
            } else {
                database.productoDao().delete(producto)
                reorderProductos()
                onSuccess()
            }
        }
    }

    fun moveProducto(productoId: Long, direction: Int) {
        viewModelScope.launch {
            val mutableList = productosSeccion.value.toMutableList()
            val index = mutableList.indexOfFirst { it.producto.id == productoId }
            if (index < 0) return@launch
            val targetIndex = index + direction
            if (targetIndex < 0 || targetIndex >= mutableList.size) return@launch

            val item = mutableList.removeAt(index)
            mutableList.add(targetIndex, item)

            val updated = mutableList.mapIndexed { idx, p -> p.producto.copy(orden = idx + 1) }
            database.productoDao().updateAll(updated)
        }
    }

    private suspend fun reorderProductos() {
        val list = productosSeccion.value.sortedBy { it.producto.orden }
        val updated = list.mapIndexed { idx, p -> p.producto.copy(orden = idx + 1) }
        database.productoDao().updateAll(updated)
    }

    // --- UNIDADES ---
    fun addUnidad(nombre: String, abreviatura: String, onResult: (Boolean) -> Unit) {
        val trimNombre = nombre.trim()
        val trimAbbr = abreviatura.trim()
        if (trimNombre.isEmpty() || trimAbbr.isEmpty()) {
            _userMessage.value = "Nombre y abreviatura no pueden estar vacíos"
            onResult(false)
            return
        }
        val currentUnidades = unidades.value
        if (currentUnidades.any { it.nombre.equals(trimNombre, ignoreCase = true) || it.abreviatura.equals(trimAbbr, ignoreCase = true) }) {
            _userMessage.value = "Ya existe una unidad con ese nombre o abreviatura"
            onResult(false)
            return
        }
        viewModelScope.launch {
            database.unidadDao().insert(Unidad(nombre = trimNombre, abreviatura = trimAbbr))
            onResult(true)
        }
    }

    fun updateUnidad(unidad: Unidad, nuevoNombre: String, nuevaAbreviatura: String, onResult: (Boolean) -> Unit) {
        val trimNombre = nuevoNombre.trim()
        val trimAbbr = nuevaAbreviatura.trim()
        if (trimNombre.isEmpty() || trimAbbr.isEmpty()) {
            _userMessage.value = "Nombre y abreviatura no pueden estar vacíos"
            onResult(false)
            return
        }
        val currentUnidades = unidades.value
        if (currentUnidades.any { it.id != unidad.id && (it.nombre.equals(trimNombre, ignoreCase = true) || it.abreviatura.equals(trimAbbr, ignoreCase = true)) }) {
            _userMessage.value = "Ya existe otra unidad con ese nombre o abreviatura"
            onResult(false)
            return
        }
        viewModelScope.launch {
            database.unidadDao().update(unidad.copy(nombre = trimNombre, abreviatura = trimAbbr))
            onResult(true)
        }
    }

    fun deleteUnidad(unidad: Unidad, onInUse: () -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val count = database.unidadDao().countProductosByUnidad(unidad.id)
            if (count > 0) {
                onInUse()
            } else {
                database.unidadDao().delete(unidad)
                onSuccess()
            }
        }
    }
}

class AjustesViewModelFactory(private val database: FuegosDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AjustesViewModel::class.java)) {
            return AjustesViewModel(database) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
