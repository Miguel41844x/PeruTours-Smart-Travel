package com.example.perutours.ui.screens.trip

import android.speech.tts.TextToSpeech
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.perutours.data.model.TripItineraryItem
import java.util.Locale

@Composable
internal fun ReadItineraryButton(items: List<TripItineraryItem>) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { result ->
            ready = result == TextToSpeech.SUCCESS
            unavailable = !ready
        }
        speaker = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    OutlinedButton(
        enabled = ready && items.isNotEmpty(),
        onClick = {
            val engine = speaker ?: return@OutlinedButton
            if (engine.isSpeaking) {
                engine.stop()
            } else {
                val language = engine.setLanguage(Locale.forLanguageTag("es-PE"))
                unavailable = language < TextToSpeech.LANG_AVAILABLE
                if (!unavailable) {
                    val text = items.filter { it.status != TripItineraryItem.STATUS_CANCELLED }
                        .sortedWith(compareBy({ it.dayIndex }, { it.sortOrder }))
                        .joinToString(". ") { "Día ${it.dayIndex}. ${it.title}. ${it.locationName}" }
                        .take(TextToSpeech.getMaxSpeechInputLength())
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "trip-itinerary")
                }
            }
        }
    ) { Text("Escuchar o detener el itinerario") }
    if (unavailable) {
        Text("La lectura en español requiere una voz instalada en este dispositivo.")
    }
}
