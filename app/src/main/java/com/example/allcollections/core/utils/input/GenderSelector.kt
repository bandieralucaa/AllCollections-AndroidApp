package com.example.allcollections.core.utils.input

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Campo di selezione del genere tramite dropdown menu.
 *
 * Utilizza un [ExposedDropdownMenuBox] che mostra un [TextField] di sola lettura
 * con il valore selezionato. Al tap, si apre un menu a tendina con le opzioni
 * configurabili tramite [options]. Se nessun genere è ancora selezionato,
 * viene mostrato un placeholder testuale.
 *
 * ### Opzioni predefinite
 * - Maschio
 * - Femmina
 * - Altro
 * - Non binario
 * - Preferisco non dichiarare
 *
 * @param selectedGender Genere attualmente selezionato (stringa vuota se non ancora scelto).
 * @param modifier Modificatore opzionale per personalizzare dimensioni e posizionamento.
 * @param label Testo della label del campo (default "Genere").
 * @param options Lista di opzioni selezionabili (default le opzioni standard in italiano).
 * @param onGenderSelected Callback invocato quando l'utente seleziona un genere; riceve la stringa scelta.
 *
 * @see ExposedDropdownMenuBox
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderSelector(
    selectedGender: String,
    modifier: Modifier = Modifier,
    label: String = "Genere",
    options: List<String> = listOf(
        "Maschio",
        "Femmina",
        "Altro",
        "Non binario",
        "Preferisco non dichiarare"
    ),
    onGenderSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // Campo di testo di sola lettura che mostra il genere selezionato
        TextField(
            value = selectedGender,
            onValueChange = {}, // Read-only
            readOnly = true,
            modifier = modifier.menuAnchor(),
            label = { Text(label) },
            placeholder = { if (selectedGender.isBlank()) Text("Seleziona genere…") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        // Menu a tendina con le opzioni
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onGenderSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}