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
import com.example.allcollections.data.model.Follow
import com.example.allcollections.data.model.FollowUser
import com.example.allcollections.data.model.UserData
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * ViewModel per la gestione del profilo utente.
 *
 * Questo ViewModel gestisce tutte le operazioni relative all'utente autenticato:
 * - Registrazione, login, logout, verifica email
 * - Lettura e aggiornamento dei dati del profilo (nome, cognome, username, bio, data di nascita, genere)
 * - Upload dell'immagine profilo su Cloudinary
 * - Sistema follow/following (segui/non seguire utenti, conteggi, liste)
 * - Cambio password con reautenticazione
 * - Esposizione di stato UI (loading, errori, liste follower/following)
 *
 * Utilizza Firebase Auth per l'autenticazione e Firestore per i dati utente e le relazioni di follow.
 * Per le immagini profilo, sfrutta Cloudinary tramite MediaManager.
 *
 * @see ProfileViewModel.Companion per costanti e percorsi Firestore
 */
class ProfileViewModel : ViewModel() {

    // ----------------------------------
    // FIREBASE
    // ----------------------------------
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ----------------------------------
    // LISTENER REGISTRATIONS
    // ----------------------------------
    /** Lista dei listener Firestore attivi (es. per follow in tempo reale). */
    private val listeners = mutableListOf<ListenerRegistration>()

    // ----------------------------------
    // UI STATE
    // ----------------------------------
    private val _profileImageUrl = mutableStateOf<String?>(null)
    /** URL dell'immagine profilo dell'utente corrente (osservabile). */
    val profileImageUrl: State<String?> = _profileImageUrl

    private val _isLoading = mutableStateOf(false)
    /** Indica se un'operazione di upload o salvataggio è in corso. */
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    /** Messaggio di errore da mostrare nella UI. */
    val errorMessage: State<String?> = _errorMessage

    private val _followersList = mutableStateOf<List<FollowUser>>(emptyList())
    /** Lista dei follower dell'utente osservato (aggiornata in tempo reale). */
    val followersList: State<List<FollowUser>> = _followersList

    private val _followingList = mutableStateOf<List<FollowUser>>(emptyList())
    /** Lista degli utenti seguiti dall'utente osservato (aggiornata in tempo reale). */
    val followingList: State<List<FollowUser>> = _followingList

    private val _isLoadingFollowers = mutableStateOf(false)
    /** Flag di caricamento per la lista follower. */
    val isLoadingFollowers: State<Boolean> = _isLoadingFollowers

    private val _isLoadingFollowing = mutableStateOf(false)
    /** Flag di caricamento per la lista following. */
    val isLoadingFollowing: State<Boolean> = _isLoadingFollowing

    companion object {
        // Costanti per i nomi delle collezioni Firestore
        private const val USERS = "users"
        private const val FOLLOWS = "follows"

        // Nomi dei campi nel documento utente
        private const val FIELD_NAME = "name"
        private const val FIELD_SURNAME = "surname"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_DATE = "dateOfBirth"
        private const val FIELD_PROFILE_IMAGE = "profileImageUrl"

        // Nomi dei campi nella collezione follows
        private const val FIELD_FOLLOWER_ID = "followerId"
        private const val FIELD_FOLLOWED_ID = "followedId"

        /** URL dell'immagine profilo predefinita (Cloudinary). */
        const val DEFAULT_PROFILE_IMAGE =
            "https://res.cloudinary.com/dqtr2napz/image/upload/v1781084965/profile-icon_hrobhm.png"
    }

    // ======================================================
    // CLEANUP (da chiamare prima del logout)
    // ======================================================

