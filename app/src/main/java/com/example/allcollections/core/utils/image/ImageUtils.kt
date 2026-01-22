package com.example.allcollections.core.utils.image

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Converte un URI in Bitmap.
 *
 * Supporta sia le API <28 che quelle moderne.
 *
 * @param imageUri URI dell'immagine.
 * @param contentResolver ContentResolver del contesto.
 * @return Bitmap decodificata dall'URI.
 * @throws IOException se l'immagine non può essere decodificata.
 */
fun uriToBitmap(imageUri: Uri, contentResolver: ContentResolver): Bitmap {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        }
    } catch (e: Exception) {
        throw IOException("Impossibile decodificare l'immagine da URI: $imageUri", e)
    }
}

/**
 * Salva un'immagine nello storage esterno (MediaStore).
 *
 * - Genera un nome file se non specificato.
 * - Usa compressione JPEG al 100%.
 * - Gestisce gli stream in maniera sicura con 'use'.
 *
 * @param imageUri URI della bitmap da salvare.
 * @param contentResolver ContentResolver del contesto.
 * @param name Nome file opzionale. Default = "IMG_<timestamp>.jpg".
 * @throws FileNotFoundException se l'outputStream non può essere aperto.
 * @throws IOException se la compressione o scrittura fallisce.
 */
fun saveImageToStorage(
    imageUri: Uri,
    contentResolver: ContentResolver,
    name: String = "IMG_${SystemClock.uptimeMillis()}.jpg"
) {
    val bitmap = uriToBitmap(imageUri, contentResolver)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val savedUri = contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: throw FileNotFoundException("Impossibile creare l'URI per salvare l'immagine.")

    contentResolver.openOutputStream(savedUri)?.use { outputStream ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
            throw IOException("Errore durante la compressione dell'immagine")
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(savedUri, contentValues, null, null)
    }
}
