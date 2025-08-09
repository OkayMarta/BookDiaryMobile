package com.example.bookdiarymobile.data

import java.io.Serializable

/**
 * Перелічення (Enum), що визначає доступні варіанти сортування для списків книг.
 *
 * Реалізує інтерфейс [Serializable], що дозволяє передавати об'єкти цього типу
 * як аргументи між фрагментами, наприклад, при виборі опції сортування на
 * екрані `SortOptionsFragment`.
 *
 * Використовується у [BookRepository] та відповідних ViewModel для побудови
 * динамічних SQL-запитів. Деякі опції сортування доступні лише для певних
 * типів списків (наприклад, сортування за рейтингом недоступне для списку "Хочу прочитати").
 */
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