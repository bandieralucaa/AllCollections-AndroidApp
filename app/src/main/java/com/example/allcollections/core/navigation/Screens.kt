package com.example.allcollections.core.navigation

/**
 * Enum che rappresenta tutte le rotte di navigazione dell'app.
 *
 * Funge da **unica fonte di verità** per i percorsi di navigazione,
 * evitando stringhe hardcodate sparse nel codice. Le rotte con parametri
 * usano i placeholder `{nomeparametro}` secondo la convenzione di Navigation Compose.
 *
 * @property route Stringa della rotta, con eventuali parametri tra `{}`.
 */
enum class Screens(val route: String) {

    // ─────────── AUTH ───────────
    LoginScreen("login"),
    RegisterScreen("register"),
    ForgotPasswordScreen("forgot_password"),
    VerifyEmailScreen("verify_email"),

    // ─────────── COLLECTION ───────────
    MyCollectionsScreen("my_collections"),
    AddCollectionScreen("add_collection"),
    CollectionDetailScreen("collection_detail/{collectionId}?itemId={itemId}"),
    AddCollectionImageScreen("add_image_collection/{collectionId}"),
    AddCollectionObjectScreen("add_object_collection/{collectionId}"),
    EditCollectionItemScreen("edit_collection_item/{collectionId}/{itemId}"),
    EditCollectionScreen("edit_collection/{collectionId}"),

    // ─────────── HOME / SEARCH / NOTIFICATIONS ───────────
    HomeScreen("home"),
    SearchScreen("search"),
    NotificationsScreen("notifications"),

    // ─────────── PROFILE ───────────
    ProfileScreen("profile"),
    PublicProfileScreen("public_profile/{userId}"),
    PhotoProfileScreen("photo_profile/{userId}/{isRegistration}"),
    EditProfileScreen("edit_profile"),
    EditPasswordScreen("edit_password"),
    EditBioScreen("edit_bio"),

    // ─────────── CHAT ───────────
    ChatsListScreen("chats"),
    ChatScreen("chat/{userId}"),

    // ─────────── SETTINGS ───────────
    SettingsScreen("settings"),
    ChooseThemeScreen("choose_theme");

    // ─────────────────────────────────────────────────────────────────────────
    // Helper generici
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Costruisce la rotta completa sostituendo in ordine i placeholder `{...}`
     * con gli argomenti forniti.
     *
     * Esempio:
     * ```kotlin
     * Screens.CollectionDetailScreen.createRoute("abc123") // → "collection_detail/abc123"
     * ```
     *
     * @param args Argomenti da sostituire ai placeholder, nell'ordine in cui compaiono nella rotta.
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
     * @param userId ID dell'utente.
     * @param isRegistration `true` se si proviene dal flusso di registrazione.
     */
    fun photoProfileRoute(userId: String, isRegistration: Boolean) =
        "photo_profile/$userId/$isRegistration"

    /**
     * Costruisce la rotta per [EditCollectionItemScreen] con i parametri richiesti.
     *
     * @param collectionId ID della collezione contenente l'oggetto.
     * @param itemId ID dell'oggetto da modificare.
     */
    fun editCollectionItemRoute(collectionId: String, itemId: String) =
        "edit_collection_item/$collectionId/$itemId"

    /**
     * Costruisce la rotta per [CollectionDetailScreen] senza scroll a un oggetto specifico.
     *
     * @param collectionId ID della collezione da visualizzare.
     */
    fun collectionDetailRoute(collectionId: String) =
        "collection_detail/$collectionId"

    /**
     * Costruisce la rotta per [AddCollectionObjectScreen].
     *
     * @param collectionId ID della collezione a cui aggiungere l'oggetto.
     */
    fun addCollectionObjectRoute(collectionId: String) =
        "add_object_collection/$collectionId"

    /**
     * Costruisce la rotta per [EditCollectionScreen].
     *
     * @param collectionId ID della collezione da modificare.
     */
    fun editCollectionRoute(collectionId: String) =
        "edit_collection/$collectionId"

    /**
     * Costruisce la rotta per [CollectionDetailScreen] con scroll diretto a un oggetto.
     *
     * Usata quando si naviga da una notifica di commento su un oggetto specifico.
     *
     * @param collectionId ID della collezione.
     * @param itemId ID dell'oggetto a cui scorrere automaticamente.
     */
    fun collectionDetailWithItemRoute(collectionId: String, itemId: String) =
        "collection_detail/$collectionId?itemId=$itemId"
}
