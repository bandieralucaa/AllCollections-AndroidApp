package com.example.allcollections.comment

data class Comment(
    val collectionId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
