package com.example.perutours.ui.screens.initial

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

    LaunchedEffect(pagerState) {
        while (true) {
            delay(CAROUSEL_AUTO_SCROLL_MILLIS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = (pagerState.settledPage + 1) % welcomeDestinations.size
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
            ) {
                PeruToursBrand(
                    modifier = Modifier.fillMaxWidth(),
                    compact = true
                )

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 230.dp, max = 330.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val destination = welcomeDestinations[page]
                        Image(
                            painter = painterResource(destination.imageRes),
                            contentDescription = destination.contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = destination.alignment
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.35f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = welcomeDestinations[pagerState.settledPage].name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = welcomeDestinations[pagerState.settledPage].region,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.86f)
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            welcomeDestinations.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .width(
                                            if (index == pagerState.settledPage) 22.dp else 6.dp
                                        )
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (index == pagerState.settledPage) {
                                                Color.White
                                            } else {
                                                Color.White.copy(alpha = 0.48f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Viaja por el Perú, a tu manera",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Cuéntanos qué tienes en mente y recibe una propuesta preparada para ti.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(22.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = navigateToSignUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Crear cuenta", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                        )
                    }

                    OutlinedButton(
                        onClick = navigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Ya tengo una cuenta")
                    }
                }
            }
        }
    }
}
