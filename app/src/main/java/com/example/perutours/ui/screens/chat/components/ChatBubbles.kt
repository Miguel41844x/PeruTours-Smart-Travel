package com.example.perutours.ui.screens.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.perutours.data.model.ChatMessage
import com.example.perutours.data.model.ChatQuotationCard
import com.example.perutours.data.model.Quotation
import com.example.perutours.domain.chat.ChatFormatters
import com.example.perutours.ui.screens.chat.ChatTextFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BubbleMaxWidth = 300.dp
private val AvatarSize = 36.dp

/** Avatar del asistente (ícono vectorial, sin imágenes externas). */
@Composable
fun AssistantAvatar(modifier: Modifier = Modifier, size: Dp = AvatarSize) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** Mensaje enviado por el usuario: alineado a la derecha, color de marca (ladrillo). */
@Composable
fun UserBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.widthIn(max = BubbleMaxWidth),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            TimeLabel(message.timestampMillis, Modifier.padding(top = 2.dp, end = 4.dp))
        }
    }
}

/**
 * Respuesta del asistente: alineada a la izquierda con avatar, color crema
 * (maíz suave). Puede traer tarjetas de cotización reales debajo del texto.
 */
@Composable
fun AssistantBubble(
    text: String,
    modifier: Modifier = Modifier,
    timestampMillis: Long? = null,
    isFallback: Boolean = false,
    quotationCards: List<ChatQuotationCard> = emptyList(),
    onOpenQuotation: (String) -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AssistantAvatar()
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.widthIn(max = BubbleMaxWidth),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (isFallback) {
                        Text(
                            text = "Respuesta sin IA · datos de tu cuenta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = rememberFormattedText(text),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            quotationCards.forEach { card ->
                QuotationChatCard(card = card, onOpen = { onOpenQuotation(card.quotationId) })
            }
            if (timestampMillis != null) {
                TimeLabel(timestampMillis, Modifier.padding(start = 4.dp))
            }
        }
    }
}

/** Mensaje de error (sin conexión, etc.) con botón para reintentar. */
@Composable
fun ErrorBubble(text: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AssistantAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            modifier = Modifier.widthIn(max = BubbleMaxWidth)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

/** Tres puntos animados mientras la IA genera la respuesta. */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AssistantAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 500, delayMillis = index * 160),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "typingDot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

/** Tarjeta de una cotización real del cliente; al tocarla abre su detalle. */
@Composable
fun QuotationChatCard(
    card: ChatQuotationCard,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatters = remember { ChatFormatters() }
    val primary = MaterialTheme.colorScheme.primary

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primary.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cotización",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            StatusPill(card.status)
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InfoRow(Icons.Default.LocationOn, "Destino", card.destination)
            InfoRow(Icons.Default.Group, "Viajeros", card.travelerCount.toString())
            if (card.validityDate.isNotBlank()) {
                InfoRow(Icons.Default.Event, "Vigencia", card.validityDate)
            }
            InfoRow(
                icon = Icons.Default.Payments,
                label = "Total",
                value = formatters.money(card.totalAmount, card.currency),
                emphasize = true
            )
            Text(
                text = "Ver detalle",
                style = MaterialTheme.typography.labelLarge,
                color = primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = quotationStatusColor(status)
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/** Colores del tema para cada estado de cotización (funcionan en claro y oscuro). */
@Composable
private fun quotationStatusColor(status: String): Color = when (status) {
    Quotation.STATUS_ACCEPTED -> MaterialTheme.colorScheme.tertiary
    Quotation.STATUS_OBSERVED -> MaterialTheme.colorScheme.secondary
    Quotation.STATUS_CANCELLED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun TimeLabel(millis: Long, modifier: Modifier = Modifier) {
    val format = remember { SimpleDateFormat("h:mm a", Locale.forLanguageTag("es-PE")) }
    Text(
        text = format.format(Date(millis)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun rememberFormattedText(raw: String): AnnotatedString = remember(raw) {
    buildAnnotatedString {
        ChatTextFormatter.format(raw).forEach { segment ->
            if (segment.bold) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment.text) }
            } else {
                append(segment.text)
            }
        }
    }
}