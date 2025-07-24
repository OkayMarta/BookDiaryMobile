package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.BookStatus
import kotlinx.coroutines.launch

class AddEditBookViewModel(private val repository: BookRepository) : ViewModel() {

    // Функція, яку буде викликати наш фрагмент для збереження книги
    fun saveBook(
        title: String,
        author: String,
        description: String,
        // Пізніше ми додамо сюди більше параметрів
    ) {
        // Запускаємо корутину в життєвому циклі ViewModel
        viewModelScope.launch {
            // Створюємо новий об'єкт Book
            val newBook = Book(
                // id генерується автоматично, тому ми його не вказуємо
                title = title,
                author = author,
                description = description,
                genre = "", // Поки що жанр порожній
                coverImagePath = null,
                // TODO: Потрібно визначити, з якого екрану ми прийшли,
                // щоб встановити правильний статус (READ або TO_READ)
                status = BookStatus.READ, // Тимчасово ставимо READ
                dateAdded = System.currentTimeMillis(), // Поточний час
                dateRead = System.currentTimeMillis(), // Тимчасово ставимо поточний час
                rating = 4, // Тимчасово ставимо 4
                isFavorite = false
            )

            // Викликаємо функцію репозиторію для додавання книги в БД
            repository.addBook(newBook)
        }
    }
}