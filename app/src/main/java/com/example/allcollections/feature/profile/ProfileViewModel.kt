package com.example.allcollections.feature.profile

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.data.model.FollowUser
import com.example.allcollections.data.model.UserData
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ProfileViewModel : ViewModel() {

    // ----------------------------------
    // FIREBASE
    // ----------------------------------
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ----------------------------------
    // LISTENER REGISTRATIONS
    // ----------------------------------
    private val listeners = mutableListOf<ListenerRegistration>()

    // ----------------------------------
    // UI STATE
    // ----------------------------------
    private val _profileImageUrl = mutableStateOf<String?>(null)
    val profileImageUrl: State<String?> = _profileImageUrl

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _followersList = mutableStateOf<List<FollowUser>>(emptyList())
    val followersList: State<List<FollowUser>> = _followersList

    private val _followingList = mutableStateOf<List<FollowUser>>(emptyList())
    val followingList: State<List<FollowUser>> = _followingList

    private val _isLoadingFollowers = mutableStateOf(false)
    val isLoadingFollowers: State<Boolean> = _isLoadingFollowers

    private val _isLoadingFollowing = mutableStateOf(false)
    val isLoadingFollowing: State<Boolean> = _isLoadingFollowing


    companion object {
        private const val USERS = "users"
        private const val FOLLOWS = "follows"

        private const val FIELD_NAME = "name"
        private const val FIELD_SURNAME = "surname"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_DATE = "dateOfBirth"
        private const val FIELD_PROFILE_IMAGE = "profileImageUrl"

        private const val FIELD_FOLLOWER_ID = "followerId"
        private const val FIELD_FOLLOWED_ID = "followedId"

        const val DEFAULT_PROFILE_IMAGE =
            "https://res.cloudinary.com/dqtr2napz/image/upload/v1758965362/default_image_profile_okdl8h.png"
    }

    // ======================================================
    // CLEANUP (da chiamare prima del logout)
    // ======================================================

    /**
     * Rimuove tutti i listener attivi per evitare errori di permessi dopo il logout
     */
    fun cleanupListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        Log.d("ProfileViewModel", "Listener puliti")
    }

    // ======================================================
    // REGISTRAZIONE
    // ======================================================

    fun registerUser(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                auth.currentUser?.uid?.let(onSuccess)
                    ?: onFailure("UserId nullo")
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Errore registrazione")
            }
    }

    fun saveUserData(
        userId: String,
        name: String,
        surname: String,
        username: String,
        email: String,
        gender: String,
        dateOfBirth: LocalDate,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = mapOf(
            FIELD_NAME to name,
            FIELD_SURNAME to surname,
            FIELD_USERNAME to username,
            FIELD_EMAIL to email,
            FIELD_GENDER to gender,
            FIELD_DATE to dateOfBirth.toString(),
            FIELD_PROFILE_IMAGE to DEFAULT_PROFILE_IMAGE
        )

        db.collection(USERS).document(userId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Errore salvataggio dati") }
    }

    /**
     * Verifica se uno username è già presente nel database.
     *
     * @param username Lo username da controllare
     * @return true se lo username esiste già, false altrimenti
     */
    suspend fun isUsernameTaken(username: String): Boolean {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // ======================================================
    // VERIFICA EMAIL
    // ======================================================

    /**
     * Invia email di verifica all'utente corrente
     */
    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "Nessun utente loggato")
            return
        }

        user.sendEmailVerification()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    /**
     * Controlla se l'email dell'utente corrente è verificata
     */
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    /**
     * Ricarica i dati dell'utente per aggiornare lo stato di verifica email
     */
    fun reloadUser(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run {
            onComplete(false)
            return
        }

        user.reload()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // ======================================================
    // CLOUDINARY (usato sia in registrazione che edit)
    // ======================================================

    fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        MediaManager.get().upload(imageUri)
            .unsigned("android_unsigned_upload")
            .callback(object : UploadCallback {

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    _isLoading.value = false
                    if (url != null) onSuccess(url)
                    else onFailure("URL Cloudinary nullo")
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    _isLoading.value = false
                    Log.e("CLOUDINARY", error.description)
                    onFailure(error.description)
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    // ======================================================
    // SALVATAGGIO / UPDATE FOTO PROFILO
    // ======================================================

    fun saveProfileImageUrl(
        userId: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(USERS)
            .document(userId)
            .update(FIELD_PROFILE_IMAGE, imageUrl)
            .addOnSuccessListener {
                _profileImageUrl.value = imageUrl
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Errore aggiornamento foto")
            }
    }

    // ======================================================
    // EDIT PROFILE (DATI UTENTE)
    // ======================================================

    fun updateUserData(
        name: String,
        surname: String,
        username: String,
        email: String,
        gender: String,
        dateOfBirth: LocalDate,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(false, "Utente non autenticato")
            return
        }

        val data = mapOf(
            FIELD_NAME to name,
            FIELD_SURNAME to surname,
            FIELD_USERNAME to username,
            FIELD_EMAIL to email,
            FIELD_GENDER to gender,
            FIELD_DATE to dateOfBirth.toString()
        )

        db.collection(USERS)
            .document(userId)
            .update(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    // ======================================================
    // LETTURA PROFILO
    // ======================================================

    suspend fun getUserData(): UserData? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext null
        val doc = db.collection(USERS).document(userId).get().await()
        doc.toObject(UserData::class.java)?.copy(userId = userId)
    }

    fun loadProfileImage() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val doc = db.collection(USERS).document(userId).get().await()
            _profileImageUrl.value = doc.getString(FIELD_PROFILE_IMAGE)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    fun getUserProfilePhoto(userId: String, onResult: (String?) -> Unit) {
        db.collection(USERS).document(userId).get()
            .addOnSuccessListener { doc ->
                val url = doc.getString(FIELD_PROFILE_IMAGE) ?: DEFAULT_PROFILE_IMAGE
                onResult(url)
            }
            .addOnFailureListener {
                onResult(DEFAULT_PROFILE_IMAGE)
            }
    }



    // ======================================================
    // LOGIN / LOGOUT
    // ======================================================

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun logout() {
        cleanupListeners()  // ← Pulisce i listener PRIMA del logout
        auth.signOut()
    }

    // ======================================================
    // FOLLOW SYSTEM
    // ======================================================

    fun followUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"
        val data = mapOf(
            FIELD_FOLLOWER_ID to followerId,
            FIELD_FOLLOWED_ID to followedId
        )

        db.collection(FOLLOWS).document(docId)
            .set(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun unfollowUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"
        db.collection(FOLLOWS).document(docId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun loadFollowers(userId: String) {
        _isLoadingFollowers.value = true
        val listener = db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWED_ID, userId)
            .addSnapshotListener { docs, error ->
                if (error != null) {
                    _followersList.value = emptyList()
                    _isLoadingFollowers.value = false
                    return@addSnapshotListener
                }
                val list = docs?.documents?.mapNotNull { it.getString(FIELD_FOLLOWER_ID) } ?: emptyList()
                loadUsersDetails(list) { users ->
                    _followersList.value = users
                    _isLoadingFollowers.value = false
                }
            }
        listeners.add(listener)  // ← Aggiunge il listener alla lista
    }

    fun loadFollowing(userId: String) {
        _isLoadingFollowing.value = true
        val listener = db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWER_ID, userId)
            .addSnapshotListener { docs, error ->
                if (error != null) {
                    _followingList.value = emptyList()
                    _isLoadingFollowing.value = false
                    return@addSnapshotListener
                }
                val list = docs?.documents?.mapNotNull { it.getString(FIELD_FOLLOWED_ID) } ?: emptyList()
                loadUsersDetails(list) { users ->
                    _followingList.value = users
                    _isLoadingFollowing.value = false
                }
            }
        listeners.add(listener)  // ← Aggiunge il listener alla lista
    }

    private fun loadUsersDetails(
        userIds: List<String>,
        onResult: (List<FollowUser>) -> Unit
    ) {
        val result = mutableListOf<FollowUser>()
        if (userIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        userIds.forEach { uid ->
            db.collection(USERS).document(uid).get()
                .addOnSuccessListener { doc ->
                    result.add(
                        FollowUser(
                            userId = uid,
                            username = doc.getString(FIELD_USERNAME) ?: "Utente",
                            profileImageUrl = doc.getString(FIELD_PROFILE_IMAGE)
                                ?: DEFAULT_PROFILE_IMAGE
                        )
                    )
                    if (result.size == userIds.size) onResult(result)
                }
        }
    }

    fun getFollowedUserIds(onResult: (List<String>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }

        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWER_ID, currentUserId)
            .get()
            .addOnSuccessListener { docs ->
                val followedIds = docs.documents.mapNotNull { it.getString(FIELD_FOLLOWED_ID) }
                onResult(followedIds)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getFollowerCount(userId: String, onResult: (Int) -> Unit) {
        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWED_ID, userId)
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun getFollowingCount(userId: String, onResult: (Int) -> Unit) {
        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWER_ID, userId)
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    fun isFollowing(currentUserId: String, targetUserId: String, onResult: (Boolean) -> Unit) {
        val docId = "${currentUserId}_$targetUserId"
        db.collection(FOLLOWS)
            .document(docId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ======================================================
    // PASSWORD
    // ======================================================

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onResult(false, "Utente non autenticato")
            return
        }

        val credential = EmailAuthProvider.getCredential(
            user.email ?: return,
            currentPassword
        )

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { onResult(false, it.message) }
            }
            .addOnFailureListener {
                onResult(false, "Password attuale errata")
            }
    }

    // Aggiungi questa funzione in ProfileViewModel.kt
    fun getUsernameById(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username") ?: "Utente"
                onResult(username)
            }
            .addOnFailureListener {
                onResult("Utente")
            }
    }
}