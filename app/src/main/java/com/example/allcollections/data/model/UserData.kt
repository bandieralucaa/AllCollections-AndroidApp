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
    val dateOfBirth: String = "",  // ← CAMBIA DA LocalDate? A String
    val email: String = "",
    val gender: String = "",
    val username: String = "",
    val profileImageUrl: String = ""
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

    /**
     * Nome completo dell'utente (Nome + Cognome).
     * Gestisce automaticamente spazi e campi vuoti.
     */
    val fullName: String
        get() = listOf(name, surname)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /**
     * Età dell'utente in anni.
     * Ritorna null se la data di nascita non è impostata o non valida.
     */
    val age: Int?
        get() = dateOfBirthAsLocalDate?.let {
            Period.between(it, LocalDate.now()).years
        }

    /**
     * Indica se l'utente ha un'immagine profilo impostata.
     */
    val hasProfileImage: Boolean
        get() = profileImageUrl.isNotBlank()

    /**
     * Nome da mostrare nell'interfaccia.
     * Priorità:
     * 1. username (formato @username)
     * 2. nome completo
     * 3. fallback "Utente"
     */
    val displayName: String
        get() = when {
            username.isNotBlank() -> "@$username"
            fullName.isNotBlank() -> fullName
            else -> "Utente"
        }

    /**
     * Verifica se l'utente è maggiorenne (>= 18 anni).
     * Se la data di nascita non è disponibile, ritorna false.
     */
    fun isAdult(): Boolean = age?.let { it >= 18 } ?: false

    /**
     * Formatta la data di nascita in formato italiano (es: "17/01/2024")
     */
    fun getFormattedDateOfBirth(): String {
        return dateOfBirthAsLocalDate?.let {
            it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } ?: dateOfBirth  // Se non può parsare, mostra la stringa originale
    }

    companion object {
        /**
         * Crea un'istanza vuota/placeholder di UserData.
         * Utile per stati iniziali o loading.
         */
        fun empty(): UserData = UserData()

        /**
         * Crea UserData con LocalDate (convertendolo automaticamente in String)
         */
        fun createWithLocalDate(
            userId: String,
            name: String,
            surname: String,
            dateOfBirth: LocalDate,
            email: String,
            gender: String,
            username: String,
            profileImageUrl: String = ""
        ): UserData {
            return UserData(
                userId = userId,
                name = name,
                surname = surname,
                dateOfBirth = dateOfBirth.toString(),
                email = email,
                gender = gender,
                username = username,
                profileImageUrl = profileImageUrl
            )
        }
    }
}

/**
 * Verifica se l'utente ha i dati minimi per essere considerato valido.
 * Utile prima di salvataggi o operazioni sensibili.
 */
val UserData.isValid: Boolean
    get() = userId.isNotBlank() &&
            email.isNotBlank() &&
            (username.isNotBlank() || fullName.isNotBlank())