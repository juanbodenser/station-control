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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.jboden.fuegos.data.Unidad

@Composable
fun AjustesUnidades(
    viewModel: AjustesViewModel,
    modifier: Modifier = Modifier,
) {
    val unidades by viewModel.unidades.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUnidad by remember { mutableStateOf<Unidad?>(null) }
    var showInUseDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva unidad")
                Text("Nueva unidad", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { innerPadding ->
        if (unidades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay unidades configuradas",
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
                items(unidades, key = { it.id }) { unidad ->
                    UnidadRow(
                        unidad = unidad,
                        onEdit = { editingUnidad = unidad },
                        onDelete = {
                            viewModel.deleteUnidad(
                                unidad = unidad,
                                onInUse = { showInUseDialog = true },
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
        UnidadDialog(
            title = "Nueva unidad",
            initialName = "",
            initialAbbreviation = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, abbr ->
                viewModel.addUnidad(name, abbr) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    editingUnidad?.let { unidad ->
        UnidadDialog(
            title = "Editar unidad",
            initialName = unidad.nombre,
            initialAbbreviation = unidad.abreviatura,
            onDismiss = { editingUnidad = null },
            onConfirm = { name, abbr ->
                viewModel.updateUnidad(unidad, name, abbr) { success ->
                    if (success) editingUnidad = null
                }
            }
        )
    }

    if (showInUseDialog) {
        AlertDialog(
            onDismissRequest = { showInUseDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            title = { Text("No se puede borrar", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Esta unidad está en uso por uno o más productos y no se puede borrar.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { showInUseDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun UnidadRow(
    unidad: Unidad,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
                text = unidad.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = unidad.abreviatura,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones de unidad", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun UnidadDialog(
    title: String,
    initialName: String,
    initialAbbreviation: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var abbreviation by remember { mutableStateOf(initialAbbreviation) }

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
                    label = { Text("Nombre (ej. kilo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { abbreviation = it },
                    label = { Text("Abreviatura (ej. kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, abbreviation) },
                enabled = name.isNotBlank() && abbreviation.isNotBlank()
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
