package com.example.allcollections.data.model

import com.google.firebase.firestore.PropertyName
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Modello dati per rappresentare un utente dell'applicazione.
 *
 * Utilizzato per:
 * - autenticazione ([email])
 * - profilo pubblico ([username], [bio], [profileImageUrl])
 * - logica UI (età calcolata da [dateOfBirth])
 *
 * **Nota:** Firestore non supporta nativamente [LocalDate]; per questo [dateOfBirth]
 * è salvato come stringa in formato ISO-8601 (`"yyyy-MM-dd"`). Usa la proprietà
 * [dateOfBirthAsLocalDate] per ottenere un oggetto [LocalDate] o `null`.
 *
 * @property userId ID univoco dell'utente, corrisponde all'UID di Firebase Auth.
 * @property name Nome (non pubblico, usato internamente).
 * @property surname Cognome (non pubblico).
 * @property dateOfBirth Data di nascita in formato ISO-8601, es. `"2000-01-17"`.
 * @property email Indirizzo email (usato per login e notifiche).
 * @property gender Genere, valori consigliati `"M"`, `"F"`, `"Altro"`.
 * @property username Nome utente pubblico visualizzato nell'app.
 * @property profileImageUrl URL della foto profilo su Cloudinary, vuoto se non impostata.
 * @property bio Breve biografia pubblica (max 160 caratteri circa).
 *
 * @see dateOfBirthAsLocalDate per la conversione a [LocalDate]
 */
data class UserData(
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("surname")
    @set:PropertyName("surname")
    var surname: String = "",

    @get:PropertyName("dateOfBirth")
    @set:PropertyName("dateOfBirth")
    var dateOfBirth: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("gender")
    @set:PropertyName("gender")
    var gender: String = "",

    @get:PropertyName("username")
    @set:PropertyName("username")
    var username: String = "",

    @get:PropertyName("profileImageUrl")
    @set:PropertyName("profileImageUrl")
    var profileImageUrl: String = "",

    @get:PropertyName("bio")
    @set:PropertyName("bio")
    var bio: String = ""
) {
    /**
     * Rappresentazione calendariale della data di nascita.
     *
     * Converte la stringa [dateOfBirth] in [LocalDate] per calcoli sull'età
     * o confronti tra date. Se [dateOfBirth] è vuota o malformata, ritorna `null`.
     *
     * **Attenzione:** questa proprietà non è salvata in Firestore,
     * viene calcolata al volo.
     *
     * @return [LocalDate] valida, o `null` se [dateOfBirth] non è una data ISO valida.
     */
    val dateOfBirthAsLocalDate: LocalDate?
        get() {
            if (dateOfBirth.isBlank()) return null
            return try {
                LocalDate.parse(dateOfBirth)
            } catch (e: DateTimeParseException) {
                null
            }
        }
}