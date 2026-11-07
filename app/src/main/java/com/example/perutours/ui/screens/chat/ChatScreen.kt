package com.example.perutours.ui.screens.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.ChatRole
import com.example.perutours.ui.screens.chat.components.AssistantBubble
import com.example.perutours.ui.screens.chat.components.ChatInputBar
import com.example.perutours.ui.screens.chat.components.ErrorBubble
import com.example.perutours.ui.screens.chat.components.QuickQuestionChips
import com.example.perutours.ui.screens.chat.components.TypingIndicator
import com.example.perutours.ui.screens.chat.components.UserBubble
import com.example.perutours.ui.theme.PeruBrick
import com.example.perutours.ui.theme.PeruBrickDark
import com.example.perutours.ui.theme.PeruValleyContainer
import org.koin.androidx.compose.koinViewModel

/**
 * HU11: pantalla del asistente virtual (Material Design 3 + Jetpack Compose).
 *
 * Encabezado de marca, lista de mensajes con desplazamiento, burbujas
 * diferenciadas por emisor, indicador de escritura, sugerencias de preguntas
 * frecuentes y campo de texto con botón de envío.
 */
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onOpenQuotation: (String) -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LightStatusBarIconsOffWhileVisible()

    // Bienvenida (1) + sugerencias (0/1) + mensajes + indicador de escritura (0/1).
    val itemCount = 1 +
            (if (state.showSuggestions) 1 else 0) +
            state.messages.size +
            (if (state.isLoading) 1 else 0)

    // Cada mensaje nuevo (o el indicador de escritura) baja la lista al final.
    LaunchedEffect(state.messages.size, state.isLoading) {
        listState.animateScrollToItem(itemCount - 1)
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                onBack = onNavigateBack,
                onNewConversation = viewModel::clearConversation
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Spacer(Modifier.size(8.dp))
                    QuickQuestionChips(
                        suggestions = ChatSuggestions.quickChips,
                        enabled = !state.isLoading,
                        onSelect = viewModel::sendMessage
                    )
                    ChatInputBar(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        onSend = viewModel::sendInput,
                        canSend = state.canSend
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "welcome") {
                AssistantBubble(text = welcomeText(state.userName))
            }

            if (state.showSuggestions) {
                item(key = "faq") {
                    FrequentQuestionsCard(
                        enabled = !state.isLoading,
                        onSelect = viewModel::sendMessage
                    )
                }
            }

            items(state.messages, key = { it.id }) { message ->
                when {
                    message.role == ChatRole.USER -> UserBubble(message)
                    message.isError -> ErrorBubble(
                        text = message.text,
                        onRetry = viewModel::retry
                    )
                    else -> AssistantBubble(
                        text = message.text,
                        timestampMillis = message.timestampMillis,
                        isFallback = message.isFallback,
                        quotationCards = message.quotationCards,
                        onOpenQuotation = onOpenQuotation
                    )
                }
            }

            if (state.isLoading) {
                item(key = "typing") { TypingIndicator() }
            }
        }
    }
}

private fun welcomeText(userName: String): String {
    val greeting = if (userName.isBlank()) "¡Hola!" else "¡Hola, $userName!"
    return "$greeting 👋\nSoy tu asistente virtual de PeruTours. Puedo ayudarte con tus " +
            "solicitudes, cotizaciones y reservas, y con dudas sobre destinos del Perú. " +
            "¿En qué puedo ayudarte hoy?"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    onBack: () -> Unit,
    onNewConversation: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.background(
            Brush.horizontalGradient(listOf(PeruBrickDark, PeruBrick))
        )
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PeruBrick,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Asistente PeruTours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PeruValleyContainer)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Asistente virtual con IA",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nueva conversación") },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onNewConversation()
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

/** Tarjeta "Preguntas frecuentes" del mensaje de bienvenida. */
@Composable
private fun FrequentQuestionsCard(
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Preguntas frecuentes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ChatSuggestions.frequentQuestions.forEachIndexed { index, suggestion ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onSelect(suggestion.prompt) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = suggestion.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = suggestion.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * El encabezado es ladrillo oscuro: mientras la pantalla esté visible, los
 * íconos de la barra de estado van en blanco (y se restauran al salir).
 */
@Composable
private fun LightStatusBarIconsOffWhileVisible() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}