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

/** Formatter per visualizzare la data nel formato italiano `dd-MM-yyyy`. */
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Campo di input per la selezione di una data tramite [DatePickerDialog].
 *
 * Mostra una [OutlinedTextField] di sola lettura con la data corrente formattata
 * e un'icona calendario che apre il dialog di selezione. Supporta la segnalazione
 * visiva degli errori tramite il parametro [isError] (es. data non valida o utente minorenne).
 *
 * @param selectedDate Data attualmente selezionata, mostrata nel campo.
 * @param modifier [Modifier] opzionale per personalizzare dimensioni e posizionamento.
 * @param label Testo della label del campo; default `"Data di nascita"`.
 * @param isError Se `true`, evidenzia il campo con il colore di errore del tema.
 * @param onDateSelected Callback invocato con la nuova [LocalDate] scelta dall'utente.
 */
@Composable
fun DatePickerField(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    label: String = "Data di nascita",
    isError: Boolean = false,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate.format(DATE_FORMATTER),
        onValueChange = {},
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Seleziona data")
            }
        },
        readOnly = true,
        modifier = modifier,
        isError = isError
    )

    if (showDialog) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // month è 0-based nel DatePickerDialog, LocalDate usa 1-based
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                showDialog = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }
}
