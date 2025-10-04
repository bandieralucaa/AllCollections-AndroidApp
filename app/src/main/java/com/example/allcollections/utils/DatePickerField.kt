package com.example.allcollections.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DatePickerField(selectedDate: LocalDate, modifier: Modifier = Modifier, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
        onValueChange = {},
        label = { Text("Data di nascita") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Seleziona data")
            }
        },
        readOnly = true,
        modifier = modifier
    )

    if (showDialog) {
        val picker = android.app.DatePickerDialog(context)
        picker.setOnDateSetListener { _, year, month, day ->
            onDateSelected(LocalDate.of(year, month + 1, day))
            showDialog = false
        }
        picker.show()
    }
}