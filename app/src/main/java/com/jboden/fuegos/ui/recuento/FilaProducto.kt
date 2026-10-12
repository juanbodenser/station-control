package com.jboden.fuegos.ui.recuento

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jboden.fuegos.data.UltimoDato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.formatCantidadConComa(): String {
    val str = if ((this % 1.0) == 0.0) {
        this.toLong().toString()
    } else {
        this.toString()
    }
    return str.replace('.', ',')
}

fun Long.formatFechaCorta(): String {
    val date = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val currentYear = LocalDate.now().year
    val pattern = if (date.year == currentYear) {
        "EEE d MMM"
    } else {
        "EEE d MMM yyyy"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag("es-ES"))
    return date.format(formatter)
}

fun Long.esHoy(): Boolean {
    val date = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())
    return date == today
}

fun Long.formatHoraCortita(): String {
    val time = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return time.format(formatter)
}

@Composable
fun FilaProducto(
    nombreProducto: String,
    abreviaturaUnidad: String,
    entradaIngresada: String,
    cantidadIngresada: String,
    ultimoDato: UltimoDato?,
    entradaPosterior: Double?,
    onEntradaChange: (String) -> Unit,
    onCantidadChange: (String) -> Unit,
    imeActionEntro: ImeAction,
    imeActionRecuento: ImeAction,
    onNextEntro: () -> Unit,
    onNextRecuento: () -> Unit,
    focusRequesterEntro: FocusRequester? = null,
    focusRequesterRecuento: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val hasCount = ultimoDato != null
    val hasEntries = entradaPosterior != null && entradaPosterior > 0.0

    val hayCantidadVal = if (hasCount) {
        val base = ultimoDato.cantidad
        if (hasEntries) base + entradaPosterior else base
    } else {
        null
    }

    val hayCantidadStr = hayCantidadVal?.formatCantidadConComa() ?: "—"
    val hayColor = if (hasEntries) tertiaryColor else if (hasCount) primaryColor else onSurfaceVariantColor

    val line2Hay = if (hasCount) {
        if (hasEntries) {
            "estimado"
        } else {
            if (ultimoDato.fecha.esHoy()) {
                "hoy ${ultimoDato.fecha.formatHoraCortita()}"
            } else {
                ultimoDato.fecha.formatFechaCorta()
            }
        }
    } else {
        "sin recuento"
    }

    val subTextAnnotated = buildAnnotatedString {
        if (hasCount && hasEntries) {
            append("contado ${ultimoDato.cantidad.formatCantidadConComa()} · ")
            withStyle(SpanStyle(color = tertiaryColor)) {
                append("+${entradaPosterior.formatCantidadConComa()} entró")
            }
        } else if (!hasCount && hasEntries) {
            withStyle(SpanStyle(color = tertiaryColor)) {
                append("+${entradaPosterior.formatCantidadConComa()} entró")
            }
        } else if (!hasCount) {
            append("Sin recuento")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. Nombre del producto + subtexto (entradas posteriores o sin recuento)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = nombreProducto,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (subTextAnnotated.isNotEmpty()) {
                Text(
                    text = subTextAnnotated,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 2. Bloque HAY (ancho fijo 76.dp, alineado a la derecha)
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = hayCantidadStr,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hayColor
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = abreviaturaUnidad,
                        style = MaterialTheme.typography.labelLarge,
                        color = onSurfaceVariantColor
                    )
                }

                Text(
                    text = line2Hay,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (hasEntries) tertiaryColor else onSurfaceVariantColor
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Campo "Entró"
            NumeroTextField(
                value = entradaIngresada,
                onValueChange = onEntradaChange,
                imeAction = imeActionEntro,
                onImeAction = onNextEntro,
                focusRequester = focusRequesterEntro
            )

            // 4. Campo "Recuento"
            NumeroTextField(
                value = cantidadIngresada,
                onValueChange = onCantidadChange,
                imeAction = imeActionRecuento,
                onImeAction = onNextRecuento,
                focusRequester = focusRequesterRecuento
            )
        }
    }
}

@Composable
fun NumeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineColor = MaterialTheme.colorScheme.outline

    BasicTextField(
        value = value.replace('.', ','),
        onValueChange = { newValue ->
            onValueChange(newValue.replace(',', '.'))
        },
        modifier = modifier
            .width(68.dp)
            .height(56.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp,
            textAlign = TextAlign.End,
            color = if (value.isNotBlank()) primaryColor else onSurfaceColor
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value.replace('.', ','),
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = remember { MutableInteractionSource() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = surfaceContainerColor,
                    unfocusedContainerColor = surfaceContainerColor,
                    disabledContainerColor = surfaceContainerColor,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = outlineColor,
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            )
        }
    )
}
