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

/**
 * Formatter per visualizzare la data nel formato italiano `dd-MM-yyyy`.
 * Usato per mostrare la data nel campo di testo.
 */
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Campo di input per la selezione di una data tramite [DatePickerDialog] nativo Android.
 *
 * Questo componente fornisce un [OutlinedTextField] di sola lettura che mostra
 * la data attuale formattata. Un'icona calendario sulla destra apre un dialog
 * nativo per la selezione della data.
 *
 * ### Comportamento
 * - Il campo è **read-only** (l'utente non può scrivere manualmente, deve usare il dialog).
 * - Il formato di visualizzazione è `dd-MM-yyyy` (giorno-mese-anno, es. "17-01-2000").
 * - Il dialog restituisce i valori anno, mese (0‑based), giorno; il componente converte
 *   automaticamente il mese in 1‑based per [LocalDate].
 * - Supporta la segnalazione visiva di errori (es. data di nascita non valida o utente minorenne)
 *   tramite il parametro [isError].
 *
 * ### Esempio di utilizzo
 * ```
 * var birthDate by remember { mutableStateOf(LocalDate.now().minusYears(18)) }
 * DatePickerField(
 *     selectedDate = birthDate,
 *     label = "Data di nascita",
 *     isError = birthDate.isAfter(LocalDate.now().minusYears(13)),
 *     onDateSelected = { birthDate = it }
 * )
 * ```
 *
 * @param selectedDate Data attualmente selezionata (mostrata nel campo).
 * @param modifier Modificatore per personalizzare dimensioni, padding, ecc.
 * @param label Testo della label del campo (default "Data di nascita").
 * @param isError Se `true`, evidenzia il campo con il colore di errore del tema.
 * @param onDateSelected Callback invocato quando l'utente seleziona una nuova data
 *                       nel dialog; riceve la [LocalDate] scelta.
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

    // Campo di testo di sola lettura con la data formattata
    OutlinedTextField(
        value = selectedDate.format(DATE_FORMATTER),
        onValueChange = {}, // Read-only, nessuna modifica diretta
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

    // Mostra il DatePickerDialog quando richiesto
    if (showDialog) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // ATTENZIONE: nel DatePickerDialog i mesi sono 0‑based (0 = Gennaio)
                // mentre LocalDate usa 1‑based (1 = Gennaio)
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(selected)
                showDialog = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1, // Converte 1‑based → 0‑based per il dialog
            selectedDate.dayOfMonth
        ).show()
    }
}