package com.example.allcollections.core.utils.permissions

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Stato corrente di un permesso Android.
 */
enum class PermissionStatus {

    /** Stato iniziale; il sistema non ha ancora verificato il permesso. */
    Unknown,

    /** Il permesso è stato concesso dall'utente. */
    Granted,

    /** Il permesso è stato negato, ma l'utente può essere richiesto di nuovo. */
    Denied,

    /** Il permesso è stato negato permanentemente ("Non chiedere più"). Occorre aprire le impostazioni. */
    PermanentlyDenied
}

/**
 * Contratto per la gestione di un singolo permesso Android in un Composable.
 *
 * Ottenuto tramite [rememberPermission]; non istanziare direttamente.
 */
interface PermissionHandler {
    /** Stringa del permesso Android (es. `Manifest.permission.CAMERA`). */
    val permission: String

    /** Stato corrente del permesso (aggiornato automaticamente). */
    val status: PermissionStatus

    /** Avvia la richiesta di permesso al sistema. */
    fun launchPermissionRequest()
}

/**
 * Composable che crea e ricorda il gestore per un singolo permesso Android.
 *
 * Al primo avvio controlla lo stato reale del permesso per determinare se è già
 * concesso o se è stato precedentemente negato, evitando richieste ridondanti.
 * Dopo ogni risposta dell'utente, distingue tra negazione semplice
 * ([PermissionStatus.Denied]) e negazione permanente ([PermissionStatus.PermanentlyDenied]).
 *
 * **Nota:** deve essere usato all'interno di una [ComponentActivity]; lancia un
 * errore a runtime se il contesto non è una `ComponentActivity`.
 *
 * @param permission Permesso Android da gestire (es. `Manifest.permission.CAMERA`).
 * @param onResult Callback opzionale invocato ogni volta che lo stato del permesso cambia.
 * @return Un [PermissionHandler] pronto all'uso.
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun rememberPermission(
    permission: String,
    onResult: (status: PermissionStatus) -> Unit = {}
): PermissionHandler {
    var status by remember { mutableStateOf(PermissionStatus.Unknown) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: error("rememberPermission deve essere usato in un ComponentActivity")

    // Verifica lo stato del permesso al primo avvio del composable
    LaunchedEffect(permission) {
        status = when {
            ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED ->
                PermissionStatus.Granted

            activity.shouldShowRequestPermissionRationale(permission) ->
                PermissionStatus.Denied

            else ->
                PermissionStatus.Unknown
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        status = when {
            isGranted -> PermissionStatus.Granted
            activity.shouldShowRequestPermissionRationale(permission) -> PermissionStatus.Denied
            else -> PermissionStatus.PermanentlyDenied
        }
        onResult(status)
    }

    return remember {
        object : PermissionHandler {
            override val permission: String = permission
            override val status: PermissionStatus
                get() = status
            override fun launchPermissionRequest() = permissionLauncher.launch(permission)
        }
    }
}
