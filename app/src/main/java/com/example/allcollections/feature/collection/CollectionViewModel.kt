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


/**
 * Stato UI per CollectionViewModel
 */
data class CollectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val collections: List<UserCollection> = emptyList(),
    val items: List<CollectionItem> = emptyList()
)

/**
 * Stato specifico per la creazione di collezioni
 */
data class CreateCollectionState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdCollectionId: String? = null
)

/**
 * ViewModel per gestione collezioni e oggetti
 */
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

    // Stato separato per la creazione di collezioni
    private val _createCollectionState = MutableStateFlow(CreateCollectionState())
    val createCollectionState: StateFlow<CreateCollectionState> = _createCollectionState.asStateFlow()

    // ────────── COLLECTIONS ──────────

    /** Carica tutte le collezioni di un utente */
    fun loadUserCollections(userId: String) = viewModelScope.launch(exceptionHandler) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val snapshot = db.collection("collections")
                .whereEqualTo("iduser", userId)
                .get()
                .await()
            val collections = snapshot.documents.mapNotNull { it.toObject<UserCollection>()?.copy(id = it.id) }
            _uiState.update { it.copy(isLoading = false, collections = collections) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Errore caricamento collezioni: ${e.message}") }
        }
    }

    /** Salva una nuova collezione (versione migliorata) */
    fun saveCollection(name: String, category: String, description: String) = viewModelScope.launch(exceptionHandler) {
        val userId = auth.currentUser?.uid ?: run {
            _createCollectionState.update { it.copy(error = "Utente non autenticato") }
            return@launch
        }

        // Previene doppie chiamate
        if (_createCollectionState.value.isLoading) return@launch

        _createCollectionState.update {
            it.copy(
                isLoading = true,
                error = null,
                createdCollectionId = null
            )
        }

        try {
            val collectionData = hashMapOf(
                "name" to name,
                "category" to category,
                "description" to description,
                "iduser" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
                "collectionImageUrl" to "" // Immagine vuota iniziale
            )

            // Salva e ottieni il riferimento del documento
            val result = db.collection("collections").add(collectionData).await()

            // Aggiorna lo stato con l'ID della nuova collezione
            _createCollectionState.update {
                it.copy(
                    isLoading = false,
                    createdCollectionId = result.id
                )
            }

            // Ricarica le collezioni in background (senza bloccare l'UI)
            launch {
                loadUserCollections(userId)
            }

        } catch (e: Exception) {
            _createCollectionState.update {
                it.copy(
                    isLoading = false,
                    error = "Errore salvataggio collezione: ${e.message}"
                )
            }
        }
    }

    /** Resetta lo stato della creazione */
    fun resetCreateCollectionState() {
        _createCollectionState.update { CreateCollectionState() }
    }

    /** Aggiorna i dati di una collezione */
    fun updateCollection(
        updatedCollection: UserCollection,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("collections")
                .document(updatedCollection.id)
                .update(
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

    /** Aggiorna immagine collezione su Cloudinary */
    fun updateCollectionImage(
        collectionId: String,
        newImageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        uploadImageToCloudinary(collectionId, newImageUri, onSuccess, onFailure)
    }

    /** Recupera una singola collezione */
    fun getCollectionById(
        collectionId: String,
        onSuccess: (UserCollection) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(exceptionHandler) {
        try {

            val doc = db.collection("collections").document(collectionId).get().await()

            if (!doc.exists()) {
                onFailure("Collezione non trovata")
                return@launch
            }

            val iduser = doc.getString("iduser")
            val name = doc.getString("name")
            val category = doc.getString("category")
            val description = doc.getString("description")
            val collectionImageUrl = doc.getString("collectionImageUrl")

            // Crea la UserCollection MANUALMENTE invece di usare toObject()
            val collection = UserCollection(
                id = doc.id,
                iduser = iduser ?: "",
                name = name ?: "",
                category = category ?: "",
                description = description ?: "",
                collectionImageUrl = collectionImageUrl ?: "",
                username = "" // Sarà calcolato dopo se necessario
            )

            onSuccess(collection)

        } catch (e: Exception) {
            onFailure(e.message ?: "Errore sconosciuto")
        }
    }


    /** Elimina una collezione e tutti gli oggetti/commenti associati */
    fun deleteCollection(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        val deleteJob = launch {
            try {

                // 1. Rimuovi IMMEDIATAMENTE dallo stato
                val currentCollections = _uiState.value.collections.toMutableList()
                val collectionToDelete = currentCollections.find { it.id == collectionId }

                if (collectionToDelete != null) {
                    currentCollections.remove(collectionToDelete)
                    _uiState.update { it.copy(collections = currentCollections) }
                }

                // 2. Elimina dal database - in un job separato per non bloccare l'UI
                val firestoreJob = launch(Dispatchers.IO) {
                    try {
                        // Cancella items e immagini
                        val itemsSnapshot = db.collection("collections")
                            .document(collectionId)
                            .collection("items")
                            .get()
                            .await()

                        itemsSnapshot.documents.forEach { doc ->
                            doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }
                            doc.reference.delete().await()
                        }

                        // Cancella commenti e notifiche
                        db.collection("comments").whereEqualTo("collectionId", collectionId)
                            .get().await().documents.forEach { it.reference.delete().await() }

                        db.collection("notifications").whereEqualTo("collectionId", collectionId)
                            .get().await().documents.forEach { it.reference.delete().await() }

                        // Cancella collezione
                        db.collection("collections").document(collectionId).delete().await()


                    } catch (e: Exception) {
                        throw e
                    }
                }

                // Attendi il completamento dell'eliminazione Firestore
                firestoreJob.join()

                // 3. Emetti evento di successo
                _events.emit(CollectionEvent.CollectionDeleted(collectionId))

            } catch (e: Exception) {
                _events.emit(CollectionEvent.Error("Errore eliminazione collezione: ${e.message}"))

                // Se c'è un errore, RICARICA le collezioni per sincronizzare
                val userId = auth.currentUser?.uid
                userId?.let { loadUserCollections(it) }
            }
        }

    }


    // Restituisce tutte le collezioni pubbliche con username dell'autore
    fun getAllCollectionsWithUsernames(
        onSuccess: (List<UserCollection>) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            val snapshot = db.collection("collections").get().await()
            val collections = snapshot.documents.mapNotNull { doc ->
                doc.toObject<UserCollection>()?.copy(id = doc.id)
            }

            // Recupera username per ogni collection
            val collectionsWithUsername = collections.map { coll ->
                val username = getUsernameByIdSync(coll.iduser ?: "")
                coll.copy(username = username)
            }

            onSuccess(collectionsWithUsername)
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore caricamento collezioni")
        }
    }

    // Restituisce le collezioni di un elenco di utenti
    fun getCollectionsByUserIds(
        userIds: List<String>,
        onSuccess: (List<UserCollection>) -> Unit
    ) = viewModelScope.launch {
        try {
            if (userIds.isEmpty()) {
                onSuccess(emptyList())
                return@launch
            }

            val snapshot = db.collection("collections")
                .whereIn("iduser", userIds)
                .get()
                .await()

            val collections = snapshot.documents.mapNotNull { doc ->
                doc.toObject<UserCollection>()?.copy(id = doc.id)
            }

            val collectionsWithUsername = collections.map { coll ->
                val username = getUsernameByIdSync(coll.iduser ?: "")
                coll.copy(username = username)
            }

            onSuccess(collectionsWithUsername)
        } catch (e: Exception) {
            onSuccess(emptyList())
        }
    }

    // Funzione di supporto sincrona per ottenere username (da usare solo dentro coroutine)
    private suspend fun getUsernameByIdSync(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("username") ?: "Utente"
        } catch (_: Exception) {
            "Utente"
        }
    }

    // ────────── ITEMS ──────────

    /** Carica tutti gli oggetti di una collezione */
    fun loadItems(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val snapshot = db.collection("collections")
                .document(collectionId)
                .collection("items")
                .orderBy("timestamp")
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { it.toObject<CollectionItem>()?.copy(id = it.id) }
            _uiState.update { it.copy(isLoading = false, items = items) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Errore caricamento oggetti: ${e.message}") }
        }
    }

    /** Recupera un singolo oggetto */
    fun getItemById(
        collectionId: String,
        itemId: String,
        onSuccess: (CollectionItem) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections")
                .document(collectionId)
                .collection("items")
                .document(itemId)
                .get()
                .await()
            val item = doc.toObject<CollectionItem>()
            if (item != null) onSuccess(item.copy(id = doc.id))
            else onFailure("Oggetto non trovato")
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore sconosciuto")
        }
    }

    /** Aggiunge un oggetto con upload su Cloudinary */
    fun addItem(
        collectionId: String,
        imageUri: Uri,
        description: String,
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
                            db.collection("collections")
                                .document(collectionId)
                                .collection("items")
                                .add(itemData)
                                .addOnSuccessListener {
                                    loadItems(collectionId)
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
                })
                .dispatch()
        }
    }

    /** Aggiorna solo descrizione */
    fun updateItemDescription(
        collectionId: String,
        itemId: String,
        newDescription: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("collections")
                .document(collectionId)
                .collection("items")
                .document(itemId)
                .update("description", newDescription)
                .await()
            loadItems(collectionId)
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e.message ?: "Errore aggiornamento descrizione")
        }
    }

    /** Aggiorna immagine e/o descrizione di un item */
    fun uploadItemImageAndUpdate(
        collectionId: String,
        itemId: String,
        imageUri: Uri,
        updatedDescription: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
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
                                val snapshot = db.collection("collections")
                                    .document(collectionId)
                                    .collection("items")
                                    .document(itemId)
                                    .get().await()
                                val oldPublicId = snapshot.getString("publicId")

                                db.collection("collections")
                                    .document(collectionId)
                                    .collection("items")
                                    .document(itemId)
                                    .update(
                                        mapOf(
                                            "description" to updatedDescription,
                                            "imageUrl" to newUrl,
                                            "publicId" to newPublicId
                                        )
                                    ).await()

                                oldPublicId?.let { deleteImageFromCloudinary(it) }
                                loadItems(collectionId)
                                onSuccess()
                            } catch (e: Exception) {
                                onFailure(e.message ?: "Errore aggiornamento item")
                            }
                        }
                    } else {
                        onFailure("URL immagine mancante")
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) { onFailure(error?.description ?: "Errore upload") }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    /** Aggiorna solo immagine di un item */
    fun updateItemImage(
        collectionId: String,
        itemId: String,
        newImageUri: Uri,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        uploadItemImageAndUpdate(collectionId, itemId, newImageUri, updatedDescription = "", onSuccess = onSuccess ?: {}, onFailure = onFailure ?: {})
    }

    /** Elimina un item e immagine */
    fun deleteItemFromCollection(collectionId: String, itemId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections")
                .document(collectionId)
                .collection("items")
                .document(itemId)
                .get().await()
            doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }

            db.collection("collections")
                .document(collectionId)
                .collection("items")
                .document(itemId)
                .delete().await()
            loadItems(collectionId)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Errore eliminazione item: ${e.message}") }
        }
    }

    // ────────── CLOUDINARY ──────────

    /** Upload immagine collezione su Cloudinary */
    fun uploadImageToCloudinary(
        collectionId: String,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run { onFailure("Utente non autenticato"); return }

        MediaManager.get().upload(imageUri)
            .option("folder", "$userId/collections/$collectionId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    if (imageUrl != null) {
                        db.collection("collections").document(collectionId)
                            .update("collectionImageUrl", imageUrl)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e.message ?: "Errore aggiornamento collezione") }
                    } else onFailure("URL immagine mancante")
                }
                override fun onError(requestId: String?, error: ErrorInfo?) { onFailure(error?.description ?: "Errore upload") }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    /** Cancella immagine Cloudinary */
    private fun deleteImageFromCloudinary(publicId: String) {
        try { MediaManager.get().cloudinary.uploader().destroy(publicId, mapOf("invalidate" to true)) } catch (_: Exception) {}
    }

    // ────────── COMMENTS ──────────

    /** Aggiunge commento e invia notifica */
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

    /** Carica commenti di una collezione */
    fun getComments(collectionId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = db.collection("comments")
            .whereEqualTo("collectionId", collectionId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Comment>()?.copy(id = doc.id)
                } ?: emptyList()

                trySend(comments)
            }

        // Chiudi il listener quando il Flow viene cancellato
        awaitClose {
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /** Elimina un commento */
    fun deleteComment(commentId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId).delete().await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore eliminazione commento: ${e.message}"))
        }
    }

    /** Modifica testo di un commento */
    fun updateComment(commentId: String, newText: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId)
                .update("text", newText.trim())
                .await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore modifica commento: ${e.message}"))
        }
    }

    /** Ottieni username di un utente */
    fun getUsernameById(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc -> onResult(doc.getString("username") ?: "Utente") }
            .addOnFailureListener { onResult("Utente") }
    }

    fun getCollectionsByUserId(userId: String, onSuccess: (List<UserCollection>) -> Unit) {
        getCollectionsByUserIds(listOf(userId), onSuccess)
    }
}