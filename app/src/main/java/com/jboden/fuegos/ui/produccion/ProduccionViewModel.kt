package com.jboden.fuegos.ui.produccion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.data.TareaProduccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProduccionViewModel(
    private val database: FuegosDatabase,
    private val context: Context,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("fuegos_copia_prefs", Context.MODE_PRIVATE)

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val tareas: StateFlow<List<TareaProduccion>> = database.tareaProduccionDao().getTareas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    fun toggleMarcada(tarea: TareaProduccion) {
        viewModelScope.launch {
            database.tareaProduccionDao().update(tarea.copy(marcada = !tarea.marcada))
        }
    }

    fun desmarcarTodas() {
        viewModelScope.launch {
            database.tareaProduccionDao().desmarcarTodas()
            _userMessage.value = "Tareas desmarcadas"
        }
    }

    fun getEncabezado(): String {
        return prefs.getString("plantilla_encabezado", "Producción para Fuegos:") ?: "Producción para Fuegos:"
    }

    fun saveEncabezado(text: String) {
        prefs.edit().putString("plantilla_encabezado", text.trim()).apply()
        _userMessage.value = "Encabezado guardado"
    }

    fun getCierre(): String {
        return prefs.getString("plantilla_cierre", "Gracias, buenas noches.") ?: "Gracias, buenas noches."
    }

    fun saveCierre(text: String) {
        prefs.edit().putString("plantilla_cierre", text.trim()).apply()
        _userMessage.value = "Cierre guardado"
    }

    fun generarMensaje(): String {
        val encabezado = getEncabezado()
        val cierre = getCierre()
        val marcadas = tareas.value.filter { it.marcada }

        val sb = StringBuilder()
        sb.append(encabezado).append("\n")
        marcadas.forEach { tarea ->
            sb.append("- ").append(tarea.texto).append("\n")
        }
        sb.append("\n").append(cierre)
        return sb.toString()
    }

    // --- CRUD Tareas para Ajustes ---
    fun addTarea(texto: String, onResult: (Boolean) -> Unit) {
        val trimmed = texto.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El texto no puede estar vacío"
            onResult(false)
            return
        }
        if (tareas.value.any { it.texto.equals(trimmed, ignoreCase = true) }) {
            _userMessage.value = "Ya existe una tarea con ese texto"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val nuevoOrden = (tareas.value.maxOfOrNull { it.orden } ?: 0) + 1
            database.tareaProduccionDao().insert(TareaProduccion(texto = trimmed, orden = nuevoOrden, marcada = false))
            onResult(true)
        }
    }

    fun updateTarea(tarea: TareaProduccion, nuevoTexto: String, onResult: (Boolean) -> Unit) {
        val trimmed = nuevoTexto.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "El texto no puede estar vacío"
            onResult(false)
            return
        }
        if (tareas.value.any { it.id != tarea.id && it.texto.equals(trimmed, ignoreCase = true) }) {
            _userMessage.value = "Ya existe otra tarea con ese texto"
            onResult(false)
            return
        }
        viewModelScope.launch {
            database.tareaProduccionDao().update(tarea.copy(texto = trimmed))
            onResult(true)
        }
    }

    fun deleteTarea(tarea: TareaProduccion) {
        viewModelScope.launch {
            database.tareaProduccionDao().delete(tarea)
            reorderTareas()
        }
    }

    fun moveTarea(tareaId: Long, direction: Int) {
        viewModelScope.launch {
            val mutableList = tareas.value.toMutableList()
            val index = mutableList.indexOfFirst { it.id == tareaId }
            if (index < 0) return@launch
            val targetIndex = index + direction
            if (targetIndex < 0 || targetIndex >= mutableList.size) return@launch

            val item = mutableList.removeAt(index)
            mutableList.add(targetIndex, item)

            val updated = mutableList.mapIndexed { idx, t -> t.copy(orden = idx + 1) }
            database.tareaProduccionDao().updateAll(updated)
        }
    }

    private suspend fun reorderTareas() {
        val list = tareas.value.sortedBy { it.orden }
        val updated = list.mapIndexed { idx, t -> t.copy(orden = idx + 1) }
        database.tareaProduccionDao().updateAll(updated)
    }
}

class ProduccionViewModelFactory(
    private val database: FuegosDatabase,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProduccionViewModel::class.java)) {
            return ProduccionViewModel(database, context) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
