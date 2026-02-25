package com.example.allcollections.feature.notification.domain

enum class NotificationType(val value: String) {
    FOLLOW("follow"),
    COMMENT("comment"),
    ITEM_COMMENT("item_comment"),
    LIKE("like"),
    NEW_ITEM("new_item"),
    GENERAL("general");

    companion object {
        fun fromString(value: String?): NotificationType {
            return entries.find { it.value == value } ?: GENERAL
        }
    }
}