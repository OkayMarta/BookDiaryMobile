package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.BookStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 1. Конструктор тепер приймає три параметри
class AddEditBookViewModel(
    private val repository: BookRepository,
    private val bookId: Int, // ID книги для редагування. -1, якщо це нова книга.
    private val bookStatusForNew: BookStatus? // Статус, який треба присвоїти НОВІЙ книзі.
) : ViewModel() {

    // 2. Створюємо MutableStateFlow для зберігання стану книги, яку редагуємо.
    // Це дозволяє фрагменту підписатися на зміни і заповнити поля, коли дані завантажаться.
    private val _uiState = MutableStateFlow<Book?>(null)
    val uiState = _uiState.asStateFlow()

    init {
        // 3. Якщо bookId не -1, це означає режим редагування.
        // Запускаємо завантаження даних книги.
        if (bookId != -1) {
            viewModelScope.launch {
                repository.getBookById(bookId).collect { existingBook ->
                    // Коли книга завантажиться, оновлюємо наш стан.
                    _uiState.value = existingBook
                }
            }
        }
    }

    // 4. Універсальна функція збереження, яка працює для обох режимів.
    fun saveBook(
        title: String,
        author: String,
        description: String,
        coverPath: String?
    ) {
        viewModelScope.launch {
            if (bookId == -1) {
                // --- РЕЖИМ СТВОРЕННЯ ---
                val newBook = Book(
                    title = title,
                    author = author,
                    description = description,
                    coverImagePath = coverPath,
                    status = bookStatusForNew ?: BookStatus.TO_READ, // Якщо статус не передали, то за замовчуванням це TO_READ
                    dateAdded = System.currentTimeMillis(),
                    dateRead = if (bookStatusForNew == BookStatus.READ) System.currentTimeMillis() else null,
                    rating = if (bookStatusForNew == BookStatus.READ) 3 else null, // тимчасове значення
                    genre = "",
                    isFavorite = false
                )
                repository.addBook(newBook)
            } else {
                /// РЕЖИМ РЕДАГУВАННЯ
                val existingBook = uiState.value ?: return@launch
                val updatedBook = existingBook.copy(
                    title = title,
                    author = author,
                    description = description,
                    coverImagePath = coverPath // <-- ВИКОРИСТОВУЄМО
                )
                repository.updateBook(updatedBook)
            }
        }
    }
}