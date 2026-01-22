package com.example.allcollections.core.navigation

/**
 * Enum che rappresenta tutte le rotte di navigazione dell'app.
 * Ogni schermata deve essere dichiarata qui per avere un'unica fonte di verità.
 *
 * Include:
 * - AUTH
 * - COLLECTION
 * - HOME / SEARCH / NOTIFICATIONS
 * - PROFILE / PUBLIC PROFILE
 * - SETTINGS / THEME
 *
 * @property route La stringa della rotta, eventualmente con parametri tra {}.
 */
enum class Screens(val route: String) {

    // ─────────── AUTH ───────────
    LoginScreen("login"),
    RegisterScreen("register"),

    // ─────────── COLLECTION ───────────
    MyCollectionsScreen("my_collections"),
    AddCollectionScreen("add_collection"),
    CollectionDetailScreen("collection_detail/{collectionId}"),
    AddCollectionImageScreen("add_image_collection/{collectionId}"),
    AddCollectionObjectScreen("add_object_collection/{collectionId}"),
    EditCollectionItemScreen("edit_collection_item/{collectionId}/{itemId}"),

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

    // ─────────── SETTINGS ───────────
    SettingsScreen("settings"),
    ChooseThemeScreen("choose_theme");

    // ─────────── GENERIC ROUTE CREATION ───────────

    /**
     * Costruisce una rotta completa sostituendo i parametri {placeholder} con gli argomenti forniti.
     * Funzione generica, funziona anche per più parametri.
     *
     * Esempio:
     * Screens.CollectionDetailScreen.createRoute("123") => "collection_detail/123"
     *
     * @param args Lista di argomenti da sostituire nei placeholder.
     * @return La rotta completa con i parametri sostituiti.
     */
    fun createRoute(vararg args: String): String {
        var result = route
        args.forEach { arg ->
            result = result.replaceFirst(Regex("\\{[^}]*\\}"), arg)
        }
        return result
    }

    // ─────────── SPECIFIC HELPERS PER ROTTE CON PARAMETRI ───────────

    fun photoProfileRoute(userId: String, isRegistration: Boolean) =
        "photo_profile/$userId/$isRegistration"

    fun publicProfileRoute(userId: String) =
        "public_profile/$userId"

    fun editCollectionItemRoute(collectionId: String, itemId: String) =
        "edit_collection_item/$collectionId/$itemId"

    fun editItemRoute(collectionId: String, itemId: String) =
        "edit_item/$collectionId/$itemId"

    fun collectionDetailRoute(collectionId: String) =
        "collection_detail/$collectionId"

    fun addCollectionImageRoute(collectionId: String) =
        "add_image_collection/$collectionId"

    fun addCollectionObjectRoute(collectionId: String) =
        "add_object_collection/$collectionId"

    fun editCollectionRoute(collectionId: String) =
        "edit_collection/$collectionId"

    fun editPhotoProfileRoute(userId: String) =
        "edit_photo_profile/$userId"
}
