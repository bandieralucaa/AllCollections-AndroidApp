package com.example.allcollections.viewModel

import androidx.lifecycle.ViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.allcollections.collection.CollectionItem
import com.example.allcollections.collection.UserCollection
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
        collectionId: String,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure("Utente non autenticato")
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
                        // Aggiorna Firestore con url immagine
                        db.collection("collections")
                            .document(collectionId)
                            .update("collectionImageUrl", url)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e.message ?: "Errore aggiornamento DB") }
                    } else {
                        onFailure("Errore: url immagine mancante")
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure(error?.description ?: "Errore upload immagine")
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
                    onSuccess(collection)
                } else {
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
                    if (imageUrl != null) {
                        val itemData = hashMapOf(
                            "description" to description,
                            "imageUrl" to imageUrl,
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



}
