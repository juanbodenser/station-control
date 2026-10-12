package com.jboden.fuegos.ui.copia

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboden.fuegos.data.CopiaSeguridad
import com.jboden.fuegos.data.FuegosDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CopiaViewModel(
    private val database: FuegosDatabase,
    private val context: Context,
) : ViewModel() {

    private val copiaSeguridad = CopiaSeguridad(database, context)
    private val prefs = context.getSharedPreferences("fuegos_copia_prefs", Context.MODE_PRIVATE)

    private val _ultimaCopia = MutableStateFlow(cargarUltimaCopia())
    val ultimaCopia: StateFlow<String> = _ultimaCopia.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    private fun cargarUltimaCopia(): String {
        val millis = prefs.getLong("ultima_copia_millis", 0L)
        return if (millis == 0L) {
            "nunca"
        } else {
            millis.formatFechaHora()
        }
    }

    fun diasDesdeUltimaCopia(): Long? {
        val millis = prefs.getLong("ultima_copia_millis", 0L)
        if (millis == 0L) return null
        return (System.currentTimeMillis() - millis) / (1000 * 60 * 60 * 24)
    }

    fun exportarCopia(uri: Uri) {
        viewModelScope.launch {
            copiaSeguridad.exportarCopia(uri)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    prefs.edit().putLong("ultima_copia_millis", now).apply()
                    _ultimaCopia.value = now.formatFechaHora()
                    _userMessage.value = "Copia guardada"
                }
                .onFailure { e ->
                    _userMessage.value = "Error al exportar copia: ${e.localizedMessage ?: "desconocido"}"
                }
        }
    }

    fun importarCopia(uri: Uri) {
        viewModelScope.launch {
            copiaSeguridad.importarCopia(uri)
                .onSuccess {
                    _userMessage.value = "Copia restaurada"
                }
                .onFailure { e ->
                    _userMessage.value = "Error al importar copia: ${e.localizedMessage ?: "archivo inválido"}"
                }
        }
    }

    private fun Long.formatFechaHora(): String {
        val zonedDateTime = Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
        return zonedDateTime.format(formatter)
    }
}

class CopiaViewModelFactory(
    private val database: FuegosDatabase,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CopiaViewModel::class.java)) {
            return CopiaViewModel(database, context) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
