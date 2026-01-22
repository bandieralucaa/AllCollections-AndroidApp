package com.example.allcollections.core.utils.permissions

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Stato di un permesso Android.
 */
enum class PermissionStatus {
    /** Stato iniziale o sconosciuto */
    Unknown,

    /** Permesso concesso */
    Granted,

    /** Permesso negato, ma l'utente può essere richiesto di nuovo */
    Denied,

    /** Permesso negato permanentemente (Non chiedere più) */
    PermanentlyDenied;

    /** Indica se il permesso è concesso */
    val isGranted: Boolean get() = this == Granted
}

/**
 * Interfaccia che gestisce un singolo permesso.
 */
interface PermissionHandler {
    val permission: String
    val status: PermissionStatus
    fun launchPermissionRequest()
}

/**
 * Composable che ricorda e gestisce lo stato di un permesso.
 *
 * @param permission Permesso Android da richiedere.
 * @param onResult Callback opzionale chiamato quando lo stato del permesso cambia.
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun rememberPermission(
    permission: String,
    onResult: (status: PermissionStatus) -> Unit = {}
): PermissionHandler {
    // Stato osservabile del permesso
    var status by remember { mutableStateOf(PermissionStatus.Unknown) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: error("rememberPermission deve essere usato in un ComponentActivity")

    // Launcher per la richiesta del permesso
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

    // Restituisce l'oggetto PermissionHandler
    return remember {
        object : PermissionHandler {
            override val permission: String = permission
            override val status: PermissionStatus
                get() = status
            override fun launchPermissionRequest() = permissionLauncher.launch(permission)
        }
    }
}
