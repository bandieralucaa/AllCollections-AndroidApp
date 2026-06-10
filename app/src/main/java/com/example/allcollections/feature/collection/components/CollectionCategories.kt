package com.example.allcollections.feature.collection.components

/**
 * Lista predefinita delle categorie disponibili per una collezione.
 *
 * Utilizzata nei form di creazione e modifica collezione per mostrare
 * un selettore (es. FlowRow di FilterChip) con categorie comuni.
 *
 * L'ultima voce "Altro ✏️" permette all'utente di inserire una categoria
 * personalizzata, attivando un campo di testo dedicato.
 *
 * Le categorie coprono un'ampia gamma di interessi: collezionismo,
 * memorabilia, tecnologia vintage, modellismo, ecc.
 *
 * @see AddCollectionScreen
 * @see EditCollectionScreen
 */
val PRESET_CATEGORIES = listOf(
    "Action Figure",
    "Fumetti & Manga",
    "Giochi & Videogiochi",
    "Libri & Riviste",
    "Modellismo",
    "Monete & Banconote",
    "Musica (Vinili, CD)",
    "Memorabilia",
    "Cartoline & Francobolli",
    "Altro ✏️"
)