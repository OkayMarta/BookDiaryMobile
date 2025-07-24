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
        status: BookStatus
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
                status = status,
                dateAdded = System.currentTimeMillis(), // Поточний час
                // Дата прочитання і рейтинг встановлюються тільки для статусу READ
                dateRead = if (status == BookStatus.READ) System.currentTimeMillis() else null,
                rating = if (status == BookStatus.READ) 4 else null, // Тимчасово
                isFavorite = false
            )

            // Викликаємо функцію репозиторію для додавання книги в БД
            repository.addBook(newBook)
        }
    }
}