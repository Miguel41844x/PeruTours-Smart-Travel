package com.example.perutours.ui.screens.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Composable
internal fun DateTimeField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RequestDateTimePicker(
    initialValue: Long,
    minimumValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val baseValue = initialValue.takeIf { it > minimumValue }
        ?: (minimumValue + DEFAULT_DATE_TIME_OFFSET_MILLIS)
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

internal fun formatDateTime(value: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(value))

internal fun tripDurationText(departure: Long, returnAt: Long): String? {
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

internal fun toDatePickerUtcMillis(value: Long): Long {
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

internal fun combineDateAndTime(selectedDateMillis: Long, hour: Int, minute: Int): Long {
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
