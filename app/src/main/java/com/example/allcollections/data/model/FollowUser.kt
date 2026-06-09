package com.example.allcollections.data.model

/**
 * Rappresenta un utente in una lista di follower o following.
 *
 * Contiene solo i dati necessari per la visualizzazione nelle liste,
 * evitando di caricare il profilo completo ([UserData]).
 *
 * @property userId ID univoco dell'utente.
 * @property username Nome utente visualizzato.
 * @property profileImageUrl URL della foto profilo.
 */
data class FollowUser(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = ""
)
