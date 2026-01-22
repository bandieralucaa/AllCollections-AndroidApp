package com.example.allcollections.data.model

/**
 * Tipologia di relazione "follow" tra utenti.
 * Serve per distinguere tra chi segue l'utente e chi viene seguito.
 */
enum class FollowType {
    /** Utente che segue l'account corrente */
    FOLLOWERS,

    /** Utente che l'account corrente segue */
    FOLLOWING
}
