package com.example.bookdiarymobile.data

import java.io.Serializable

enum class SortOrder : Serializable {
    // Для всіх списків
    TITLE_ASC,
    TITLE_DESC,

    // Тільки для Read/Favorites
    DATE_READ_ASC,
    DATE_READ_DESC,
    RATING_ASC,
    RATING_DESC,

    // Тільки для ToRead
    DATE_ADDED_ASC,
    DATE_ADDED_DESC
}