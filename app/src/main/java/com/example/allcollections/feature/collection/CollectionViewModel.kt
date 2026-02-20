package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.Comment
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

data class CollectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val collections: List<UserCollection> = emptyList(),
    val items: List<CollectionItem> = emptyList()
)

data class CreateCollectionState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdCollectionId: String? = null
)

class CollectionViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = Firebase.auth

    sealed class CollectionEvent {
        data class CollectionDeleted(val collectionId: String) : CollectionEvent()
        data class Error(val message: String) : CollectionEvent()
    }

    private val _events = MutableSharedFlow<CollectionEvent>()
    val events: SharedFlow<CollectionEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            _uiState.update { it.copy(error = throwable.message ?: "Errore sconosciuto") }
        }
    }

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _createCollectionState = MutableStateFlow(CreateCollectionState())
    val createCollectionState: StateFlow<CreateCollectionState> = _createCollectionState.asStateFlow()

    // ────────── COLLECTIONS ──────────

    fun loadUserCollections(userId: String) = viewModelScope.launch(exceptionHandler) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val snapshot = db.collection("collections").whereEqualTo("iduser", userId).get().await()
            val collections = snapshot.documents.mapNotNull { it.toObject<UserCollection>()?.copy(id = it.id) }
            _uiState.update { it.copy(isLoading = false, collections = collections) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Errore caricamento collezioni: ${e.message}") }
        }
    }

    fun saveCollection(name: String, category: String, description: String) = viewModelScope.launch(exceptionHandler) {
        val userId = auth.currentUser?.uid ?: run {
            _createCollectionState.update { it.copy(error = "Utente non autenticato") }
            return@launch
        }
        if (_createCollectionState.value.isLoading) return@launch
        _createCollectionState.update { it.copy(isLoading = true, error = null, createdCollectionId = null) }
        try {
            val collectionData = hashMapOf(
                "name" to name,
                "category" to category,
                "description" to description,
                "iduser" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
                "collectionImageUrl" to ""
            )
            val result = db.collection("collections").add(collectionData).await()
            _createCollectionState.update { it.copy(isLoading = false, createdCollectionId = result.id) }
            launch { loadUserCollections(userId) }
        } catch (e: Exception) {
            _createCollectionState.update { it.copy(isLoading = false, error = "Errore salvataggio collezione: ${e.message}") }
        }
    }

    fun resetCreateCollectionState() {
        _createCollectionState.update { CreateCollectionState() }
    }

    fun updateCollection(
        updatedCollection: UserCollection,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("collections").document(updatedCollection.id).update(
                mapOf(
                    "name" to updatedCollection.name,
                    "category" to updatedCollection.category,
                    "description" to updatedCollection.description
                )
            ).await()
            updatedCollection.iduser?.let { loadUserCollections(it) }
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e.message ?: "Errore aggiornamento collezione")
        }
    }

    fun updateCollectionImage(collectionId: String, newImageUri: Uri, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        uploadImageToCloudinary(collectionId, newImageUri, onSuccess, onFailure)
    }

    fun getCollectionById(
        collectionId: String,
        onSuccess: (UserCollection) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections").document(collectionId).get().await()
            if (!doc.exists()) { onFailure("Collezione non trovata"); return@launch }
            val collection = UserCollection(
                id = doc.id,
                iduser = doc.getString("iduser") ?: "",
                name = doc.getString("name") ?: "",
                category = doc.getString("category") ?: "",
                description = doc.getString("description") ?: "",
                collectionImageUrl = doc.getString("collectionImageUrl") ?: "",
                username = ""
            )
            onSuccess(collection)
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore sconosciuto")
        }
    }

    fun deleteCollection(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        launch {
            try {
                val currentCollections = _uiState.value.collections.toMutableList()
                val collectionToDelete = currentCollections.find { it.id == collectionId }
                if (collectionToDelete != null) {
                    currentCollections.remove(collectionToDelete)
                    _uiState.update { it.copy(collections = currentCollections) }
                }
                val firestoreJob = launch(Dispatchers.IO) {
                    val itemsSnapshot = db.collection("collections").document(collectionId).collection("items").get().await()
                    itemsSnapshot.documents.forEach { doc ->
                        doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }
                        doc.reference.delete().await()
                    }
                    db.collection("comments").whereEqualTo("collectionId", collectionId).get().await().documents.forEach { it.reference.delete().await() }
                    db.collection("notifications").whereEqualTo("collectionId", collectionId).get().await().documents.forEach { it.reference.delete().await() }
                    db.collection("likes").whereEqualTo("collectionId", collectionId).get().await().documents.forEach { it.reference.delete().await() }
                    db.collection("collections").document(collectionId).delete().await()
                }
                firestoreJob.join()
                _events.emit(CollectionEvent.CollectionDeleted(collectionId))
            } catch (e: Exception) {
                _events.emit(CollectionEvent.Error("Errore eliminazione collezione: ${e.message}"))
                auth.currentUser?.uid?.let { loadUserCollections(it) }
            }
        }
    }

    fun getAllCollectionsWithUsernames(onSuccess: (List<UserCollection>) -> Unit, onFailure: (String) -> Unit) = viewModelScope.launch {
        try {
            val snapshot = db.collection("collections").get().await()
            val collections = snapshot.documents.mapNotNull { doc -> doc.toObject<UserCollection>()?.copy(id = doc.id) }
            val collectionsWithUsername = collections.map { coll ->
                val username = getUsernameByIdSync(coll.iduser ?: "")
                coll.copy(username = username)
            }
            onSuccess(collectionsWithUsername)
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore caricamento collezioni")
        }
    }

    fun getCollectionsByUserIds(userIds: List<String>, onSuccess: (List<UserCollection>) -> Unit) = viewModelScope.launch {
        try {
            if (userIds.isEmpty()) { onSuccess(emptyList()); return@launch }
            val snapshot = db.collection("collections").whereIn("iduser", userIds).get().await()
            val collections = snapshot.documents.mapNotNull { doc -> doc.toObject<UserCollection>()?.copy(id = doc.id) }
            val collectionsWithUsername = collections.map { coll ->
                val username = getUsernameByIdSync(coll.iduser ?: "")
                coll.copy(username = username)
            }
            onSuccess(collectionsWithUsername)
        } catch (e: Exception) {
            onSuccess(emptyList())
        }
    }

    private suspend fun getUsernameByIdSync(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("username") ?: "Utente"
        } catch (_: Exception) { "Utente" }
    }

    // ────────── ITEMS ──────────

    fun loadItems(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val snapshot = db.collection("collections").document(collectionId).collection("items").orderBy("timestamp").get().await()
            val items = snapshot.documents.mapNotNull { it.toObject<CollectionItem>()?.copy(id = it.id) }
            _uiState.update { it.copy(isLoading = false, items = items) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Errore caricamento oggetti: ${e.message}") }
        }
    }

    fun getItemById(collectionId: String, itemId: String, onSuccess: (CollectionItem) -> Unit, onFailure: (String) -> Unit) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections").document(collectionId).collection("items").document(itemId).get().await()
            val item = doc.toObject<CollectionItem>()
            if (item != null) onSuccess(item.copy(id = doc.id)) else onFailure("Oggetto non trovato")
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore sconosciuto")
        }
    }

    /** Notifica tutti gli utenti che hanno messo like alla collezione */
    private fun notifyLikers(
        collectionId: String,
        collectionName: String,
        notificationViewModel: NotificationViewModel
    ) {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("likes")
            .whereEqualTo("collectionId", collectionId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val likerUserId = doc.getString("userId") ?: return@forEach
                    if (likerUserId != currentUid) {
                        notificationViewModel.sendNewItemNotification(
                            recipientId = likerUserId,
                            collectionId = collectionId,
                            collectionName = collectionName
                        )
                    }
                }
            }
    }

    /** Aggiunge un oggetto con upload su Cloudinary */
    fun addItem(
        collectionId: String,
        imageUri: Uri,
        description: String,
        notificationViewModel: NotificationViewModel,
        onComplete: ((success: Boolean, message: String?) -> Unit)? = null
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(error = "Utente non autenticato") }
            onComplete?.invoke(false, "Utente non autenticato")
            return
        }
        viewModelScope.launch(exceptionHandler) {
            MediaManager.get().upload(imageUri)
                .option("folder", "$userId/collections/$collectionId/items")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val imageUrl = resultData?.get("secure_url") as? String
                        val publicId = resultData?.get("public_id") as? String
                        if (imageUrl != null) {
                            val itemData = hashMapOf(
                                "description" to description,
                                "imageUrl" to imageUrl,
                                "publicId" to publicId,
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            db.collection("collections").document(collectionId).collection("items").add(itemData)
                                .addOnSuccessListener {
                                    loadItems(collectionId)
                                    // Notifica chi ha messo like
                                    db.collection("collections").document(collectionId).get()
                                        .addOnSuccessListener { collDoc ->
                                            val collectionName = collDoc.getString("name") ?: ""
                                            notifyLikers(collectionId, collectionName, notificationViewModel)
                                        }
                                    onComplete?.invoke(true, null)
                                }
                                .addOnFailureListener { e ->
                                    _uiState.update { it.copy(isLoading = false, error = "Errore salvataggio item: ${e.message}") }
                                    onComplete?.invoke(false, e.message)
                                }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "URL immagine mancante") }
                            onComplete?.invoke(false, "URL immagine mancante")
                        }
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        _uiState.update { it.copy(isLoading = false, error = error?.description ?: "Errore upload") }
                        onComplete?.invoke(false, error?.description)
                    }
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }
    }

    fun updateItemDescription(collectionId: String, itemId: String, newDescription: String, onSuccess: (() -> Unit)? = null, onFailure: ((String) -> Unit)? = null) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("collections").document(collectionId).collection("items").document(itemId).update("description", newDescription).await()
            loadItems(collectionId)
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e.message ?: "Errore aggiornamento descrizione")
        }
    }

    fun uploadItemImageAndUpdate(collectionId: String, itemId: String, imageUri: Uri, updatedDescription: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure("Utente non autenticato"); return }
        MediaManager.get().upload(imageUri)
            .option("folder", "$userId/collections/$collectionId/items")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val newUrl = resultData?.get("secure_url") as? String
                    val newPublicId = resultData?.get("public_id") as? String
                    if (newUrl != null && newPublicId != null) {
                        viewModelScope.launch(exceptionHandler) {
                            try {
                                val snapshot = db.collection("collections").document(collectionId).collection("items").document(itemId).get().await()
                                val oldPublicId = snapshot.getString("publicId")
                                db.collection("collections").document(collectionId).collection("items").document(itemId).update(
                                    mapOf("description" to updatedDescription, "imageUrl" to newUrl, "publicId" to newPublicId)
                                ).await()
                                oldPublicId?.let { deleteImageFromCloudinary(it) }
                                loadItems(collectionId)
                                onSuccess()
                            } catch (e: Exception) {
                                onFailure(e.message ?: "Errore aggiornamento item")
                            }
                        }
                    } else onFailure("URL immagine mancante")
                }
                override fun onError(requestId: String?, error: ErrorInfo?) { onFailure(error?.description ?: "Errore upload") }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    fun updateItemImage(collectionId: String, itemId: String, newImageUri: Uri, onSuccess: (() -> Unit)? = null, onFailure: ((String) -> Unit)? = null) {
        uploadItemImageAndUpdate(collectionId, itemId, newImageUri, updatedDescription = "", onSuccess = onSuccess ?: {}, onFailure = onFailure ?: {})
    }

    fun deleteItemFromCollection(collectionId: String, itemId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections").document(collectionId).collection("items").document(itemId).get().await()
            doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }
            db.collection("collections").document(collectionId).collection("items").document(itemId).delete().await()
            loadItems(collectionId)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Errore eliminazione item: ${e.message}") }
        }
    }

    // ────────── CLOUDINARY ──────────

    fun uploadImageToCloudinary(collectionId: String, imageUri: Uri, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run { onFailure("Utente non autenticato"); return }
        MediaManager.get().upload(imageUri)
            .option("folder", "$userId/collections/$collectionId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    if (imageUrl != null) {
                        db.collection("collections").document(collectionId).update("collectionImageUrl", imageUrl)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e.message ?: "Errore aggiornamento collezione") }
                    } else onFailure("URL immagine mancante")
                }
                override fun onError(requestId: String?, error: ErrorInfo?) { onFailure(error?.description ?: "Errore upload") }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun deleteImageFromCloudinary(publicId: String) {
        try { MediaManager.get().cloudinary.uploader().destroy(publicId, mapOf("invalidate" to true)) } catch (_: Exception) {}
    }

    // ────────── COMMENTS ──────────

    fun addComment(comment: Comment, notificationViewModel: NotificationViewModel) {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("comments").add(comment)
            .addOnSuccessListener {
                val collectionId = comment.collectionId
                if (collectionId.isBlank()) return@addOnSuccessListener
                db.collection("collections").document(collectionId).get()
                    .addOnSuccessListener { collDoc ->
                        val recipientId = collDoc.getString("iduser")
                        val collectionName = collDoc.getString("name") ?: ""
                        if (!recipientId.isNullOrBlank() && recipientId != currentUid) {
                            notificationViewModel.sendCommentNotification(
                                recipientId = recipientId,
                                collectionId = collectionId,
                                collectionName = collectionName,
                                commentText = comment.text
                            )
                        }
                    }
            }
    }

    fun getComments(collectionId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = db.collection("comments")
            .whereEqualTo("collectionId", collectionId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val comments = snapshot?.documents?.mapNotNull { doc -> doc.toObject<Comment>()?.copy(id = doc.id) } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)

    fun deleteComment(commentId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId).delete().await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore eliminazione commento: ${e.message}"))
        }
    }

    fun updateComment(commentId: String, newText: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId).update("text", newText.trim()).await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore modifica commento: ${e.message}"))
        }
    }

    fun getUsernameById(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc -> onResult(doc.getString("username") ?: "Utente") }
            .addOnFailureListener { onResult("Utente") }
    }

    fun getCollectionsByUserId(userId: String, onSuccess: (List<UserCollection>) -> Unit) {
        getCollectionsByUserIds(listOf(userId), onSuccess)
    }

    // ────────── LIKES ──────────

    fun likeCollection(collectionId: String, notificationViewModel: NotificationViewModel) = viewModelScope.launch(exceptionHandler) {
        val currentUid = auth.currentUser?.uid ?: return@launch
        val docId = "${currentUid}_$collectionId"
        try {
            db.collection("likes").document(docId).set(
                mapOf("userId" to currentUid, "collectionId" to collectionId, "timestamp" to FieldValue.serverTimestamp())
            ).await()
            val collDoc = db.collection("collections").document(collectionId).get().await()
            val recipientId = collDoc.getString("iduser")
            val collectionName = collDoc.getString("name") ?: ""
            if (!recipientId.isNullOrBlank() && recipientId != currentUid) {
                notificationViewModel.sendLikeNotification(
                    recipientId = recipientId,
                    collectionId = collectionId,
                    collectionName = collectionName
                )
            }
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore like: ${e.message}"))
        }
    }

    fun unlikeCollection(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        val currentUid = auth.currentUser?.uid ?: return@launch
        val docId = "${currentUid}_$collectionId"
        try {
            db.collection("likes").document(docId).delete().await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore rimozione like: ${e.message}"))
        }
    }

    fun hasLiked(collectionId: String, onResult: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: run { onResult(false); return }
        val docId = "${currentUid}_$collectionId"
        db.collection("likes").document(docId).get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    fun getLikesCount(collectionId: String, onResult: (Int) -> Unit) {
        db.collection("likes").whereEqualTo("collectionId", collectionId).get()
            .addOnSuccessListener { onResult(it.size()) }
            .addOnFailureListener { onResult(0) }
    }

    fun getLikedCollections(onSuccess: (List<UserCollection>) -> Unit) = viewModelScope.launch {
        val currentUid = auth.currentUser?.uid ?: run { onSuccess(emptyList()); return@launch }
        try {
            val likesSnapshot = db.collection("likes").whereEqualTo("userId", currentUid).get().await()
            val collectionIds = likesSnapshot.documents.mapNotNull { it.getString("collectionId") }
            if (collectionIds.isEmpty()) { onSuccess(emptyList()); return@launch }
            val collections = collectionIds.mapNotNull { collectionId ->
                val doc = db.collection("collections").document(collectionId).get().await()
                if (!doc.exists()) return@mapNotNull null
                val username = getUsernameByIdSync(doc.getString("iduser") ?: "")
                doc.toObject<UserCollection>()?.copy(id = doc.id, username = username)
            }
            onSuccess(collections)
        } catch (e: Exception) {
            onSuccess(emptyList())
        }
    }

    fun getLikers(collectionId: String, onSuccess: (List<UserData>) -> Unit) = viewModelScope.launch {
        try {
            val likesSnapshot = db.collection("likes")
                .whereEqualTo("collectionId", collectionId)
                .get().await()
            val userIds = likesSnapshot.documents.mapNotNull { it.getString("userId") }
            if (userIds.isEmpty()) { onSuccess(emptyList()); return@launch }
            val users = userIds.mapNotNull { userId ->
                val doc = db.collection("users").document(userId).get().await()
                if (!doc.exists()) return@mapNotNull null
                UserData(
                    userId = doc.id,
                    name = doc.getString("name") ?: "",
                    surname = doc.getString("surname") ?: "",
                    username = doc.getString("username") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    email = "",
                    dateOfBirth = "",
                    gender = ""
                )
            }
            onSuccess(users)
        } catch (e: Exception) {
            onSuccess(emptyList())
        }
    }
}