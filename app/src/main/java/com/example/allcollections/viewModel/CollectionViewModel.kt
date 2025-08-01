package com.example.allcollections.viewModel

import androidx.lifecycle.ViewModel
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.collection.CollectionItem
import com.example.allcollections.collection.UserCollection
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class CollectionViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    fun saveCollection(
        name: String,
        category: String,
        description: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onFailure("Utente non autenticato")
            return
        }

        val collectionData = hashMapOf(
            "name" to name,
            "category" to category,
            "description" to description,
            "iduser" to userId
        )


        viewModelScope.launch {
            try {
                val documentReference = db.collection("collections").add(collectionData).await()
                onSuccess(documentReference.id)
            } catch (e: Exception) {
                onFailure("Errore durante il salvataggio della collezione: ${e.message}")
            }
        }
    }

    fun getCollections(
        iduser: String?,
        onSuccess: (List<UserCollection>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (iduser == null) {
            onFailure("Utente non autenticato")
            return
        }
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("collections")
                    .whereEqualTo("iduser", iduser)
                    .get()
                    .await()

                val collections = querySnapshot.documents.mapNotNull { doc ->
                    val collection = doc.toObject(UserCollection::class.java)
                    collection?.copy(id = doc.id) // 🔥 questo è ciò che serve
                }

                onSuccess(collections)
            } catch (e: Exception) {
                onFailure("Errore durante il recupero delle collezioni: ${e.message}")
            }
        }
    }

    fun uploadImageToCloudinary(
        collectionId: String?,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            onFailure("Utente non autenticato")
            return
        }

        if (collectionId.isNullOrEmpty()) {
            onFailure("collectionId mancante")
            return
        }

        MediaManager.get().upload(imageUri)
            .option("folder", "$userId/collections/$collectionId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        db.collection("collections")
                            .document(collectionId)
                            .update("collectionImageUrl", url)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onFailure("Errore aggiornamento DB: ${e.message}")
                            }
                    } else {
                        onFailure("URL immagine mancante nella risposta")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure("Errore upload Cloudinary: ${error?.description}")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }


    fun getCollectionById(
        collectionId: String,
        onSuccess: (UserCollection) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val docSnapshot = db.collection("collections").document(collectionId).get().await()
                val collection = docSnapshot.toObject(UserCollection::class.java)
                if (collection != null) {
                    val withId = collection.copy(id = docSnapshot.id)
                    onSuccess(withId)
                }
                else {
                    onFailure("Collezione non trovata")
                }
            } catch (e: Exception) {
                onFailure("Errore: ${e.message}")
            }
        }
    }

    fun addItemToCollection(
        collectionId: String,
        imageUri: Uri,
        description: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("Utente non autenticato")
            return
        }

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
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        db.collection("collections")
                            .document(collectionId)
                            .collection("items")
                            .add(itemData)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onFailure("Errore salvataggio oggetto: ${e.message}")
                            }
                    } else {
                        onFailure("Errore: URL immagine mancante")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure(error?.description ?: "Errore upload immagine")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }


    fun getItemsFromCollection(
        collectionId: String,
        onSuccess: (List<CollectionItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("collections")
                    .document(collectionId)
                    .collection("items")
                    .orderBy("timestamp")
                    .get()
                    .await()

                val items = querySnapshot.documents.mapNotNull { doc ->
                    val item = doc.toObject(CollectionItem::class.java)
                    item?.copy(id = doc.id)
                }

                onSuccess(items)
            } catch (e: Exception) {
                onFailure("Errore nel recupero degli oggetti: ${e.message}")
            }
        }
    }

    fun deleteItemFromCollection(
        collectionId: String,
        itemId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val itemRef = db.collection("collections")
            .document(collectionId)
            .collection("items")
            .document(itemId)

        viewModelScope.launch {
            try {
                val snapshot = itemRef.get().await()
                val publicId = snapshot.getString("publicId")

                itemRef.delete().await() // Cancella da Firestore

                publicId?.let {
                    deleteImageFromCloudinary(it) // Cancella da Cloudinary
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure("Errore eliminazione: ${e.message}")
            }
        }
    }

    fun deleteImageFromCloudinary(publicId: String): Boolean {
        val options = mapOf("invalidate" to true)

        return try {
            val result = MediaManager.get()
                .cloudinary
                .uploader()
                .destroy(publicId, options)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getItemById(
        collectionId: String,
        itemId: String,
        onSuccess: (CollectionItem) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val docSnapshot = db.collection("collections")
                    .document(collectionId)
                    .collection("items")
                    .document(itemId)
                    .get()
                    .await()

                val item = docSnapshot.toObject(CollectionItem::class.java)
                if (item != null) {
                    onSuccess(item.copy(id = docSnapshot.id))
                } else {
                    onFailure("Oggetto non trovato")
                }
            } catch (e: Exception) {
                onFailure("Errore caricamento oggetto: ${e.message}")
            }
        }
    }


    fun updateItemDescription(
        collectionId: String,
        itemId: String,
        newDescription: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                db.collection("collections")
                    .document(collectionId)
                    .collection("items")
                    .document(itemId)
                    .update("description", newDescription)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onFailure("Errore aggiornamento descrizione: ${e.message}")
            }
        }
    }

    fun updateItemImage(
        collectionId: String,
        itemId: String,
        newImageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("Utente non autenticato")
            return
        }

        MediaManager.get().upload(newImageUri)
            .option("folder", "$userId/collections/$collectionId/items")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val newImageUrl = resultData?.get("secure_url") as? String
                    val newPublicId = resultData?.get("public_id") as? String

                    if (newImageUrl != null && newPublicId != null) {
                        viewModelScope.launch {
                            try {
                                val snapshot = db.collection("collections")
                                    .document(collectionId)
                                    .collection("items")
                                    .document(itemId)
                                    .get()
                                    .await()

                                val oldPublicId = snapshot.getString("publicId")

                                db.collection("collections")
                                    .document(collectionId)
                                    .collection("items")
                                    .document(itemId)
                                    .update(
                                        mapOf(
                                            "imageUrl" to newImageUrl,
                                            "publicId" to newPublicId
                                        )
                                    )
                                    .await()

                                oldPublicId?.let {
                                    deleteImageFromCloudinary(it)
                                }

                                onSuccess()
                            } catch (e: Exception) {
                                onFailure("Errore aggiornamento immagine: ${e.message}")
                            }
                        }
                    } else {
                        onFailure("URL o publicId mancanti")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure(error?.description ?: "Errore upload immagine")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    fun deleteCollection(
        collectionId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val itemsSnapshot = db.collection("collections")
                    .document(collectionId)
                    .collection("items")
                    .get()
                    .await()

                if (itemsSnapshot.isEmpty) {
                    db.collection("collections")
                        .document(collectionId)
                        .delete()
                        .await()
                    onSuccess()
                } else {
                    val items = itemsSnapshot.documents

                    items.map { it.id }.forEach { itemId ->
                        deleteItemFromCollectionSuspend(collectionId, itemId)
                    }

                    db.collection("collections")
                        .document(collectionId)
                        .delete()
                        .await()
                    onSuccess()
                }
            } catch (e: Exception) {
                onFailure("Errore eliminazione: ${e.message}")
            }
        }
    }

    private suspend fun deleteItemFromCollectionSuspend(collectionId: String, itemId: String) {
        val itemRef = db.collection("collections")
            .document(collectionId)
            .collection("items")
            .document(itemId)

        val snapshot = itemRef.get().await()
        val publicId = snapshot.getString("publicId")

        itemRef.delete().await()

        publicId?.let { deleteImageFromCloudinary(it) }
    }


    fun UserCollection.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        map["id"] = id
        map["name"] = name
        map["category"] = category
        map["description"] = description
        collectionImageUrl?.let { map["collectionImageUrl"] = it }
        map["iduser"] = iduser

        return map
    }


    fun updateCollection(
        updatedCollection: UserCollection,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updateData = mapOf(
                    "name" to updatedCollection.name,
                    "category" to updatedCollection.category,
                    "description" to updatedCollection.description
                )

                Log.d("UpdateCollection", "ID: ${updatedCollection.id} | Dati: $updateData")


                db.collection("collections")
                    .document(updatedCollection.id)
                    .update(updateData)
                    .await()

                onSuccess()
            } catch (e: Exception) {
                onFailure("Errore durante l’aggiornamento: ${e.message}")
            }
        }
    }



    fun updateCollectionImage(
        collectionId: String,
        newImageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("Utente non autenticato")
            return
        }

        MediaManager.get().upload(newImageUri)
            .option("folder", "$userId/collections/$collectionId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val newImageUrl = resultData?.get("secure_url") as? String
                    val newPublicId = resultData?.get("public_id") as? String

                    if (newImageUrl != null && newPublicId != null) {
                        viewModelScope.launch {
                            try {
                                val snapshot = db.collection("collections")
                                    .document(collectionId)
                                    .get()
                                    .await()

                                val oldPublicId = snapshot.getString("publicId")

                                db.collection("collections")
                                    .document(collectionId)
                                    .update(
                                        mapOf(
                                            "collectionImageUrl" to newImageUrl,
                                            "publicId" to newPublicId
                                        )
                                    )
                                    .await()

                                oldPublicId?.let {
                                    deleteImageFromCloudinary(it)
                                }

                                onSuccess(newImageUrl)
                            } catch (e: Exception) {
                                onFailure("Errore aggiornamento immagine: ${e.message}")
                            }
                        }
                    } else {
                        onFailure("URL o publicId mancanti")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure(error?.description ?: "Errore upload immagine")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }





}
