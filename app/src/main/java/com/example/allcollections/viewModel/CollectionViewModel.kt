package com.example.allcollections.viewModel

import androidx.lifecycle.ViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.allcollections.collection.UserCollection
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class CollectionViewModel : ViewModel() {

    private val db = com.google.firebase.Firebase.firestore
    private val storage = Firebase.storage
    private val auth = com.google.firebase.Firebase.auth

    fun saveCollection(
        name: String,
        category: String,
        description: String,
        iduser: String?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (iduser != null) {
                val collectionData = hashMapOf(
                    "name" to name,
                    "category" to category,
                    "description" to description,
                    "iduser" to iduser
                )

                try {
                    val documentReference = db.collection("collections").add(collectionData).await()
                    onSuccess(documentReference.id)
                } catch (e: Exception) {
                    onFailure("Errore durante il salvataggio della collezione: ${e.message}")
                }
            } else {
                onFailure("Utente non autenticato")
            }
        }
    }

    fun getCollections(iduser: String?, onSuccess: (List<UserCollection>) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            if (iduser != null) {
                try {
                    val querySnapshot = db.collection("collections").whereEqualTo("iduser", iduser).get().await()
                    val collections = querySnapshot.toObjects(UserCollection::class.java)
                    onSuccess(collections)
                } catch (e: Exception) {
                    onFailure("Errore durante il recupero delle collezioni: ${e.message}")
                }
            } else {
                onFailure("Utente non autenticato")
            }
        }
    }

    fun saveImageCollection(collectionId: String, imageUri: Uri, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val storageRef = storage.reference
        val imageRef = storageRef.child("$userId/collections/$collectionId/image.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    db.collection("collections")
                        .document(collectionId)
                        .update("collectionImageUrl", uri.toString())
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
}

