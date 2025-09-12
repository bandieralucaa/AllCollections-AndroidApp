package com.example.allcollections.collection

data class CollectionItem(
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val id: String = ""
)
