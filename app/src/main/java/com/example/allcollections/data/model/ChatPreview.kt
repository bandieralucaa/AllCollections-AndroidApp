package com.example.allcollections.data.model

import java.util.Date

/**
 * Anteprima di una conversazione nella lista delle chat recenti.
 *
 * Usata per popolare la lista delle conversazioni senza caricare
 * tutti i messaggi, tenendo solo i dati essenziali per la preview.
 *
 * @property otherUserId ID dell'altro utente nella conversazione.
 * @property lastMessage Testo dell'ultimo messaggio inviato o ricevuto.
 * @property timestamp Data e ora dell'ultimo messaggio.
 * @property unreadCount Numero di messaggi non ancora letti dall'utente corrente.
 */
data class ChatPreview(
    val otherUserId: String,
    val lastMessage: String,
    val timestamp: Date,
    val unreadCount: Int
)
