package com.example.perutours.ui.screens.request

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.components.ScreenHorizontalPadding
import com.example.perutours.ui.components.SectionHeading
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelRequestScreen(
    suggestedDestination: String = "",
    referencePackageTitle: String = "",
    referencePackageDuration: String = "",
    referencePackageDescription: String = "",
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: TravelRequestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var dateTimeTarget by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(
        suggestedDestination,
        referencePackageTitle,
        referencePackageDuration,
        referencePackageDescription
    ) {
        viewModel.prefillFromPackage(
            destination = suggestedDestination,
            packageTitle = referencePackageTitle,
            duration = referencePackageDuration,
            description = referencePackageDescription
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.detectCurrentLocation() else viewModel.onLocationPermissionDenied()
    }

    fun requestLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.detectCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    dateTimeTarget?.let { target ->
        val nowMillis = System.currentTimeMillis()
        val initialValue = if (target == "departure") uiState.departureAtMillis else uiState.returnAtMillis
        val minimumValue = if (target == "departure") {
            nowMillis
        } else {
            maxOf(nowMillis, uiState.departureAtMillis)
        }

        RequestDateTimePicker(
            initialValue = initialValue,
            minimumValue = minimumValue,
            onDismiss = { dateTimeTarget = null },
            onConfirm = { value ->
                if (target == "departure") viewModel.onDepartureChanged(value)
                else viewModel.onReturnChanged(value)
                dateTimeTarget = null
            }
        )
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (uiState.savedRequestId != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Solicitud enviada") },
            text = { Text("Un agente revisará los datos y preparará una cotización. Te avisaremos cuando esté lista.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSavedRequest()
                        onSaved()
                    }
                ) { Text("Volver al inicio") }
            }
        )
    }

    Scaffold(
        topBar = { PeruToursTopBar(title = "Planear viaje", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = viewModel::submitRequest,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .height(52.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("Verificando lugares...", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Text("Enviar solicitud", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = ScreenHorizontalPadding, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Cuéntanos cómo quieres viajar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Con estos datos prepararemos una propuesta personalizada.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))
                SectionHeading(
                    title = "Ruta",
                    supportingText = "Verificaremos que ambos lugares existan antes de enviar."
                )

                OutlinedTextField(
                    value = uiState.originCity,
                    onValueChange = viewModel::onOriginCityChanged,
                    label = { Text("Ciudad de origen") },
                    placeholder = { Text("Ej. Lima, Perú") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else {
                            IconButton(onClick = ::requestLocation) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Usar mi ubicación")
                            }
                        }
                    },
                    isError = uiState.originCityError != null,
                    supportingText = {
                        when {
                            uiState.originCityError != null -> Text(uiState.originCityError.orEmpty())
                            uiState.originLatitude != null -> Text("Ubicación detectada. Puedes editarla.")
                            else -> Text("Escribe una ciudad o usa tu ubicación actual.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = viewModel::onDestinationChanged,
                    label = { Text("Destino") },
                    placeholder = { Text("Ej. Cusco, Perú") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    isError = uiState.destinationError != null,
                    supportingText = uiState.destinationError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                SectionHeading(
                    title = "Fechas",
                    supportingText = "Solo puedes elegir fechas futuras."
                )

                DateTimeField(
                    label = "Salida",
                    value = uiState.departureAtMillis,
                    error = uiState.departureError,
                    onClick = { dateTimeTarget = "departure" }
                )
                DateTimeField(
                    label = "Retorno",
                    value = uiState.returnAtMillis,
                    error = uiState.returnError,
                    onClick = { dateTimeTarget = "return" }
                )

                tripDurationText(
                    departure = uiState.departureAtMillis,
                    returnAt = uiState.returnAtMillis
                )?.let { duration ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Duración estimada: $duration",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionHeading(
                    title = "Viajeros y detalles",
                    supportingText = "Puedes añadir información útil para preparar la propuesta."
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Viajeros", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Máximo 20 personas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = viewModel::decrementTravelerCount,
                                enabled = uiState.travelerCount > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar viajero")
                            }
                            Text(
                                text = uiState.travelerCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            IconButton(
                                onClick = viewModel::incrementTravelerCount,
                                enabled = uiState.travelerCount < 20
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar viajero")
                            }
                        }
                    }
                }
                uiState.travelerCountError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChanged,
                    label = { Text("Notas para el agente (opcional)") },
                    placeholder = { Text("Preferencias, restricciones o actividades de interés") },
                    supportingText = { Text("${uiState.notes.length}/500") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }
    }
}

@Composable
private fun DateTimeField(
    label: String,
    value: Long,
    error: String?,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Text(
                text = if (value == 0L) "Seleccionar fecha y hora" else formatDateTime(value),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatDateTime(value: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(value))

private fun tripDurationText(departure: Long, returnAt: Long): String? {
    if (departure <= 0L || returnAt <= departure) return null
    val difference = returnAt - departure
    val days = TimeUnit.MILLISECONDS.toDays(difference)
    val hours = TimeUnit.MILLISECONDS.toHours(difference) % 24
    return when {
        days > 0 && hours > 0 -> "$days días y $hours horas"
        days > 0 -> "$days días"
        else -> "$hours horas"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDateTimePicker(
    initialValue: Long,
    minimumValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val baseValue = initialValue.takeIf { it > minimumValue }
        ?: minimumValue + DEFAULT_DATE_TIME_OFFSET_MILLIS
    val initialCalendar = remember(baseValue) {
        Calendar.getInstance().apply { timeInMillis = baseValue }
    }
    val minimumDateMillis = remember(minimumValue) { toDatePickerUtcMillis(minimumValue) }
    val selectableDates = remember(minimumDateMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= minimumDateMillis
        }
    }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = toDatePickerUtcMillis(baseValue),
        selectableDates = selectableDates
    )
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )
    var selectingTime by remember { mutableStateOf(false) }

    if (!selectingTime) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (datePickerState.selectedDateMillis != null) selectingTime = true
                    }
                ) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        val selectedDateTime = datePickerState.selectedDateMillis?.let { selectedDate ->
            combineDateAndTime(selectedDate, timePickerState.hour, timePickerState.minute)
        }
        val isValidTime = selectedDateTime != null && selectedDateTime > minimumValue

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Selecciona la hora") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                    if (!isValidTime) {
                        Text(
                            text = "Selecciona una hora posterior a la fecha mínima.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = isValidTime,
                    onClick = { selectedDateTime?.let(onConfirm) }
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}

private fun toDatePickerUtcMillis(value: Long): Long {
    val localDate = Calendar.getInstance().apply { timeInMillis = value }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localDate.get(Calendar.YEAR),
            localDate.get(Calendar.MONTH),
            localDate.get(Calendar.DAY_OF_MONTH)
        )
    }.timeInMillis
}

private fun combineDateAndTime(selectedDateMillis: Long, hour: Int, minute: Int): Long {
    val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDateMillis
    }
    return Calendar.getInstance().apply {
        clear()
        set(
            utcDate.get(Calendar.YEAR),
            utcDate.get(Calendar.MONTH),
            utcDate.get(Calendar.DAY_OF_MONTH),
            hour,
            minute
        )
    }.timeInMillis
}

private const val DEFAULT_DATE_TIME_OFFSET_MILLIS = 60 * 60 * 1000L
