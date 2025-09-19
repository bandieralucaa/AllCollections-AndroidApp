package com.example.allcollections.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.notification.NotificationItem
import com.example.allcollections.profile.FollowType
import com.example.allcollections.profile.UserData
import com.example.allcollections.utils.formatRelativeTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn
    private val _loginErrorMessage = mutableStateOf<String?>(null)
    val loginErrorMessage: State<String?> = _loginErrorMessage
    private val _profileImageUrl = mutableStateOf<String?>(null)
    val profileImageUrl: State<String?> = _profileImageUrl

    var pendingUserData: UserData? = null



    fun registerUser(
        name: String,
        surname: String,
        dateOfBirth: LocalDate,
        email: String,
        password: String,
        gender: String,
        username: String,
        profileImageUri: Uri,
        context: Context,
        callback: (Boolean, String?) -> Unit
    ) {
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank() || gender.isBlank() || username.isBlank()) {
            callback(false, "Si prega di compilare tutti i campi")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser ?: return@addOnCompleteListener

                    saveProfilePicture(profileImageUri, context) { imageUrl ->
                        if (imageUrl == null) {
                            callback(false, "Errore nel caricamento dell'immagine")
                            return@saveProfilePicture
                        }

                        val user = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "dateOfBirth" to dateOfBirth.toString(),
                            "email" to email,
                            "gender" to gender,
                            "username" to username,
                            "profileImageUrl" to imageUrl
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
                    Log.d("LOGIN", "Login effettuato con successo: ${FirebaseAuth.getInstance().currentUser?.uid}")

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

    fun saveProfilePicture(uri: Uri, context: Context, onComplete: (String?) -> Unit) {
        Log.d("ProfileViewModel", "Inizio upload immagine")
        MediaManager.get().upload(uri)
            .unsigned("android_unsigned_upload")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    Log.d("ProfileViewModel", "Upload successo: $imageUrl")
                    onComplete(imageUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("ProfileViewModel", "Errore upload: ${error.description}")
                    onComplete(null)
                }

                override fun onStart(requestId: String) {
                    Log.d("ProfileViewModel", "Upload iniziato")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    Log.d("ProfileViewModel", "Upload in progresso: $bytes/$totalBytes")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.d("ProfileViewModel", "Upload rimandato: ${error.description}")
                }
            })
            .dispatch()
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
        callback: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return

        val userData = mutableMapOf<String, Any?>(
            "name" to name,
            "surname" to surname,
            "dateOfBirth" to dateOfBirth.toString(),
            "email" to email,
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

    fun followUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"
        val data = mapOf("followerId" to followerId, "followedId" to followedId)

        FirebaseFirestore.getInstance()
            .collection("follows")
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                val notification = mapOf(
                    "recipientId" to followedId,
                    "senderId" to followerId,
                    "type" to "follow",
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "read" to false
                )

                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .add(notification)

                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun isFollowing(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"

        FirebaseFirestore.getInstance()
            .collection("follows")
            .document(docId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun unfollowUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"

        FirebaseFirestore.getInstance()
            .collection("follows")
            .document(docId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getFollowerCount(userId: String, onResult: (Int) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("follows")
            .whereEqualTo("followedId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun getFollowingCount(userId: String, onResult: (Int) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun getFollowList(
        userId: String,
        type: FollowType,
        onResult: (List<UserData>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val query = when (type) {
            FollowType.FOLLOWERS -> db.collection("follows").whereEqualTo("followedId", userId)
            FollowType.FOLLOWING -> db.collection("follows").whereEqualTo("followerId", userId)
        }

        query.get().addOnSuccessListener { followDocs ->
            val otherUserIds = followDocs.documents.mapNotNull { doc ->
                when (type) {
                    FollowType.FOLLOWERS -> doc.getString("followerId")
                    FollowType.FOLLOWING -> doc.getString("followedId")
                }
            }

            if (otherUserIds.isEmpty()) {
                onResult(emptyList())
                return@addOnSuccessListener
            }

            db.collection("users")
                .whereIn(FieldPath.documentId(), otherUserIds)
                .get()
                .addOnSuccessListener { userDocs ->
                    val users = userDocs.documents.mapNotNull { doc ->
                        try {UserData(
                            userId = doc.id,
                            name = doc.getString("name") ?: "",
                            surname = doc.getString("surname") ?: "",
                            dateOfBirth = LocalDate.parse(doc.getString("dateOfBirth") ?: "2000-01-01"),
                            email = doc.getString("email") ?: "",
                            gender = doc.getString("gender") ?: "",
                            username = doc.getString("username") ?: "",
                            profileImageUrl = doc.getString("profileImageUrl") ?: ""
                        )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onResult(users)
                }
        }
    }

    fun getNotifications(userId: String, onResult: (List<NotificationItem>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val formatter = SimpleDateFormat("dd MMMM yyyy 'alle' HH:mm", Locale("it", "IT"))
        val notifications = mutableListOf<NotificationItem>()

        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                docs.documents.forEach { doc ->
                    val senderId = doc.getString("senderId") ?: return@forEach
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val formattedDate = timestamp?.let { formatRelativeTime(it) } ?: ""
                    val read = doc.getBoolean("read") ?: false
                    val notificationId = doc.id

                    db.collection("users").document(senderId).get()
                        .addOnSuccessListener { userDoc ->
                            val user = try {
                                UserData(
                                    userId = userDoc.id,
                                    name = userDoc.getString("name") ?: "",
                                    surname = userDoc.getString("surname") ?: "",
                                    dateOfBirth = LocalDate.parse(userDoc.getString("dateOfBirth") ?: "2000-01-01"),
                                    email = userDoc.getString("email") ?: "",
                                    gender = userDoc.getString("gender") ?: "",
                                    username = userDoc.getString("username") ?: "",
                                    profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }

                            user?.let {
                                notifications.add(NotificationItem(it, formattedDate, read, notificationId))
                                onResult(notifications.sortedByDescending { it.timestamp })
                            }
                        }
                }
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        FirebaseFirestore.getInstance()
            .collection("notifications")
            .document(notificationId)
            .update("read", true)
    }

}

