package com.example.allcollections.core.utils.input

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Composable per selezionare il genere tramite dropdown menu.
 *
 * Mostra una TextField leggibile e un menu a tendina con le opzioni disponibili.
 *
 * @param selectedGender Genere selezionato attualmente.
 * @param modifier Modifier opzionale per personalizzare layout.
 * @param label Label del campo, default = "Genere".
 * @param options Lista di opzioni disponibili. Default: comuni in italiano.
 * @param onGenderSelected Callback chiamato quando l'utente seleziona un genere.
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
    // Stato per aprire/chiudere il menu
    var expanded by remember { mutableStateOf(false) }

    // Box con TextField + menu a tendina
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedGender,
            onValueChange = {}, // Campo di sola lettura
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
