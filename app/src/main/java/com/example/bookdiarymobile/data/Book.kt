package com.example.bookdiarymobile.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Клас-сутність (Entity), що представляє книгу в базі даних Room.
 *
 * Кожен екземпляр цього класу відповідає одному рядку в таблиці "books".
 *
 * @property id Унікальний ідентифікатор книги, генерується автоматично базою даних.
 * @property title Назва книги.
 * @property author Автор книги.
 * @property genre Жанр книги.
 * @property description Опис або особисті нотатки про книгу.
 * @property coverImagePath Шлях до файлу обкладинки у внутрішньому сховищі додатку. Може бути `null`, якщо обкладинка не додана.
 * @property status Статус книги, що визначається переліченням [BookStatus] (наприклад, READ, TO_READ).
 * @property dateAdded Дата додавання книги у щоденник, збережена як мітка часу (timestamp).
 * @property dateRead Дата завершення читання книги, збережена як мітка часу. Може бути `null` для книг, які ще не прочитані.
 * @property rating Особистий рейтинг книги (наприклад, від 1 до 5). Може бути `null`, якщо рейтинг не встановлено.
 * @property isFavorite Прапорець, що вказує, чи додана книга до списку "Улюблених".
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val author: String,
    val genre: String,
    val description: String,

    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String?,

    val status: BookStatus,

    @ColumnInfo(name = "date_added")
    val dateAdded: Long,

    @ColumnInfo(name = "date_read")
    val dateRead: Long?,

    val rating: Int?,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)