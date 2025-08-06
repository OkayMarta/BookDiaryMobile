package com.example.bookdiarymobile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.BookStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel для екрану додавання/редагування книги.
 * Відповідає за завантаження даних для редагування та збереження змін у репозиторії.
 */
@HiltViewModel
class AddEditBookViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle // Hilt автоматично надасть цей об'єкт
) : ViewModel() {

    // Отримуємо аргументи з SavedStateHandle
    private val bookId: Int = savedStateHandle.get<Int>("book_id") ?: -1
    private val bookStatusForNew: BookStatus? = savedStateHandle.get<String>("book_status")?.let { BookStatus.valueOf(it) }
    private val isTransitioningToRead: Boolean = savedStateHandle.get<Boolean>("is_transitioning_to_read") ?: false

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
        rating: Int?,
        genre: String
    ) {
        viewModelScope.launch {
            if (bookId == -1) {
                // --- РЕЖИМ СТВОРЕННЯ НОВОЇ КНИГИ ---
                val newBook = Book(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description,
                    coverImagePath = newCoverPath,
                    status = bookStatusForNew ?: BookStatus.TO_READ, // Якщо статус не передано, за замовчуванням TO_READ.
                    dateAdded = System.currentTimeMillis(),
                    // Використовуємо передані значення, якщо книга одразу прочитана
                    dateRead = if (bookStatusForNew == BookStatus.READ) dateRead else null,
                    rating = if (bookStatusForNew == BookStatus.READ) rating else null,
                    isFavorite = false
                )
                repository.addBook(newBook)
            } else {
                // --- ЛОГІКА РЕДАГУВАННЯ ---
                val existingBook = uiState.value ?: return@launch

                if (newCoverPath != null && newCoverPath != existingBook.coverImagePath) {
                    existingBook.coverImagePath?.let { oldPath ->
                        try {
                            File(oldPath).delete()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                val updatedBook = existingBook.copy(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description,
                    coverImagePath = newCoverPath,
                    // Встановлюємо статус READ, якщо ми в режимі переходу,
                    // інакше залишаємо старий статус.
                    status = if (isTransitioningToRead) BookStatus.READ else existingBook.status,
                    dateRead = dateRead,
                    rating = rating
                )
                repository.updateBook(updatedBook)
            }
        }
    }

    /**
     * Перевіряє, чи є поточна книга (нова чи та, що редагується)
     * книгою зі статусом READ.
     */
    fun isCurrentBookRead(): Boolean {
        // Якщо редагуємо існуючу книгу, перевіряємо її статус
        if (bookId != -1) {
            return uiState.value?.status == BookStatus.READ
        }
        // Якщо створюємо нову, перевіряємо статус, переданий через navArgs
        return bookStatusForNew == BookStatus.READ
    }
}