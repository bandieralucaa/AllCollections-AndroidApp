package com.example.allcollections.data.model

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Modello dati per rappresentare un utente dell'app.
 *
 * Contiene tutte le informazioni principali usate:
 * - autenticazione
 * - profilo pubblico
 * - logica UI (età, nome visualizzato, avatar)
 *
 * NOTA: Firestore non supporta LocalDate direttamente,
 * quindi dateOfBirth è salvato come String (formato ISO: "2024-01-17")
 */
data class UserData(
    val userId: String = "",
    val name: String = "",
    val surname: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val gender: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val bio: String = ""
) {

    /**
     * Utility: converte la stringa in LocalDate (per calcoli)
     */
    val dateOfBirthAsLocalDate: LocalDate?
        get() = try {
            LocalDate.parse(dateOfBirth)
        } catch (e: DateTimeParseException) {
            null
        }


}
