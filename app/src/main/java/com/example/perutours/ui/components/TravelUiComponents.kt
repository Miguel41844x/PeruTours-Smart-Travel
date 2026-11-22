package com.example.perutours.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/** Encabezado común para los espacios del cliente y del equipo de la agencia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Column(modifier) {
        TopAppBar(
            title = { ProvideTextStyle(MaterialTheme.typography.titleLarge, title) },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    }
}

@Composable
fun TravelPageIntro(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        eyebrow?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(description, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Las imágenes fallidas mantienen la estructura y una alternativa legible. */
@Composable
fun TravelPhoto(url: String, description: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url.takeIf(String::isNotBlank),
        contentDescription = description,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentScale = ContentScale.Crop,
        loading = { PhotoPlaceholder() },
        error = { PhotoPlaceholder() }
    )
}

@Composable
private fun PhotoPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Landscape, contentDescription = "Imagen no disponible",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun TravelEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(20.dp).size(30.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