    /**
     * Rimuove tutti i listener Firestore attivi per evitare errori di permessi dopo il logout.
     * Deve essere chiamato prima di [logout].
     */
    fun cleanupListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        Log.d("ProfileViewModel", "Listener puliti")
    }

    // ======================================================
    // REGISTRAZIONE
    // ======================================================

    /**
     * Registra un nuovo utente con email e password su Firebase Auth.
     *
     * @param email Email dell'utente.
     * @param password Password scelta.
     * @param onSuccess Callback con l'UID del nuovo utente.
     * @param onFailure Callback con messaggio di errore.
     */
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

    /**
     * Salva i dati anagrafici dell'utente su Firestore dopo la registrazione.
     *
     * @param userId UID dell'utente.
     * @param name Nome.
     * @param surname Cognome.
     * @param username Username univoco.
     * @param email Email.
     * @param gender Genere.
     * @param dateOfBirth Data di nascita (convertita in stringa ISO).
     * @param onSuccess Callback di successo.
     * @param onFailure Callback con messaggio di errore.
     */
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
     * Verifica se uno username è già presente nel database (controllo univocità).
     *
     * @param username Lo username da controllare.
     * @return `true` se lo username esiste già, `false` altrimenti.
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
     * Invia un'email di verifica all'utente corrente.
     *
     * @param onResult Callback (successo, messaggioErrore).
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
     * Controlla se l'email dell'utente corrente è verificata.
     *
     * @return `true` se verificata, `false` altrimenti.
     */
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    /**
     * Ricarica i dati dell'utente da Firebase Auth per aggiornare lo stato di verifica email.
     *
     * @param onComplete Callback con esito del ricaricamento.
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

    /**
     * Carica un'immagine profilo su Cloudinary (unsigned upload).
     *
     * @param imageUri URI locale dell'immagine da caricare.
     * @param onSuccess Callback con l'URL pubblico Cloudinary.
     * @param onFailure Callback con descrizione dell'errore.
     */
    fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isLoading.value = true

        MediaManager.get().upload(imageUri)
            .unsigned("android_unsigned_upload") // Presigned upload preset
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

    /**
     * Aggiorna il campo `profileImageUrl` su Firestore e nello stato locale.
     *
     * @param userId UID dell'utente.
     * @param imageUrl URL dell'immagine (Cloudinary).
     * @param onSuccess Callback di successo.
     * @param onFailure Callback con messaggio di errore.
     */
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

    /**
     * Aggiorna i dati anagrafici dell'utente corrente.
     *
     * @param name Nome.
     * @param surname Cognome.
     * @param username Username.
     * @param email Email.
     * @param gender Genere.
     * @param dateOfBirth Data di nascita.
     * @param onResult Callback (successo, messaggioErrore).
     */
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

    /**
     * Aggiorna solo la biografia (bio) dell'utente corrente.
     *
     * @param bio Nuova biografia (verrà trimmata).
     * @param onResult Callback (successo, messaggioErrore).
     */
    fun saveBio(bio: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(false, "Utente non autenticato")
            return
        }
        db.collection(USERS).document(userId)
            .update("bio", bio.trim())
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    // ======================================================
    // LETTURA PROFILO
    // ======================================================

    /**
     * Recupera i dati completi dell'utente corrente da Firestore (sospeso).
     *
     * @return [UserData] se l'utente è autenticato e il documento esiste, altrimenti `null`.
     */
    suspend fun getUserData(): UserData? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext null
        val doc = db.collection(USERS).document(userId).get().await()
        doc.toObject(UserData::class.java)?.copy(userId = userId)
    }

    /**
     * Carica l'URL dell'immagine profilo dell'utente corrente nello stato [profileImageUrl].
     */
    fun loadProfileImage() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val doc = db.collection(USERS).document(userId).get().await()
            _profileImageUrl.value = doc.getString(FIELD_PROFILE_IMAGE)
        }
    }

    /**
     * Restituisce l'UID dell'utente corrente (se autenticato).
     *
     * @return UID o `null`.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Recupera l'URL della foto profilo di un qualsiasi utente (callback).
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con URL (default [DEFAULT_PROFILE_IMAGE] in caso di errore).
     */
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

    /**
     * Recupera la biografia (bio) di un qualsiasi utente.
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con il testo della bio (stringa vuota in caso di errore).
     */
    fun getUserBio(userId: String, onResult: (String) -> Unit) {
        db.collection(USERS).document(userId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("bio") ?: "")
            }
            .addOnFailureListener {
                onResult("")
            }
    }

    // ======================================================
    // LOGIN / LOGOUT
    // ======================================================

    /**
     * Esegue il login con email e password.
     *
     * @param email Email.
     * @param password Password.
     * @param onResult Callback (successo, messaggioErrore).
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    /**
     * Esegue il logout, pulendo prima i listener Firestore.
     */
    fun logout() {
        cleanupListeners()  // Pulisce i listener PRIMA del logout
        auth.signOut()
    }

    // ======================================================
    // FOLLOW SYSTEM
    // ======================================================

    /**
     * Fa sì che l'utente [followerId] segua l'utente [followedId].
     * @param followerId ID di chi segue.
     * @param followedId ID di chi viene seguito.
     * @param onResult Callback con esito (true = successo).
     */
    fun followUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"
        val follow = Follow(
            followerId = followerId,
            followedId = followedId,
            timestamp = Timestamp.now()
        )
        db.collection(FOLLOWS).document(docId)
            .set(follow)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Rimuove il follow da [followerId] verso [followedId].
     *
     * @param followerId ID di chi smette di seguire.
     * @param followedId ID di chi non viene più seguito.
     * @param onResult Callback con esito.
     */
    fun unfollowUser(followerId: String, followedId: String, onResult: (Boolean) -> Unit) {
        val docId = "${followerId}_$followedId"
        db.collection(FOLLOWS).document(docId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Carica in tempo reale la lista dei follower di un utente (observabile tramite [followersList]).
     * Il listener Firestore viene automaticamente gestito e rimosso con [cleanupListeners].
     *
     * @param userId ID dell'utente di cui caricare i follower.
     */
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
                val follows = docs?.documents?.mapNotNull { it.toObject(Follow::class.java) } ?: emptyList()
                val followerIds = follows.map { it.followerId }
                loadUsersDetails(followerIds) { users ->
                    _followersList.value = users
                    _isLoadingFollowers.value = false
                }
            }
        listeners.add(listener)
    }

    /**
     * Carica in tempo reale la lista degli utenti seguiti da un utente (observabile tramite [followingList]).
     *
     * @param userId ID dell'utente di cui caricare i seguiti.
     */
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
                val follows = docs?.documents?.mapNotNull { it.toObject(Follow::class.java) } ?: emptyList()
                val followedIds = follows.map { it.followedId }
                loadUsersDetails(followedIds) { users ->
                    _followingList.value = users
                    _isLoadingFollowing.value = false
                }
            }
        listeners.add(listener)
    }

    /**
     * Carica i dettagli (username, foto) di una lista di ID utente.
     * Gestisce anche il caso di fallimento di una singola query, continuando con le altre.
     *
     * @param userIds Lista di ID utente.
     * @param onResult Callback con la lista di [FollowUser] ottenuta.
     */
    private fun loadUsersDetails(
        userIds: List<String>,
        onResult: (List<FollowUser>) -> Unit
    ) {
        val result = mutableListOf<FollowUser>()
        if (userIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        var completedQueries = 0
        userIds.forEach { uid ->
            db.collection(USERS).document(uid).get()
                .addOnSuccessListener { doc ->
                    synchronized(result) {
                        result.add(
                            FollowUser(
                                userId = uid,
                                username = doc.getString(FIELD_USERNAME) ?: "Utente",
                                profileImageUrl = doc.getString(FIELD_PROFILE_IMAGE) ?: DEFAULT_PROFILE_IMAGE
                            )
                        )
                        completedQueries++
                        if (completedQueries == userIds.size) {
                            onResult(result.toList())
                        }
                    }
                }
                .addOnFailureListener {
                    // In caso di errore, aggiungi comunque un utente con dati di fallback
                    synchronized(result) {
                        result.add(
                            FollowUser(
                                userId = uid,
                                username = "Utente",
                                profileImageUrl = DEFAULT_PROFILE_IMAGE
                            )
                        )
                        completedQueries++
                        if (completedQueries == userIds.size) {
                            onResult(result.toList())
                        }
                    }
                }
        }
    }

    /**
     * Recupera la lista degli ID degli utenti seguiti dall'utente corrente.
     * @param onResult Callback con lista di ID.
     */
    fun getFollowedUserIds(onResult: (List<String>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }
        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWER_ID, currentUserId)
            .get()
            .addOnSuccessListener { docs ->
                val follows = docs.documents.mapNotNull { it.toObject(Follow::class.java) }
                val followedIds = follows.map { it.followedId }
                onResult(followedIds)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /**
     * Restituisce il numero di follower di un utente.
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con il conteggio.
     */
    fun getFollowerCount(userId: String, onResult: (Int) -> Unit) {
        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWED_ID, userId)
            .get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }

    /**
     * Restituisce il numero di utenti seguiti da un utente.
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con il conteggio.
     */
    fun getFollowingCount(userId: String, onResult: (Int) -> Unit) {
        db.collection(FOLLOWS)
            .whereEqualTo(FIELD_FOLLOWER_ID, userId)
            .get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }

    /**
     * Verifica se l'utente [currentUserId] segue l'utente [targetUserId].
     *
     * @param currentUserId ID dell'utente loggato (follower potenziale).
     * @param targetUserId ID dell'utente target.
     * @param onResult Callback con esito booleano.
     */
    fun isFollowing(currentUserId: String, targetUserId: String, onResult: (Boolean) -> Unit) {
        val docId = "${currentUserId}_$targetUserId"
        db.collection(FOLLOWS)
            .document(docId)
            .get()
            .addOnSuccessListener { doc -> onResult(doc.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    // ======================================================
    // PASSWORD
    // ======================================================

    /**
     * Cambia la password dell'utente corrente dopo aver verificato quella attuale.
     *
     * @param currentPassword Password attuale (per reautenticazione).
     * @param newPassword Nuova password.
     * @param onResult Callback (successo, messaggioErrore).
     */
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

    /**
     * Recupera lo username di un utente dato il suo ID (callback).
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con lo username trovato, o `"Utente"` come fallback.
     */
    fun getUsernameById(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("username") ?: "Utente")
            }
            .addOnFailureListener {
                onResult("Utente")
            }
    }
}