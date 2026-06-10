package com.example.allcollections.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Testo di errore uniforme: colore rosso (tema) e stile opzionale.
 * Usato per mostrare messaggi di validazione, errori di login, ecc.
 */
@Composable
fun ErrorText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = style,
        modifier = modifier
    )
}