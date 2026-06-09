package com.example.allcollections.core.navigation

/**
 * Enum che rappresenta tutte le rotte di navigazione dell'app.
 *
 * Funge da **unica fonte di verità** per i percorsi di navigazione,
 * evitando stringhe hardcodate sparse nel codice.
 *
 * Le rotte con parametri usano i placeholder `{nomeparametro}` secondo la
 * convenzione di Navigation Compose. I placeholder vengono poi sostituiti
 * con i valori reali tramite [createRoute] o i metodi helper specifici.
 *
 * @property route Stringa della rotta, con eventuali parametri tra `{}`.
 *
 * @see NavGraphBuilder.composable
 * @see NavController.navigate
 */
enum class Screens(val route: String) {

    // ─────────── AUTH ───────────
    /** Schermata di login. */
    LoginScreen("login"),
    /** Schermata di registrazione nuovo utente. */
    RegisterScreen("register"),
    /** Schermata di recupero password dimenticata. */
    ForgotPasswordScreen("forgot_password"),
    /** Schermata di verifica email post‑registrazione. */
    VerifyEmailScreen("verify_email"),

    // ─────────── COLLECTION ───────────
    /** Lista delle collezioni dell'utente corrente. */
    MyCollectionsScreen("my_collections"),
    /** Schermata per creare una nuova collezione (nome, categoria, descrizione). */
    AddCollectionScreen("add_collection"),
    /**
     * Dettaglio di una collezione.
     * Parametri: `collectionId` (obbligatorio), `itemId` (opzionale, per scroll a un oggetto).
     */
    CollectionDetailScreen("collection_detail/{collectionId}?itemId={itemId}"),
    /** Schermata per caricare l'immagine di copertina di una collezione. */
    AddCollectionImageScreen("add_image_collection/{collectionId}"),
    /** Schermata per aggiungere un nuovo oggetto a una collezione. */
    AddCollectionObjectScreen("add_object_collection/{collectionId}"),
    /** Schermata per modificare un oggetto esistente. */
    EditCollectionItemScreen("edit_collection_item/{collectionId}/{itemId}"),
    /** Schermata per modificare i metadati di una collezione (nome, categoria, descrizione). */
    EditCollectionScreen("edit_collection/{collectionId}"),

    // ─────────── HOME / SEARCH / NOTIFICATIONS ───────────
    /** Feed principale con collezioni in evidenza. */
    HomeScreen("home"),
    /** Schermata di ricerca utenti e collezioni. */
    SearchScreen("search"),
    /** Schermata delle notifiche push. */
    NotificationsScreen("notifications"),

    // ─────────── PROFILE ───────────
    /** Profilo personale dell'utente corrente. */
    ProfileScreen("profile"),
    /** Profilo pubblico di un altro utente. Parametro: `userId`. */
    PublicProfileScreen("public_profile/{userId}"),
    /**
     * Schermata per impostare la foto profilo.
     * Parametri: `userId`, `isRegistration` (booleano, usato per il comportamento di back stack).
     */
    PhotoProfileScreen("photo_profile/{userId}/{isRegistration}"),
    /** Schermata per modificare i dati anagrafici (nome, cognome, username, etc.). */
    EditProfileScreen("edit_profile"),
    /** Schermata per cambiare password. */
    EditPasswordScreen("edit_password"),
    /** Schermata per modificare la biografia. */
    EditBioScreen("edit_bio"),

    // ─────────── CHAT ───────────
    /** Lista delle conversazioni recenti. */
    ChatsListScreen("chats"),
    /** Schermata di chat con un singolo utente. Parametro: `userId` del destinatario. */
    ChatScreen("chat/{userId}"),

    // ─────────── SETTINGS ───────────
    /** Schermata principale delle impostazioni. */
    SettingsScreen("settings"),
    /** Schermata per la scelta del tema (chiaro, scuro, sistema). */
    ChooseThemeScreen("choose_theme");

    // ─────────────────────────────────────────────────────────────────────────
    // Helper generici
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Costruisce la rotta completa sostituendo in ordine i placeholder `{...}`
     * con gli argomenti forniti.
     *
     * **Attenzione:** i placeholder vengono sostituiti nell'ordine in cui
     * compaiono nella stringa [route]. Assicurati che il numero e l'ordine
     * degli argomenti corrisponda esattamente ai placeholder presenti.
     *
     * ### Esempio
     * ```kotlin
     * // route = "collection_detail/{collectionId}?itemId={itemId}"
     * Screens.CollectionDetailScreen.createRoute("abc123", "item456")
     * // → "collection_detail/abc123?itemId=item456"
     * ```
     *
     * @param args Argomenti da sostituire ai placeholder, nell'ordine di comparsa.
     * @return La rotta con tutti i placeholder sostituiti.
     */
    fun createRoute(vararg args: String): String {
        var result = route
        args.forEach { arg ->
            result = result.replaceFirst(Regex("\\{[^}]*\\}"), arg)
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper specifici per rotte con parametri
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Costruisce la rotta per [PhotoProfileScreen] con i parametri richiesti.
     *
     * @param userId ID dell'utente (passato al ViewModel per salvare la foto).
     * @param isRegistration `true` se proviene dal flusso di registrazione
     *                       (la pressione del back button riporta al login invece che al profilo).
     * @return Rotta completa, es. `"photo_profile/abc123/true"`.
     */
    fun photoProfileRoute(userId: String, isRegistration: Boolean) =
        "photo_profile/$userId/$isRegistration"

    /**
     * Costruisce la rotta per [EditCollectionItemScreen] con i parametri richiesti.
     *
     * @param collectionId ID della collezione contenente l'oggetto.
     * @param itemId ID dell'oggetto da modificare.
     * @return Rotta completa, es. `"edit_collection_item/col456/item789"`.
     */
    fun editCollectionItemRoute(collectionId: String, itemId: String) =
        "edit_collection_item/$collectionId/$itemId"

    /**
     * Costruisce la rotta per [CollectionDetailScreen] senza scroll a un oggetto specifico.
     *
     * @param collectionId ID della collezione da visualizzare.
     * @return Rotta completa, es. `"collection_detail/abc123"`.
     */
    fun collectionDetailRoute(collectionId: String) =
        "collection_detail/$collectionId"

    /**
     * Costruisce la rotta per [AddCollectionObjectScreen] (aggiunta di un nuovo oggetto).
     *
     * @param collectionId ID della collezione a cui aggiungere l'oggetto.
     * @return Rotta completa, es. `"add_object_collection/col456"`.
     */
    fun addCollectionObjectRoute(collectionId: String) =
        "add_object_collection/$collectionId"

    /**
     * Costruisce la rotta per [EditCollectionScreen] (modifica dati collezione).
     *
     * @param collectionId ID della collezione da modificare.
     * @return Rotta completa, es. `"edit_collection/col456"`.
     */
    fun editCollectionRoute(collectionId: String) =
        "edit_collection/$collectionId"

    /**
     * Costruisce la rotta per [CollectionDetailScreen] con scroll automatico a un oggetto specifico.
     *
     * Utilizzata quando si naviga da una notifica di commento su un oggetto.
     * Il parametro `itemId` viene passato come query parameter.
     *
     * @param collectionId ID della collezione.
     * @param itemId ID dell'oggetto a cui scorrere automaticamente.
     * @return Rotta completa, es. `"collection_detail/abc123?itemId=item456"`.
     */
    fun collectionDetailWithItemRoute(collectionId: String, itemId: String) =
        "collection_detail/$collectionId?itemId=$itemId"
}