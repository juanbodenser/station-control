package com.jboden.fuegos.ui.consumo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jboden.fuegos.domain.ResultadoProducto
import com.jboden.fuegos.domain.ResultadoTramo
import com.jboden.fuegos.ui.recuento.formatCantidadConComa
import com.jboden.fuegos.ui.recuento.formatFechaCorta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConsumo(
    viewModel: ConsumoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secciones by viewModel.secciones.collectAsStateWithLifecycle()
    val tipoPeriodo by viewModel.tipoPeriodo.collectAsStateWithLifecycle()
    val selectedSeccionId by viewModel.selectedSeccionId.collectAsStateWithLifecycle()
    val resultados by viewModel.resultadosConsumo.collectAsStateWithLifecycle()

    var showDateRangePicker by remember { mutableStateOf(false) }
    var selectedProductoForDetail by remember { mutableStateOf<ResultadoProducto?>(null) }

    val selectedSeccion = secciones.find { it.id == selectedSeccionId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Consumo", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val periodos = listOf("Hoy", "7 días", "30 días", "Personalizado")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periodos.forEach { periodo ->
                    FilterChip(
                        selected = tipoPeriodo == periodo,
                        onClick = {
                            if (periodo == "Personalizado") {
                                showDateRangePicker = true
                            } else {
                                viewModel.setTipoPeriodo(periodo)
                            }
                        },
                        label = { Text(periodo) }
                    )
                }
            }

            var seccionExpanded by remember { mutableStateOf(false) }
            val seccionNombre = if (selectedSeccionId == 0L || selectedSeccionId == null) "Todas" else selectedSeccion?.nombre ?: "Todas"

            ExposedDropdownMenuBox(
                expanded = seccionExpanded,
                onExpandedChange = { seccionExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = seccionNombre,
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
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            viewModel.setSelectedSeccionId(0L)
                            seccionExpanded = false
                        }
                    )
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

            if (resultados.all { it.productos.isEmpty() }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos para el periodo seleccionado",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    resultados.forEach { seccionRes ->
                        if (seccionRes.productos.isNotEmpty()) {
                            item(key = "seccion_${seccionRes.seccion.id}") {
                                Text(
                                    text = seccionRes.seccion.nombre,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(
                                seccionRes.productos,
                                key = { prodRes -> "${seccionRes.seccion.id}-${prodRes.productoId}" }
                            ) { prodRes ->
                                ProductoConsumoRow(
                                    resultado = prodRes,
                                    onClick = {
                                        if (prodRes.tieneDatosSuficientes && prodRes.tramos.isNotEmpty()) {
                                            selectedProductoForDetail = prodRes
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis ?: start
                        if (start != null) {
                            viewModel.setTipoPeriodo("Personalizado", start, end)
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }

    selectedProductoForDetail?.let { prodRes ->
        AlertDialog(
            onDismissRequest = { selectedProductoForDetail = null },
            title = { Text("Desglose: ${prodRes.productoNombre}") },
            text = {
                Column(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            prodRes.tramos,
                            key = { tramo -> "${tramo.fechaInicio}-${tramo.fechaFin}" }
                        ) { tramo ->
                            TramoItemView(tramo = tramo, unidad = prodRes.unidadAbreviatura)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProductoForDetail = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun ProductoConsumoRow(
    resultado: ResultadoProducto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = resultado.productoNombre,
                style = MaterialTheme.typography.titleLarge
            )

            if (!resultado.tieneDatosSuficientes) {
                Text(
                    text = "Sin datos suficientes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                val rangoStr = resultado.rangoFechasStr ?: ""
                val mediaStr = resultado.consumoDiarioMedio?.let { "≈ ${it.formatCantidadConComa()} ${resultado.unidadAbreviatura}/día" } ?: ""
                val infoSub = listOf(mediaStr, rangoStr).filter { it.isNotBlank() }.joinToString(" · ")

                if (infoSub.isNotBlank()) {
                    Text(
                        text = infoSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (resultado.tramosIncoherentesCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Aviso",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(16.dp)
                        )
                        Text(
                            text = "${resultado.tramosIncoherentesCount} tramo${if (resultado.tramosIncoherentesCount > 1) "s" else ""} revisar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (resultado.tieneDatosSuficientes) {
            Text(
                text = "${resultado.consumoTotal.formatCantidadConComa()} ${resultado.unidadAbreviatura}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TramoItemView(
    tramo: ResultadoTramo,
    unidad: String,
    modifier: Modifier = Modifier,
) {
    val inicioStr = tramo.fechaInicio.formatFechaCorta()
    val finStr = tramo.fechaFin.formatFechaCorta()
    val color = if (tramo.esIncoherente) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$inicioStr → $finStr",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (tramo.esIncoherente) {
                Text(
                    text = "REVISAR",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            text = "Hay inicial: ${tramo.cantidadInicial.formatCantidadConComa()} $unidad | Entró: +${tramo.entradas.formatCantidadConComa()} $unidad",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Hay final: ${tramo.cantidadFinal.formatCantidadConComa()} $unidad | Consumo: ${tramo.consumo.formatCantidadConComa()} $unidad",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
