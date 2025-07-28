package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.BookStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel для екрану додавання/редагування книги.
 * Відповідає за завантаження даних для редагування та збереження змін у репозиторії.
 */
class AddEditBookViewModel(
    private val repository: BookRepository,
    private val bookId: Int,                 // ID книги для редагування. -1 означає створення нової книги.
    private val bookStatusForNew: BookStatus? // Статус, який присвоюється новій книзі (READ або TO_READ).
) : ViewModel() {

    /**
     * StateFlow, що зберігає стан (дані) книги, яка редагується.
     * Фрагмент підписується на цей потік, щоб заповнити поля, коли дані завантажаться.
     */
    private val _uiState = MutableStateFlow<Book?>(null)
    val uiState = _uiState.asStateFlow()

    init {
        // Якщо bookId не дорівнює -1, це означає, що ми в режимі редагування.
        // Запускаємо асинхронне завантаження даних книги з репозиторію.
        if (bookId != -1) {
            viewModelScope.launch {
                repository.getBookById(bookId).collect { existingBook ->
                    // Коли дані книги завантажені, оновлюємо наш стан (_uiState).
                    _uiState.value = existingBook
                }
            }
        }
    }

    /**
     * Універсальна функція збереження, яка обробляє як створення нової книги,
     * так і оновлення існуючої.
     */
    fun saveBook(
        title: String,
        author: String,
        description: String,
        newCoverPath: String?, // Шлях до нової обкладинки, або null, якщо її не змінювали/немає.
        dateRead: Long?,
        rating: Int?
    ) {
        viewModelScope.launch {
            if (bookId == -1) {
                // --- РЕЖИМ СТВОРЕННЯ НОВОЇ КНИГИ ---
                val newBook = Book(
                    title = title,
                    author = author,
                    description = description,
                    coverImagePath = newCoverPath,
                    status = bookStatusForNew ?: BookStatus.TO_READ, // Якщо статус не передано, за замовчуванням TO_READ.
                    dateAdded = System.currentTimeMillis(),
                    // Використовуємо передані значення, якщо книга одразу прочитана
                    dateRead = if (bookStatusForNew == BookStatus.READ) dateRead else null,
                    rating = if (bookStatusForNew == BookStatus.READ) rating else null,
                    genre = "", // Поле для майбутнього розширення функціоналу.
                    isFavorite = false
                )
                repository.addBook(newBook)
            } else {
                // --- РЕЖИМ РЕДАГУВАННЯ ІСНУЮЧОЇ КНИГИ ---
                val existingBook = uiState.value ?: return@launch // Отримуємо поточні дані книги.

                // Перевіряємо, чи було обрано нову обкладинку.
                // Якщо так, видаляємо старий файл обкладинки, щоб не засмічувати пам'ять.
                if (newCoverPath != null && newCoverPath != existingBook.coverImagePath) {
                    existingBook.coverImagePath?.let { oldPath ->
                        try {
                            File(oldPath).delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Створюємо оновлений об'єкт книги з новими даними.
                val updatedBook = existingBook.copy(
                    title = title,
                    author = author,
                    description = description,
                    coverImagePath = newCoverPath, // Записуємо шлях до нової (або старої, якщо не змінювали) обкладинки.
                    dateRead = dateRead,
                    rating =  rating
                )
                repository.updateBook(updatedBook)
            }
        }
    }
}