package com.jboden.fuegos.ui.ajustes

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jboden.fuegos.data.Producto
import com.jboden.fuegos.data.ProductoConUnidad
import com.jboden.fuegos.data.Seccion
import com.jboden.fuegos.data.Unidad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesProductos(
    viewModel: AjustesViewModel,
    modifier: Modifier = Modifier,
) {
    val secciones by viewModel.secciones.collectAsStateWithLifecycle()
    val unidades by viewModel.unidades.collectAsStateWithLifecycle()
    val selectedSeccionId by viewModel.selectedSeccionId.collectAsStateWithLifecycle()
    val productos by viewModel.productosSeccion.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProducto by remember { mutableStateOf<ProductoConUnidad?>(null) }
    var showHasHistoryDialog by remember { mutableStateOf(false) }
    var pendingDeleteProducto by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(secciones, selectedSeccionId) {
        if (selectedSeccionId == null && secciones.isNotEmpty()) {
            viewModel.setSelectedSeccionId(secciones.first().id)
        }
    }

    val selectedSeccion = secciones.find { it.id == selectedSeccionId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedSeccionId != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                    Text("Nuevo producto", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedSeccion?.nombre ?: "Seleccionar sección",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sección") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    secciones.forEach { seccion ->
                        DropdownMenuItem(
                            text = { Text(seccion.nombre) },
                            onClick = {
                                viewModel.setSelectedSeccionId(seccion.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedSeccionId == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Selecciona una sección", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (productos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos en esta sección", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(productos, key = { _, p -> p.producto.id }) { index, prodConUnidad ->
                        ProductoRow(
                            productoConUnidad = prodConUnidad,
                            canMoveUp = index > 0,
                            canMoveDown = index < productos.size - 1,
                            onMoveUp = { viewModel.moveProducto(prodConUnidad.producto.id, -1) },
                            onMoveDown = { viewModel.moveProducto(prodConUnidad.producto.id, 1) },
                            onEdit = { editingProducto = prodConUnidad },
                            onToggleActivo = { viewModel.toggleProductoActivo(prodConUnidad.producto) },
                            onDelete = {
                                pendingDeleteProducto = prodConUnidad.producto
                                viewModel.deleteProducto(
                                    producto = prodConUnidad.producto,
                                    onHasHistory = { showHasHistoryDialog = true },
                                    onSuccess = {}
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }
                }
            }
        }
    }

    if (showAddDialog && selectedSeccionId != null) {
        ProductoDialog(
            title = "Nuevo producto",
            initialName = "",
            initialUnidadId = unidades.firstOrNull()?.id ?: 0L,
            initialSeccionId = selectedSeccionId!!,
            secciones = secciones,
            unidades = unidades,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unidadId, seccionId ->
                viewModel.addProducto(seccionId, unidadId, name) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    editingProducto?.let { prod ->
        ProductoDialog(
            title = "Editar producto",
            initialName = prod.producto.nombre,
            initialUnidadId = prod.producto.unidadId,
            initialSeccionId = prod.producto.seccionId,
            secciones = secciones,
            unidades = unidades,
            onDismiss = { editingProducto = null },
            onConfirm = { name, unidadId, seccionId ->
                viewModel.updateProducto(prod.producto, name, seccionId, unidadId) { success ->
                    if (success) editingProducto = null
                }
            }
        )
    }

    if (showHasHistoryDialog && pendingDeleteProducto != null) {
        AlertDialog(
            onDismissRequest = { showHasHistoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            title = { Text("No se puede borrar", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Este producto tiene historial; solo puedes ocultarlo.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteProducto?.let { viewModel.ocultarProducto(it) }
                        showHasHistoryDialog = false
                        pendingDeleteProducto = null
                    }
                ) {
                    Text("Ocultar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showHasHistoryDialog = false
                        pendingDeleteProducto = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProductoRow(
    productoConUnidad: ProductoConUnidad,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onToggleActivo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prod = productoConUnidad.producto
    val textColor = if (prod.activo) MaterialTheme.colorScheme.onSurface else Color.Gray
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = prod.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                if (!prod.activo) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OCULTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            Text(
                text = productoConUnidad.abreviaturaUnidad,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = prod.activo,
                onCheckedChange = { onToggleActivo() }
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones de producto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoDialog(
    title: String,
    initialName: String,
    initialUnidadId: Long,
    initialSeccionId: Long,
    secciones: List<Seccion>,
    unidades: List<Unidad>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedUnidadId by remember { mutableStateOf(initialUnidadId) }
    var selectedSeccionId by remember { mutableStateOf(initialSeccionId) }

    var unidadExpanded by remember { mutableStateOf(false) }
    var seccionExpanded by remember { mutableStateOf(false) }

    val selectedUnidad = unidades.find { it.id == selectedUnidadId }
    val selectedSeccion = secciones.find { it.id == selectedSeccionId }

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
                    label = { Text("Nombre del producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = unidadExpanded,
                    onExpandedChange = { unidadExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedUnidad?.let { "${it.nombre} (${it.abreviatura})" } ?: "Seleccionar unidad",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unidadExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unidadExpanded,
                        onDismissRequest = { unidadExpanded = false }
                    ) {
                        unidades.forEach { unidad ->
                            DropdownMenuItem(
                                text = { Text("${unidad.nombre} (${unidad.abreviatura})") },
                                onClick = {
                                    selectedUnidadId = unidad.id
                                    unidadExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = seccionExpanded,
                    onExpandedChange = { seccionExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSeccion?.nombre ?: "Seleccionar sección",
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
                                    selectedSeccionId = seccion.id
                                    seccionExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedUnidadId, selectedSeccionId) },
                enabled = name.isNotBlank() && selectedUnidadId != 0L && selectedSeccionId != 0L
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
