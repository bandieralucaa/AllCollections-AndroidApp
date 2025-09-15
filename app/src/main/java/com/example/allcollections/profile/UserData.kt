package com.example.allcollections.profile

import java.time.LocalDate

data class UserData(
    val userId: String = "",
    val name: String,
    val surname: String,
    val dateOfBirth: LocalDate,
    val email: String,
    val gender: String,
    val username: String
)
