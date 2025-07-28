package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val repository: BookRepository, // Зробимо репозиторій полем класу
    bookId: Int // ViewModel одразу отримує ID книги, яку треба завантажити
) : ViewModel() {

    // Отримуємо потік з однією книгою з репозиторію за її ID
    val book: StateFlow<Book> = repository.getBookById(bookId)
        .filterNotNull() // Фільтруємо, щоб уникнути початкового null значення
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            // Початкове значення - це "заглушка", доки реальні дані не завантажаться
            initialValue = Book(
                id = -1, title = "Loading...", author = "", genre = "",
                description = "", coverImagePath = null,
                status = com.example.bookdiarymobile.data.BookStatus.READ,
                dateAdded = 0, dateRead = null, rating = null
            )
        )
    fun deleteBook() {
        viewModelScope.launch {
            // Викликаємо функцію видалення з репозиторію,
            // передаючи поточний завантажений об'єкт книги
            repository.deleteBook(book.value)
        }
    }

    /**
     * === НОВА ФУНКЦІЯ ===
     * Змінює статус "вибране" для поточної книги.
     * Запускається в корутині, щоб не блокувати головний потік.
     */
    fun toggleFavoriteStatus() {
        viewModelScope.launch {
            // Отримуємо поточний об'єкт книги з StateFlow
            val currentBook = book.value
            // Створюємо копію об'єкта, інвертуючи значення isFavorite
            val updatedBook = currentBook.copy(
                isFavorite = !currentBook.isFavorite
            )
            // Викликаємо метод репозиторію для оновлення книги в базі даних
            repository.updateBook(updatedBook)
        }
    }
}