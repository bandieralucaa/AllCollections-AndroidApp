package com.example.allcollections.viewModel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn
    private val _loginErrorMessage = mutableStateOf<String?>(null)
    val loginErrorMessage: State<String?> = _loginErrorMessage
    private val _profileImageUrl = mutableStateOf<String?>(null)
    val profileImageUrl: State<String?> = _profileImageUrl


    fun registerUser(
        name: String,
        surname: String,
        dateOfBirth: LocalDate,
        email: String,
        password: String,
        gender: String,
        username: String,
        callback: (Boolean, String?) -> Unit
    ) {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank() || gender.isBlank() || username.isBlank()) {
            callback(false, "Si prega di compilare tutti i campi")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val user = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "dateOfBirth" to dateOfBirth.toString(),
                            "email" to email,
                            "password" to password,
                            "gender" to gender,
                            "username" to username
                        )

                        db.collection("users")
                            .document(currentUser.uid)
                            .set(user)
                            .addOnSuccessListener {
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                callback(false, "Errore durante la registrazione: ${e.message}")
                            }
                    }
                } else {
                    callback(false, "Errore durante la registrazione: ${task.exception?.message}")
                }
            }
    }


    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _loginErrorMessage.value = "Inserire email e/o password"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoggedIn.value = true
                    _loginErrorMessage.value = null
                    callback(true, null)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "L'utente non esiste."
                        is FirebaseAuthInvalidCredentialsException -> "Credenziali non valide."
                        else -> "Errore di login sconosciuto."
                    }
                    callback(false, errorMessage)
                }
            }
    }

    fun logout(callback: () -> Unit) {
        auth.signOut()
        _isLoggedIn.value = false
        callback()
    }

    fun saveProfilePicture(imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("$userId/profile_images/image.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { _ ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val userData = hashMapOf(
                        "profileImageUrl" to uri.toString()
                    )

                    val userDataJava = hashMapOf<String, Any>().apply {
                        putAll(userData)
                    }

                    db.collection("users")
                        .document(userId)
                        .update(userDataJava)
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    suspend fun getUsername(): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val userDocument = db.collection("users").document(userId).get().await()
        return userDocument.getString("username") ?: ""
    }

    suspend fun getProfileImage() {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val userDocument = db.collection("users").document(userId).get().await()
        _profileImageUrl.value = userDocument.getString("profileImageUrl")
    }

    suspend fun getUserData(): UserData {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

        return withContext(Dispatchers.IO) {
            val userDocument = db.collection("users").document(userId).get().await()

            UserData(
                name = userDocument.getString("name") ?: "",
                surname = userDocument.getString("surname") ?: "",
                dateOfBirth = LocalDate.parse(userDocument.getString("dateOfBirth") ?: ""),
                email = userDocument.getString("email") ?: "",
                password = userDocument.getString("password") ?: "",  // Avoid storing plain passwords in the database
                gender = userDocument.getString("gender") ?: "",
                username = userDocument.getString("username") ?: ""
            )
        }
    }

    fun updateUserData(
        name: String,
        surname: String,
        dateOfBirth: LocalDate,
        email: String,
        gender: String,
        username: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return

        val userData = mutableMapOf<String, Any?>(
            "name" to name,
            "surname" to surname,
            "dateOfBirth" to dateOfBirth.toString(),
            "email" to email,
            "password" to password,  // Evita di memorizzare le password in chiaro nel database
            "gender" to gender,
            "username" to username
        )

        db.collection("users")
            .document(userId)
            .update(userData)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante l'aggiornamento dei dati: ${e.message}")
            }
    }


}

data class UserData(
    val name: String,
    val surname: String,
    val dateOfBirth: LocalDate,
    val email: String,
    val password: String,
    val gender: String,
    val username: String
)
