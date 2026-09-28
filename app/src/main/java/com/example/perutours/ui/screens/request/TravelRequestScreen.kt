package com.example.perutours.ui.screens.request

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelRequestScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: TravelRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var dateTimeTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectCurrentLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
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
        val initialValue = if (target == "departure") {
            uiState.departureAtMillis
        } else {
            uiState.returnAtMillis
        }

        RequestDateTimePicker(
            initialValue = initialValue,
            onDismiss = { dateTimeTarget = null },
            onConfirm = { value ->
                if (target == "departure") {
                    viewModel.onDepartureChanged(value)
                } else {
                    viewModel.onReturnChanged(value)
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nueva solicitud de viaje",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Cuéntanos cómo quieres viajar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Usaremos estos datos para preparar una cotización personalizada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            OutlinedTextField(
                value = uiState.destination,
                onValueChange = viewModel::onDestinationChanged,
                label = { Text("Destino") },
                placeholder = { Text("Ej. Cusco") },
                isError = uiState.destinationError != null,
                supportingText = uiState.destinationError?.let { error ->
                    { Text(error) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.originCity,
                onValueChange = {},
                label = { Text("Ciudad de origen") },
                placeholder = { Text("Se detectará con tu ubicación") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    } else {
                        IconButton(onClick = ::requestLocation) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Detectar ubicación"
                            )
                        }
                    }
                },
                isError = uiState.originCityError != null,
                supportingText = uiState.originCityError?.let { error ->
                    { Text(error) }
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(18.dp))

            DateTimeField(
                label = "Salida",
                value = uiState.departureAtMillis,
                error = uiState.departureError,
                onClick = { dateTimeTarget = "departure" }
            )

            Spacer(Modifier.height(12.dp))

            DateTimeField(
                label = "Retorno",
                value = uiState.returnAtMillis,
                error = uiState.returnError,
                onClick = { dateTimeTarget = "return" }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Cantidad de viajeros",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::decrementTravelerCount) {
                    Icon(Icons.Default.Remove, contentDescription = "Reducir viajeros")
                }
                Text(
                    text = uiState.travelerCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = viewModel::incrementTravelerCount) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar viajeros")
                }
            }
            uiState.travelerCountError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Observaciones opcionales") },
                supportingText = { Text("${uiState.notes.length}/500") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.validate() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Text(
                        text = "Revisar solicitud",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
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
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp)
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
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatDateTime(value: Long): String {
    return SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(Date(value))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDateTimePicker(
    initialValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val baseValue = initialValue.takeIf { it > 0L } ?: System.currentTimeMillis()
    val initialCalendar = remember(baseValue) {
        Calendar.getInstance().apply { timeInMillis = baseValue }
    }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = baseValue
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
                        if (datePickerState.selectedDateMillis != null) {
                            selectingTime = true
                        }
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Selecciona la hora") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis ?: return@TextButton
                        onConfirm(
                            combineDateAndTime(
                                selectedDateMillis = selectedDate,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                        )
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun combineDateAndTime(
    selectedDateMillis: Long,
    hour: Int,
    minute: Int
): Long {
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
