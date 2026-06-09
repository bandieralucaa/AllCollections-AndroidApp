package com.example.allcollections.core.utils.input

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Campo di selezione del genere tramite dropdown menu.
 *
 * Mostra un [TextField] di sola lettura con il valore selezionato e un menu a tendina
 * con le opzioni disponibili. Se nessun genere è ancora selezionato, mostra un placeholder.
 *
 * @param selectedGender Genere attualmente selezionato (stringa vuota se non ancora scelto).
 * @param modifier [Modifier] opzionale per personalizzare dimensioni e posizionamento.
 * @param label Testo della label del campo; default `"Genere"`.
 * @param options Lista di opzioni selezionabili; default: le opzioni standard in italiano.
 * @param onGenderSelected Callback invocato con il genere scelto dall'utente.
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
        TextField(
            value = selectedGender,
            onValueChange = {},
            readOnly = true,
            modifier = modifier.menuAnchor(),
            label = { Text(label) },
            placeholder = { if (selectedGender.isBlank()) Text("Seleziona genere…") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

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
