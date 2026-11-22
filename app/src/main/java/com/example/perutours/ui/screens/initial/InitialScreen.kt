package com.example.perutours.ui.screens.initial

import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.perutours.R
import com.example.perutours.ui.components.PeruToursBrand
import kotlinx.coroutines.delay

private const val CAROUSEL_AUTO_SCROLL_MILLIS = 4_500L

private data class WelcomeDestination(
    val imageRes: Int,
    val name: String,
    val region: String,
    val contentDescription: String,
    val alignment: Alignment = Alignment.Center
)

private val welcomeDestinations = listOf(
    WelcomeDestination(
        imageRes = R.drawable.welcome_machu_picchu,
        name = "Machu Picchu",
        region = "Cusco",
        contentDescription = "Machu Picchu entre las montañas de Cusco",
        alignment = Alignment.CenterEnd
    ),
    WelcomeDestination(
        imageRes = R.drawable.welcome_vinicunca,
        name = "Montaña de Siete Colores",
        region = "Cusco",
        contentDescription = "Montaña de Siete Colores en Cusco"
    ),
    WelcomeDestination(
        imageRes = R.drawable.welcome_titicaca,
        name = "Lago Titicaca",
        region = "Puno",
        contentDescription = "Lago Titicaca y sus totorales en Puno"
    ),
    WelcomeDestination(
        imageRes = R.drawable.welcome_colca,
        name = "Cañón del Colca",
        region = "Arequipa",
        contentDescription = "Cóndor sobrevolando el cañón del Colca en Arequipa"
    )
)

@Composable
fun InitialScreen(
    navigateToLogin: () -> Unit,
    navigateToSignUp: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { welcomeDestinations.size })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val touchExploration = LocalContext.current
        .getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        ?.isTouchExplorationEnabled == true

    LaunchedEffect(pagerState, lifecycle, touchExploration) {
        if (!touchExploration) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(CAROUSEL_AUTO_SCROLL_MILLIS)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.settledPage + 1) % welcomeDestinations.size)
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter) {
            val photoHeight = (maxHeight * 0.47f).coerceIn(220.dp, 440.dp)
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    PeruToursBrand(compact = true)
                    TextButton(onClick = navigateToLogin) { Text("Iniciar sesión") }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(photoHeight)
                    .clip(MaterialTheme.shapes.extraLarge)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val destination = welcomeDestinations[page]
                        Image(painterResource(destination.imageRes), destination.contentDescription,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                            alignment = destination.alignment)
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0.35f to Color.Transparent, 1f to Color(0xDD10251B))))
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(welcomeDestinations[pagerState.settledPage].region.uppercase(),
                            style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text(welcomeDestinations[pagerState.settledPage].name,
                            style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            welcomeDestinations.indices.forEach { index ->
                                Box(Modifier.width(if (index == pagerState.settledPage) 28.dp else 8.dp)
                                    .height(4.dp).clip(MaterialTheme.shapes.small)
                                    .background(Color.White.copy(alpha = if (index == pagerState.settledPage) 1f else 0.45f)))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("El Perú que\nquieres vivir.", style = MaterialTheme.typography.displaySmall)
                Text("Elige un destino, arma tu ruta y recibe una propuesta de tu agente.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 22.dp))
                Button(onClick = navigateToSignUp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Empezar mi viaje")
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
                OutlinedButton(onClick = navigateToLogin,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 52.dp)) {
                    Text("Ya tengo una cuenta")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
