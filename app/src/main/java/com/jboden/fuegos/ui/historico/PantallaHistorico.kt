package com.jboden.fuegos.ui.historico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jboden.fuegos.data.MovimientoHistorial
import com.jboden.fuegos.ui.recuento.formatCantidadConComa
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Long.formatFechaLarga(): String {
    val date = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.forLanguageTag("es-ES"))
    return date.format(formatter)
}

fun Long.formatHora(): String {
    val time = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return time.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorico(
    viewModel: HistoricoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secciones by viewModel.secciones.collectAsStateWithLifecycle()
    val productos by viewModel.productosSeccion.collectAsStateWithLifecycle()
    val selectedSeccionId by viewModel.selectedSeccionId.collectAsStateWithLifecycle()
    val selectedProductoId by viewModel.selectedProductoId.collectAsStateWithLifecycle()
    val movimientos by viewModel.movimientos.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onUserMessageShown()
        }
    }

    LaunchedEffect(secciones, selectedSeccionId) {
        if (selectedSeccionId == null && secciones.isNotEmpty()) {
            viewModel.setSelectedSeccionId(secciones.first().id)
        }
    }

    val selectedSeccion = secciones.find { it.id == selectedSeccionId }
    val selectedProducto = if (selectedProductoId == 0L || selectedProductoId == null) null else productos.find { it.id == selectedProductoId }

    var editingMovimiento by remember { mutableStateOf<MovimientoHistorial?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Histórico", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var seccionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = seccionExpanded,
                    onExpandedChange = { seccionExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedSeccion?.nombre ?: "Sección",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sección") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seccionExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = seccionExpanded,
                        onDismissRequest = { seccionExpanded = false }
                    ) {
                        secciones.forEach { seccion ->
                            DropdownMenuItem(
                                text = { Text(seccion.nombre) },
                                onClick = {
                                    viewModel.setSelectedSeccionId(seccion.id)
                                    seccionExpanded = false
                                }
                            )
                        }
                    }
                }

                var productoExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = productoExpanded,
                    onExpandedChange = { productoExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedProducto?.nombre ?: "Todos",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productoExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productoExpanded,
                        onDismissRequest = { productoExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = {
                                viewModel.setSelectedProductoId(0L)
                                productoExpanded = false
                            }
                        )
                        productos.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text(prod.nombre) },
                                onClick = {
                                    viewModel.setSelectedProductoId(prod.id)
                                    productoExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (movimientos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todavía no hay movimientos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val agrupadosPorDia = movimientos.groupBy { it.fechaMillis.formatFechaLarga() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    agrupadosPorDia.forEach { (fechaStr, listaMovs) ->
                        item(key = "d-$fechaStr") {
                            Text(
                                text = fechaStr.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            listaMovs,
                            key = { mov ->
                                when (mov) {
                                    is MovimientoHistorial.RecuentoMovimiento -> "r-${mov.id}"
                                    is MovimientoHistorial.EntradaMovimiento -> "e-${mov.id}"
                                }
                            }
                        ) { mov ->
                            MovimientoRow(
                                movimiento = mov,
                                onClick = { editingMovimiento = mov }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    editingMovimiento?.let { mov ->
        EditarMovimientoDialog(
            movimiento = mov,
            onDismiss = { editingMovimiento = null },
            onSave = { nuevaCantidad ->
                viewModel.updateMovimiento(mov, nuevaCantidad)
                editingMovimiento = null
            },
            onDelete = {
                viewModel.deleteMovimiento(mov)
                editingMovimiento = null
            }
        )
    }
}

@Composable
fun MovimientoRow(
    movimiento: MovimientoHistorial,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horaStr = movimiento.fechaMillis.formatHora()
    val cantidadFormateada = movimiento.cantidad.formatCantidadConComa()

    val (tipoTexto, cantidadTexto, icon, color) = when (movimiento) {
        is MovimientoHistorial.RecuentoMovimiento -> {
            Quad(
                "Recuento",
                "$cantidadFormateada ${movimiento.unidadAbreviatura}",
                Icons.Default.Assessment,
                MaterialTheme.colorScheme.primary
            )
        }
        is MovimientoHistorial.EntradaMovimiento -> {
            Quad(
                "Entrada",
                "+$cantidadFormateada ${movimiento.unidadAbreviatura}",
                Icons.Default.AddCircle,
                MaterialTheme.colorScheme.tertiary
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tipoTexto,
                tint = color
            )
            Column {
                Text(
                    text = "$tipoTexto · ${movimiento.productoNombre}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movimiento.seccionNombre} · $horaStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = cantidadTexto,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EditarMovimientoDialog(
    movimiento: MovimientoHistorial,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    var cantidadText by remember { mutableStateOf(movimiento.cantidad.toString().replace('.', ',')) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${movimiento.productoNombre} (${movimiento.seccionNombre})",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { newValue ->
                        val normalized = newValue.replace(',', '.')
                        if (normalized.isEmpty() || normalized.matches(Regex("""^\d+(\.\d*)?$"""))) {
                            cantidadText = newValue
                        }
                    },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Borrar")
                }
                TextButton(
                    onClick = {
                        val parsed = cantidadText.replace(',', '.').toDoubleOrNull()
                        if (parsed != null) {
                            onSave(parsed)
                        }
                    },
                    enabled = cantidadText.replace(',', '.').toDoubleOrNull() != null
                ) {
                    Text("Guardar cambios")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar borrado") },
            text = { Text("¿Borrar este movimiento? No se puede deshacer") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
