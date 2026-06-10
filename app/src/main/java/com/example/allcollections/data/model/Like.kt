package com.example.allcollections.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Documento di relazione "like" tra un utente e una collezione.
 *
 * Corrisponde al documento Firestore nella collezione "likes".
 * L'ID del documento è solitamente `"${userId}_${collectionId}"`.
 *
 * @property userId ID dell'utente che ha messo like.
 * @property collectionId ID della collezione ricevente.
 * @property timestamp Data e ora del like.
 */
data class Like(
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("collectionId") @set:PropertyName("collectionId")
    var collectionId: String = "",

    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now()
)