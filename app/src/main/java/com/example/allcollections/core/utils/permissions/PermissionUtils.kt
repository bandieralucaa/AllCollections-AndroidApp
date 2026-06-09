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
 *
 * Utilizzato per tracciare l'esito della richiesta di permesso e aggiornare
 * la UI di conseguenza (es. mostrare spiegazioni, abilitare pulsanti, navigare
 * alle impostazioni).
 *
 * @see PermissionHandler.status
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
 *
 * @property permission Stringa del permesso Android (es. `Manifest.permission.CAMERA`).
 * @property status Stato corrente del permesso (aggiornato automaticamente).
 */
interface PermissionHandler {
    val permission: String
    val status: PermissionStatus
    fun launchPermissionRequest()
}

/**
 * Composable che crea e ricorda il gestore per un singolo permesso Android.
 *
 * Questa funzione gestisce l'intero ciclo di vita di una richiesta di permesso:
 * - Controlla lo stato iniziale (già concesso, già negato con rationale, mai chiesto).
 * - Mostra il prompt di sistema quando chiamato [PermissionHandler.launchPermissionRequest].
 * - Dopo la risposta dell'utente, aggiorna lo stato distinguendo tra negazione semplice
 *   ([PermissionStatus.Denied]) e negazione permanente ([PermissionStatus.PermanentlyDenied]).
 *
 * ### Utilizzo tipico
 * ```
 * val cameraPermission = rememberPermission(Manifest.permission.CAMERA)
 *
 * Button(onClick = { cameraPermission.launchPermissionRequest() }) {
 *     Text(when (cameraPermission.status) {
 *         PermissionStatus.Granted -> "Fotocamera disponibile"
 *         PermissionStatus.Denied -> "Richiedi fotocamera"
 *         PermissionStatus.PermanentlyDenied -> "Vai alle impostazioni"
 *         else -> "Verifica permesso"
 *     })
 * }
 * ```
 *
 * **Importante:** deve essere usato all'interno di una [ComponentActivity]
 * (tipicamente una `MainActivity` o `FragmentActivity`). Se il contesto non è
 * una `ComponentActivity`, la funzione lancia un'eccezione a runtime.
 *
 * @param permission Permesso Android da gestire (es. `Manifest.permission.CAMERA`).
 * @param onResult Callback opzionale invocato ogni volta che lo stato del permesso cambia.
 * @return Un [PermissionHandler] pronto all'uso.
 *
 * @see PermissionStatus
 * @see androidx.activity.compose.rememberLauncherForActivityResult
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun rememberPermission(
    permission: String,
    onResult: (status: PermissionStatus) -> Unit = {}
): PermissionHandler {
    var status by remember { mutableStateOf(PermissionStatus.Unknown) }

    val context = LocalContext.current
    // Verifica che il contesto sia una ComponentActivity (necessaria per shouldShowRequestPermissionRationale)
    val activity = context as? ComponentActivity
        ?: error("rememberPermission deve essere usato in un ComponentActivity")

    // Determina lo stato iniziale del permesso
    LaunchedEffect(permission) {
        status = when {
            // Già concesso
            ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED ->
                PermissionStatus.Granted

            // Negato ma può essere richiesto di nuovo (l'utente ha visto il rationale in passato)
            activity.shouldShowRequestPermissionRationale(permission) ->
                PermissionStatus.Denied

            // Non ancora richiesto o stato sconosciuto
            else ->
                PermissionStatus.Unknown
        }
    }

    // Launcher per la richiesta del permesso al sistema
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Aggiorna lo stato in base alla risposta dell'utente
        status = when {
            isGranted -> PermissionStatus.Granted
            // Se l'utente ha negato ma è ancora possibile mostrare rationale → denied temporaneo
            activity.shouldShowRequestPermissionRationale(permission) -> PermissionStatus.Denied
            // Altrimenti l'utente ha spuntato "Non chiedere più" → negato permanentemente
            else -> PermissionStatus.PermanentlyDenied
        }
        onResult(status)
    }

    // Restituisce l'handler memorizzato
    return remember {
        object : PermissionHandler {
            override val permission: String = permission
            override val status: PermissionStatus
                get() = status
            override fun launchPermissionRequest() = permissionLauncher.launch(permission)
        }
    }
}