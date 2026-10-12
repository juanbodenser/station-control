package com.jboden.fuegos.ui.recuento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getFechaHoyStr(): String {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-ES"))
    return today.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecuento(
    viewModel: FuegosViewModel,
    onNavigateToProduccion: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToHistorico: () -> Unit,
    onNavigateToConsumo: () -> Unit,
    onNavigateToCopia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { uiState.secciones.size }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onUserMessageShown()
        }
    }

    val seccionActual = uiState.secciones.getOrNull(pagerState.currentPage)
    val camposRellenadosCount = if (seccionActual != null) {
        seccionActual.productos.count { prod ->
            val id = prod.producto.id
            !viewModel.cantidadesIngresadas[id].isNullOrBlank() || !viewModel.entradasIngresadas[id].isNullOrBlank()
        }
    } else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Recuento", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = getFechaHoyStr(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Producción") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToProduccion()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Histórico") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToHistorico()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Consumo") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToConsumo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copia de seguridad") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCopia()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajustes") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToAjustes()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (seccionActual != null && uiState.secciones.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.guardarRecuento(seccionActual)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                val buttonText = if (camposRellenadosCount > 0) {
                                    "Guardar ($camposRellenadosCount)"
                                } else {
                                    "Guardar"
                                }
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.secciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay secciones disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }
                ) {
                    uiState.secciones.forEachIndexed { index, seccionUiState ->
                        val tieneCambios = viewModel.tieneCambiosSinGuardar(seccionUiState)
                        val isSelected = pagerState.currentPage == index

                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = seccionUiState.seccion.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tieneCambios) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val seccionState = uiState.secciones[page]
                    PaginaSeccion(
                        seccionState = seccionState,
                        cantidadesIngresadas = viewModel.cantidadesIngresadas,
                        entradasIngresadas = viewModel.entradasIngresadas,
                        onCantidadChange = { prodId, cant ->
                            viewModel.onCantidadChanged(prodId, cant)
                        },
                        onEntradaChange = { prodId, entro ->
                            viewModel.onEntradaChanged(prodId, entro)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaginaSeccion(
    seccionState: SeccionUiState,
    cantidadesIngresadas: Map<Long, String>,
    entradasIngresadas: Map<Long, String>,
    onCantidadChange: (Long, String) -> Unit,
    onEntradaChange: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val productos = seccionState.productos
    val focusRequesters = remember(productos) {
        List(productos.size * 2) { FocusRequester() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PRODUCTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(76.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "HAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                Box(modifier = Modifier.width(68.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "ENTRÓ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                Box(modifier = Modifier.width(68.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "RECUENTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = productos,
                key = { _, it -> it.producto.id }
            ) { index, prodConUnidad ->
                val prodId = prodConUnidad.producto.id
                val cantidadText = cantidadesIngresadas[prodId] ?: ""
                val entradaText = entradasIngresadas[prodId] ?: ""
                val ultimoDato = seccionState.ultimasCantidades[prodId]
                val entradaPosterior = seccionState.entradasPosteriores[prodId]

                val entroIndex = index * 2
                val recuentoIndex = index * 2 + 1
                val isLastField = index == productos.size - 1

                FilaProducto(
                    nombreProducto = prodConUnidad.producto.nombre,
                    abreviaturaUnidad = prodConUnidad.abreviaturaUnidad,
                    entradaIngresada = entradaText,
                    cantidadIngresada = cantidadText,
                    ultimoDato = ultimoDato,
                    entradaPosterior = entradaPosterior,
                    onEntradaChange = { nuevaEntrada ->
                        onEntradaChange(prodId, nuevaEntrada)
                    },
                    onCantidadChange = { nuevaCant ->
                        onCantidadChange(prodId, nuevaCant)
                    },
                    imeActionEntro = ImeAction.Next,
                    imeActionRecuento = if (isLastField) ImeAction.Done else ImeAction.Next,
                    onNextEntro = {
                        try {
                            focusRequesters[recuentoIndex].requestFocus()
                        } catch (_: Exception) {}
                    },
                    onNextRecuento = {
                        if (!isLastField) {
                            try {
                                focusRequesters[entroIndex + 2].requestFocus()
                            } catch (_: Exception) {}
                        } else {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    focusRequesterEntro = focusRequesters[entroIndex],
                    focusRequesterRecuento = focusRequesters[recuentoIndex]
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        }
    }
}
