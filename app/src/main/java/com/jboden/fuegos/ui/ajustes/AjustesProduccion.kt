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
import androidx.compose.material3.Button
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
import com.jboden.fuegos.data.TareaProduccion
import com.jboden.fuegos.ui.produccion.ProduccionViewModel

@Composable
fun AjustesProduccion(
    viewModel: ProduccionViewModel,
    modifier: Modifier = Modifier,
) {
    val tareas by viewModel.tareas.collectAsStateWithLifecycle()

    var encabezadoText by remember { mutableStateOf(viewModel.getEncabezado()) }
    var cierreText by remember { mutableStateOf(viewModel.getCierre()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTarea by remember { mutableStateOf<TareaProduccion?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva tarea")
                Text("Nueva tarea", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Plantilla del mensaje", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = encabezadoText,
                        onValueChange = { encabezadoText = it },
                        label = { Text("Encabezado del mensaje") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cierreText,
                        onValueChange = { cierreText = it },
                        label = { Text("Cierre del mensaje") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveEncabezado(encabezadoText)
                            viewModel.saveCierre(cierreText)
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Guardar plantilla")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Tareas de producción", style = MaterialTheme.typography.titleLarge)
                }
            }

            if (tareas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tareas configuradas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(tareas, key = { _, t -> t.id }) { index, tarea ->
                    TareaRow(
                        tarea = tarea,
                        canMoveUp = index > 0,
                        canMoveDown = index < tareas.size - 1,
                        onMoveUp = { viewModel.moveTarea(tarea.id, -1) },
                        onMoveDown = { viewModel.moveTarea(tarea.id, 1) },
                        onEdit = { editingTarea = tarea },
                        onDelete = { viewModel.deleteTarea(tarea) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        }
    }

    if (showAddDialog) {
        TareaDialog(
            title = "Nueva tarea",
            initialTexto = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { texto ->
                viewModel.addTarea(texto) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    editingTarea?.let { tarea ->
        TareaDialog(
            title = "Editar tarea",
            initialTexto = tarea.texto,
            onDismiss = { editingTarea = null },
            onConfirm = { texto ->
                viewModel.updateTarea(tarea, texto) { success ->
                    if (success) editingTarea = null
                }
            }
        )
    }
}

@Composable
fun TareaRow(
    tarea: TareaProduccion,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
        Text(
            text = tarea.texto,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones de tarea", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun TareaDialog(
    title: String,
    initialTexto: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var texto by remember { mutableStateOf(initialTexto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Texto de la tarea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(texto) },
                enabled = texto.isNotBlank()
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
