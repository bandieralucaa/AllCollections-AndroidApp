package com.example.allcollections.data.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Modello dati per rappresentare un utente dell'app.
 *
 * Contiene tutte le informazioni principali usate per:
 * - autenticazione (email)
 * - profilo pubblico (username, bio, immagine)
 * - logica UI (età, nome visualizzato)
 *
 * **Nota:** Firestore non supporta [LocalDate] direttamente, quindi [dateOfBirth]
 * è salvato come stringa in formato ISO-8601 (es. `"2000-01-17"`).
 *
 * @property userId ID univoco dell'utente (corrisponde all'UID di Firebase Auth).
 * @property name Nome dell'utente.
 * @property surname Cognome dell'utente.
 * @property dateOfBirth Data di nascita in formato ISO-8601 (`"yyyy-MM-dd"`).
 * @property email Indirizzo email dell'utente.
 * @property gender Genere dell'utente (es. `"M"`, `"F"`, `"Altro"`).
 * @property username Nome utente pubblico visualizzato nell'app.
 * @property profileImageUrl URL della foto profilo su Cloudinary.
 * @property bio Breve biografia pubblica dell'utente.
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
     * Converte [dateOfBirth] in [LocalDate] per calcoli sull'età o confronti tra date.
     *
     * @return Il [LocalDate] corrispondente, oppure `null` se il formato non è valido o il campo è vuoto.
     */
    val dateOfBirthAsLocalDate: LocalDate?
        get() = try {
            LocalDate.parse(dateOfBirth)
        } catch (e: DateTimeParseException) {
            null
        }
}
