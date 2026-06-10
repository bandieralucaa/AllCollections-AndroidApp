package com.example.allcollections.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Documento di relazione "follow" tra due utenti.
 *
 * Corrisponde al documento Firestore nella collezione "follows".
 * L'ID del documento è solitamente `"${followerId}_${followedId}"`.
 *
 * @property followerId ID dell'utente che segue.
 * @property followedId ID dell'utente seguito.
 * @property timestamp Data e ora del follow.
 */
data class Follow(
    @get:PropertyName("followerId")
    @set:PropertyName("followerId")
    var followerId: String = "",

    @get:PropertyName("followedId")
    @set:PropertyName("followedId")
    var followedId: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now()
)