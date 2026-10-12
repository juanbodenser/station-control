package com.jboden.fuegos.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jboden.fuegos.data.Origen
import com.jboden.fuegos.data.Seccion

@Composable
fun AjustesSecciones(
    viewModel: AjustesViewModel,
    modifier: Modifier = Modifier,
) {
    val secciones by viewModel.secciones.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSeccion by remember { mutableStateOf<Seccion?>(null) }
    var showHasCountsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva sección")
                Text("Nueva sección", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { innerPadding ->
        if (secciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay secciones configuradas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(secciones, key = { _, s -> s.id }) { index, seccion ->
                    SeccionRow(
                        seccion = seccion,
                        canMoveUp = index > 0,
                        canMoveDown = index < secciones.size - 1,
                        onMoveUp = { viewModel.moveSeccion(seccion.id, -1) },
                        onMoveDown = { viewModel.moveSeccion(seccion.id, 1) },
                        onEdit = { editingSeccion = seccion },
                        onDelete = {
                            viewModel.deleteSeccion(
                                seccion = seccion,
                                onHasCounts = { showHasCountsDialog = true },
                                onSuccess = {}
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        }
    }

    if (showAddDialog) {
        SeccionDialog(
            title = "Nueva sección",
            initialName = "",
            initialOrigen = Origen.PROVEEDOR,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, origen ->
                viewModel.addSeccion(name, origen) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    editingSeccion?.let { seccion ->
        SeccionDialog(
            title = "Editar sección",
            initialName = seccion.nombre,
            initialOrigen = seccion.origen,
            onDismiss = { editingSeccion = null },
            onConfirm = { name, origen ->
                viewModel.updateSeccion(seccion, name, origen) { success ->
                    if (success) editingSeccion = null
                }
            }
        )
    }

    if (showHasCountsDialog) {
        AlertDialog(
            onDismissRequest = { showHasCountsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            title = { Text("No se puede borrar", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Esta sección tiene recuentos guardados y no se puede borrar.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { showHasCountsDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun SeccionRow(
    seccion: Seccion,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    fun Origen.toDisplayString(): String = when (this) {
        Origen.PROVEEDOR -> "Proveedor"
        Origen.TALLER -> "Taller"
        Origen.COCINA -> "Cocina"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = seccion.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = seccion.origen.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones de sección", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                if (canMoveUp) {
                    DropdownMenuItem(
                        text = { Text("Subir") },
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        }
                    )
                }
                if (canMoveDown) {
                    DropdownMenuItem(
                        text = { Text("Bajar") },
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Borrar", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeccionDialog(
    title: String,
    initialName: String,
    initialOrigen: Origen,
    onDismiss: () -> Unit,
    onConfirm: (String, Origen) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedOrigen by remember { mutableStateOf(initialOrigen) }
    var origenExpanded by remember { mutableStateOf(false) }

    val origenes = listOf(Origen.PROVEEDOR, Origen.TALLER, Origen.COCINA)
    fun Origen.toDisplayString(): String = when (this) {
        Origen.PROVEEDOR -> "Proveedor"
        Origen.TALLER -> "Taller"
        Origen.COCINA -> "Cocina"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la sección") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = origenExpanded,
                    onExpandedChange = { origenExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedOrigen.toDisplayString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = origenExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = origenExpanded,
                        onDismissRequest = { origenExpanded = false }
                    ) {
                        origenes.forEach { origen ->
                            DropdownMenuItem(
                                text = { Text(origen.toDisplayString()) },
                                onClick = {
                                    selectedOrigen = origen
                                    origenExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedOrigen) },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
