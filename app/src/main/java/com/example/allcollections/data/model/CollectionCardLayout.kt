package com.example.allcollections.data.model

/**
 * Layout disponibili per la visualizzazione delle card delle collezioni.
 *
 * Questi layout permettono all'utente di scegliere come visualizzare
 * le proprie collezioni nella schermata principale o in altre griglie.
 *
 * @property Horizontal Card con immagine e testo affiancati (layout orizzontale).
 * @property Vertical Card con immagine sopra e testo sotto (layout verticale).
 */
enum class CollectionCardLayout {

    /** Card con immagine e testo affiancati (layout orizzontale). */
    Horizontal,

    /** Card con immagine sopra e testo sotto (layout verticale). */
    Vertical
}