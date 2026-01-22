package com.example.allcollections.core.utils.input

import android.app.DatePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Formato data principale dell'app
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Composable per selezionare una data tramite TextField + DatePickerDialog.
 *
 * Mostra una TextField leggibile e un'icona per aprire il DatePicker.
 * La TextField è di sola lettura.
 *
 * @param selectedDate Data corrente selezionata.
 * @param modifier Modifier opzionale per personalizzare il layout.
 * @param label Label del campo, default = "Data".
 * @param onDateSelected Callback chiamato quando l'utente seleziona una nuova data.
 */
@Composable
fun DatePickerField(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    label: String = "Data",
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // TextField leggibile con icona per aprire il DatePicker
    OutlinedTextField(
        value = selectedDate.format(DATE_FORMATTER),
        onValueChange = {}, // Campo di sola lettura
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Seleziona data")
            }
        },
        readOnly = true,
        modifier = modifier
    )

    // Mostra DatePicker solo se showDialog è true
    if (showDialog) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                showDialog = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }
}
