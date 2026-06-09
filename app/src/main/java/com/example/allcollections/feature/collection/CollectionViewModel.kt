package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.CollectionUiState
import com.example.allcollections.data.model.Comment
import com.example.allcollections.data.model.CreateCollectionState
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

/**
 * ViewModel per la gestione delle collezioni e dei loro oggetti.
 *
 * Gestisce il ciclo di vita completo di una collezione: creazione, lettura,
 * aggiornamento, eliminazione, upload immagini su Cloudinary, commenti
 * (sia sulla collezione sia sui singoli oggetti), sistema like e
 * notifiche ai follower tramite [NotificationViewModel].
 *
 * Espone lo stato UI tramite [StateFlow] e gli eventi one-shot (eliminazione,
 * errori) tramite [SharedFlow] per evitare che vengano riosservati
 * alla ricomposizione.
 */
class CollectionViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = Firebase.auth

    // ────────── EVENTI ONE-SHOT ──────────

    /**
     * Evento emesso una sola volta dal ViewModel verso la UI.
     */
    sealed class CollectionEvent {
        /** La collezione con [collectionId] è stata eliminata con successo. */
        data class CollectionDeleted(val collectionId: String) : CollectionEvent()
        /** Si è verificato un errore durante un'operazione asincrona. */
        data class Error(val message: String) : CollectionEvent()
    }

    private val _events = MutableSharedFlow<CollectionEvent>()
    val events: SharedFlow<CollectionEvent> = _events.asSharedFlow()

    /** Handler globale per eccezioni non gestite nei coroutine del ViewModel. */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            _uiState.update { it.copy(error = throwable.message ?: "Errore sconosciuto") }
        }
    }

    private val _uiState = MutableStateFlow(CollectionUiState())

    /** Stato UI per le schermate collezioni (loading, error, lista collections e items). */
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _createCollectionState = MutableStateFlow(CreateCollectionState())

    /** Stato UI per il flusso di creazione di una nuova collezione. */
    val createCollectionState: StateFlow<CreateCollectionState> = _createCollectionState.asStateFlow()

    // ────────── COLLECTIONS ──────────

    /**
     * Carica tutte le collezioni dell'utente [userId] da Firestore.
     *
     * @param userId ID dell'utente di cui caricare le collezioni.
     */
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

    /**
     * Crea una nuova collezione su Firestore per l'utente corrente.
     *
     * Al successo aggiorna [createCollectionState] con l'ID della collezione creata,
     * triggerando la navigazione alla schermata di aggiunta immagine.
     *
     * @param name Nome della collezione.
     * @param category Categoria della collezione.
     * @param description Descrizione della collezione (opzionale).
     */
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

    /** Azzera lo stato di creazione collezione per evitare ri-navigazioni spurie. */
    fun resetCreateCollectionState() {
        _createCollectionState.update { CreateCollectionState() }
    }

    /**
     * Aggiorna nome, categoria e descrizione di una collezione esistente.
     *
     * @param updatedCollection Collezione con i nuovi valori da salvare.
     * @param onSuccess Callback invocato al completamento con successo.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
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
            loadUserCollections(updatedCollection.iduser)
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e.message ?: "Errore aggiornamento collezione")
        }
    }

    /**
     * Aggiorna l'immagine di copertina di una collezione tramite Cloudinary.
     *
     * @param collectionId ID della collezione.
     * @param newImageUri URI locale della nuova immagine.
     * @param onSuccess Callback invocato al completamento con successo.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
    fun updateCollectionImage(collectionId: String, newImageUri: Uri, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        uploadImageToCloudinary(collectionId, newImageUri, onSuccess, onFailure)
    }

    /**
     * Recupera una singola collezione da Firestore tramite [collectionId].
     *
     * @param collectionId ID della collezione da recuperare.
     * @param onSuccess Callback invocato con la [UserCollection] trovata.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
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

    /**
     * Elimina una collezione e tutti i dati correlati (items, commenti, notifiche, like).
     *
     * L'eliminazione avviene in modo ottimista: la collezione è rimossa dalla UI immediatamente
     * e poi cancellata fisicamente da Firestore su un dispatcher IO.
     *
     * @param collectionId ID della collezione da eliminare.
     */
    fun deleteCollection(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        launch {
            try {
                // Rimozione ottimistica dalla UI
                val currentCollections = _uiState.value.collections.toMutableList()
                currentCollections.removeAll { it.id == collectionId }
                _uiState.update { it.copy(collections = currentCollections) }

                val firestoreJob = launch(Dispatchers.IO) {
                    // Elimina items con relative immagini Cloudinary e commenti
                    val itemsSnapshot = db.collection("collections").document(collectionId).collection("items").get().await()
                    itemsSnapshot.documents.forEach { doc ->
                        doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }
                        db.collection("comments")
                            .whereEqualTo("collectionId", collectionId)
                            .whereEqualTo("itemId", doc.id)
                            .get().await().documents.forEach { it.reference.delete().await() }
                        doc.reference.delete().await()
                    }
                    // Elimina commenti, notifiche, like e il documento collezione
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

    /**
     * Recupera tutte le collezioni con username del proprietario denormalizzato.
     *
     * @param onSuccess Callback con la lista delle collezioni arricchite con username.
     * @param onFailure Callback con il messaggio di errore.
     */
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

    /**
     * Recupera le collezioni degli utenti con ID in [userIds], con username denormalizzato.
     *
     * @param userIds Lista di userId di cui caricare le collezioni.
     * @param onSuccess Callback con la lista delle collezioni trovate.
     */
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

    /** Recupera lo username di un utente in modo sospeso (per uso interno). */
    private suspend fun getUsernameByIdSync(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("username") ?: "Utente"
        } catch (_: Exception) { "Utente" }
    }

    // ────────── ITEMS ──────────

    /**
     * Carica gli oggetti di una collezione ordinati per timestamp.
     *
     * @param collectionId ID della collezione.
     */
    fun loadItems(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val snapshot = db.collection("collections").document(collectionId)
                .collection("items").orderBy("timestamp").get().await()
            val items = snapshot.documents.mapNotNull { it.toObject<CollectionItem>()?.copy(id = it.id) }
            _uiState.update { it.copy(isLoading = false, items = items) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Errore caricamento oggetti: ${e.message}") }
        }
    }

    /**
     * Recupera un singolo oggetto da Firestore.
     *
     * @param collectionId ID della collezione contenente l'oggetto.
     * @param itemId ID dell'oggetto.
     * @param onSuccess Callback con il [CollectionItem] trovato.
     * @param onFailure Callback con il messaggio di errore.
     */
    fun getItemById(collectionId: String, itemId: String, onSuccess: (CollectionItem) -> Unit, onFailure: (String) -> Unit) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections").document(collectionId).collection("items").document(itemId).get().await()
            val item = doc.toObject<CollectionItem>()
            if (item != null) onSuccess(item.copy(id = doc.id)) else onFailure("Oggetto non trovato")
        } catch (e: Exception) {
            onFailure(e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Invia notifiche push a tutti gli utenti che hanno messo like alla collezione.
     * Esclude l'utente corrente.
     */
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

    /**
     * Aggiunge un nuovo oggetto a una collezione con upload immagine su Cloudinary.
     *
     * Al successo: salva l'oggetto su Firestore, ricarica la lista items e notifica i likers.
     *
     * @param collectionId ID della collezione di destinazione.
     * @param imageUri URI locale dell'immagine da caricare.
     * @param description Descrizione dell'oggetto.
     * @param notificationViewModel ViewModel per notificare i likers.
     * @param onComplete Callback `(success, errorMessage)` al termine dell'operazione.
     */
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
                            db.collection("collections").document(collectionId).collection("items")
                                .add(itemData)
                                .addOnSuccessListener {
                                    loadItems(collectionId)
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

    /**
     * Aggiorna solo la descrizione di un oggetto.
     *
     * @param collectionId ID della collezione.
     * @param itemId ID dell'oggetto.
     * @param newDescription Nuova descrizione da salvare.
     * @param onSuccess Callback invocato al successo.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
    fun updateItemDescription(
        collectionId: String,
        itemId: String,
        newDescription: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("collections").document(collectionId).collection("items")
                .document(itemId).update("description", newDescription).await()
            loadItems(collectionId)
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e.message ?: "Errore aggiornamento descrizione")
        }
    }

    /**
     * Carica una nuova immagine su Cloudinary e aggiorna sia l'URL che la descrizione dell'oggetto.
     * Elimina la vecchia immagine da Cloudinary dopo il successo.
     *
     * @param collectionId ID della collezione.
     * @param itemId ID dell'oggetto da aggiornare.
     * @param imageUri URI locale della nuova immagine.
     * @param updatedDescription Nuova descrizione da salvare contestualmente.
     * @param onSuccess Callback invocato al successo.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
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
                                val snapshot = db.collection("collections").document(collectionId)
                                    .collection("items").document(itemId).get().await()
                                val oldPublicId = snapshot.getString("publicId")
                                db.collection("collections").document(collectionId)
                                    .collection("items").document(itemId).update(
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

    /**
     * Elimina un oggetto da una collezione, includendo l'immagine Cloudinary e i commenti correlati.
     *
     * @param collectionId ID della collezione.
     * @param itemId ID dell'oggetto da eliminare.
     */
    fun deleteItemFromCollection(collectionId: String, itemId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            val doc = db.collection("collections").document(collectionId).collection("items").document(itemId).get().await()
            doc.getString("publicId")?.let { deleteImageFromCloudinary(it) }
            db.collection("comments")
                .whereEqualTo("collectionId", collectionId)
                .whereEqualTo("itemId", itemId)
                .get().await().documents.forEach { it.reference.delete().await() }
            db.collection("collections").document(collectionId).collection("items").document(itemId).delete().await()
            loadItems(collectionId)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Errore eliminazione item: ${e.message}") }
        }
    }

    // ────────── CLOUDINARY ──────────

    /**
     * Carica un'immagine su Cloudinary e aggiorna il campo `collectionImageUrl` della collezione.
     *
     * @param collectionId ID della collezione.
     * @param imageUri URI locale dell'immagine.
     * @param onSuccess Callback invocato al successo.
     * @param onFailure Callback invocato con il messaggio di errore.
     */
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

    /**
     * Elimina un'immagine da Cloudinary tramite il suo [publicId].
     * Gestisce silenziosamente gli errori loggandoli senza propagarli.
     *
     * @param publicId Public ID Cloudinary dell'immagine da eliminare.
     */
    private fun deleteImageFromCloudinary(publicId: String) {
        try {
            MediaManager.get().cloudinary.uploader().destroy(publicId, mapOf("invalidate" to true))
        } catch (e: Exception) {
            android.util.Log.e("CollectionViewModel", "Errore eliminazione immagine Cloudinary (publicId=$publicId): ${e.message}", e)
        }
    }

    // ────────── COMMENTS (COLLEZIONE) ──────────

    /**
     * Aggiunge un commento alla collezione e invia una notifica al proprietario.
     *
     * @param comment Commento da aggiungere (con [Comment.itemId] vuoto).
     * @param notificationViewModel ViewModel per inviare la notifica al proprietario.
     */
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

    /**
     * Osserva in tempo reale i commenti di una collezione (esclusi quelli degli oggetti).
     *
     * @param collectionId ID della collezione.
     * @return [Flow] aggiornato in tempo reale con la lista dei commenti.
     */
    fun getComments(collectionId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = db.collection("comments")
            .whereEqualTo("collectionId", collectionId)
            .whereEqualTo("itemId", "")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val comments = snapshot?.documents?.mapNotNull { doc -> doc.toObject<Comment>()?.copy(id = doc.id) } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Elimina un commento.
     *
     * @param commentId ID del documento commento da eliminare.
     */
    fun deleteComment(commentId: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId).delete().await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore eliminazione commento: ${e.message}"))
        }
    }

    /**
     * Aggiorna il testo di un commento.
     *
     * @param commentId ID del documento commento da aggiornare.
     * @param newText Nuovo testo (verrà trimmato).
     */
    fun updateComment(commentId: String, newText: String) = viewModelScope.launch(exceptionHandler) {
        try {
            db.collection("comments").document(commentId).update("text", newText.trim()).await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore modifica commento: ${e.message}"))
        }
    }

    // ────────── COMMENTS (OGGETTO) ──────────

    /**
     * Aggiunge un commento a un oggetto della collezione e invia una notifica al proprietario.
     *
     * Riutilizza la stessa collection Firestore `"comments"`, distinguendo i commenti
     * degli oggetti tramite il campo `itemId` non vuoto.
     *
     * @param comment Commento da aggiungere (con [Comment.itemId] valorizzato).
     * @param notificationViewModel ViewModel per inviare la notifica al proprietario.
     */
    fun addItemComment(comment: Comment, notificationViewModel: NotificationViewModel) {
        val currentUid = auth.currentUser?.uid ?: return
        val itemId = comment.itemId
        db.collection("comments").add(comment)
            .addOnSuccessListener {
                val collectionId = comment.collectionId
                if (collectionId.isBlank()) return@addOnSuccessListener
                db.collection("collections").document(collectionId).get()
                    .addOnSuccessListener { collDoc ->
                        val recipientId = collDoc.getString("iduser")
                        val collectionName = collDoc.getString("name") ?: ""
                        if (!recipientId.isNullOrBlank() && recipientId != currentUid) {
                            db.collection("collections").document(collectionId)
                                .collection("items").document(itemId).get()
                                .addOnSuccessListener { itemDoc ->
                                    val itemDescription = itemDoc.getString("description")
                                    notificationViewModel.sendItemCommentNotification(
                                        recipientId = recipientId,
                                        collectionId = collectionId,
                                        collectionName = collectionName,
                                        itemId = itemId,
                                        itemDescription = itemDescription,
                                        commentText = comment.text
                                    )
                                }
                        }
                    }
            }
    }

    /**
     * Osserva in tempo reale i commenti di un oggetto specifico.
     *
     * @param collectionId ID della collezione contenente l'oggetto.
     * @param itemId ID dell'oggetto di cui caricare i commenti.
     * @return [Flow] aggiornato in tempo reale con i commenti dell'oggetto.
     */
    fun getItemComments(collectionId: String, itemId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = db.collection("comments")
            .whereEqualTo("collectionId", collectionId)
            .whereEqualTo("itemId", itemId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Comment>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Recupera lo username di un utente dato il suo ID.
     *
     * @param userId ID dell'utente.
     * @param onResult Callback con lo username trovato, o `"Utente"` come fallback.
     */
    fun getUsernameById(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc -> onResult(doc.getString("username") ?: "Utente") }
            .addOnFailureListener { onResult("Utente") }
    }

    /**
     * Recupera le collezioni di un singolo utente tramite il suo ID.
     *
     * @param userId ID dell'utente.
     * @param onSuccess Callback con la lista delle collezioni trovate.
     */
    fun getCollectionsByUserId(userId: String, onSuccess: (List<UserCollection>) -> Unit) {
        getCollectionsByUserIds(listOf(userId), onSuccess)
    }

    // ────────── LIKES ──────────

    /**
     * Aggiunge un like alla collezione e notifica il proprietario.
     *
     * @param collectionId ID della collezione.
     * @param notificationViewModel ViewModel per la notifica al proprietario.
     */
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

    /**
     * Rimuove il like dell'utente corrente dalla collezione.
     *
     * @param collectionId ID della collezione da cui rimuovere il like.
     */
    fun unlikeCollection(collectionId: String) = viewModelScope.launch(exceptionHandler) {
        val currentUid = auth.currentUser?.uid ?: return@launch
        val docId = "${currentUid}_$collectionId"
        try {
            db.collection("likes").document(docId).delete().await()
        } catch (e: Exception) {
            _events.emit(CollectionEvent.Error("Errore rimozione like: ${e.message}"))
        }
    }

    /**
     * Verifica se l'utente corrente ha messo like alla collezione.
     *
     * @param collectionId ID della collezione.
     * @param onResult Callback con `true` se l'utente ha già messo like.
     */
    fun hasLiked(collectionId: String, onResult: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: run { onResult(false); return }
        val docId = "${currentUid}_$collectionId"
        db.collection("likes").document(docId).get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Conta il numero totale di like su una collezione.
     *
     * @param collectionId ID della collezione.
     * @param onResult Callback con il conteggio like.
     */
    fun getLikesCount(collectionId: String, onResult: (Int) -> Unit) {
        db.collection("likes").whereEqualTo("collectionId", collectionId).get()
            .addOnSuccessListener { onResult(it.size()) }
            .addOnFailureListener { onResult(0) }
    }

    /**
     * Recupera le collezioni a cui l'utente corrente ha messo like.
     *
     * @param onSuccess Callback con la lista delle collezioni con like.
     */
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

    /**
     * Recupera la lista degli utenti che hanno messo like alla collezione.
     *
     * @param collectionId ID della collezione.
     * @param onSuccess Callback con la lista di [UserData] dei likers.
     */
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