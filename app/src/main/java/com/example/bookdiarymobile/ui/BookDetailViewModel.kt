package com.example.bookdiarymobile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.BookStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для екрану деталей книги ([BookDetailFragment]).
 *
 * Відповідає за:
 * - Отримання `book_id` з навігаційних аргументів через [SavedStateHandle].
 * - Завантаження та надання даних конкретної книги для UI через [StateFlow].
 * - Обробку дій користувача, таких як видалення книги або додавання її до улюблених.
 *
 * @param repository Репозиторій для доступу до даних книг.
 * @param savedStateHandle Об'єкт, що надається Hilt для доступу до навігаційних аргументів.
 */
@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Ідентифікатор книги, отриманий з навігаційних аргументів. */
    private val bookId: Int = savedStateHandle.get<Int>("book_id")!!

    /**
     * Надає потік [StateFlow] з даними поточної книги, на який підписується UI.
     *
     * - `repository.getBookById(bookId)`: Отримує "холодний" потік з даними книги з бази даних.
     * - `filterNotNull()`: Відфільтровує початкові `null` значення, які може випромінювати Room,
     *   поки дані не завантажено.
     * - `stateIn()`: Перетворює потік на "гарячий" [StateFlow], що кешує останнє значення.
     *   - `started = SharingStarted.WhileSubscribed(5000L)`: Оптимізація, яка тримає потік
     *     активним, лише коли є підписники (тобто UI видимий), з 5-секундним тайм-аутом,
     *     щоб пережити короткочасні зміни конфігурації (наприклад, поворот екрану).
     *   - `initialValue`: Початкове значення-заглушка, яке відображається, поки
     *     реальні дані завантажуються з бази даних. Це запобігає помилкам `NullPointerException`
     *     в UI та покращує користувацький досвід.
     */
    val book: StateFlow<Book> = repository.getBookById(bookId)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Book(
                id = -1, title = "Loading...", author = "", genre = "",
                description = "", coverImagePath = null,
                status = BookStatus.TO_READ,
                dateAdded = 0, dateRead = null, rating = null
            )
        )

    /**
     * Асинхронно видаляє поточну книгу з репозиторію.
     * Операція виконується в межах [viewModelScope].
     */
    fun deleteBook() {
        viewModelScope.launch {
            repository.deleteBook(book.value)
        }
    }

    /**
     * Асинхронно перемикає статус "Улюблене" для поточної книги.
     * Використовує імутабельний підхід: створює копію об'єкта [Book]
     * зі зміненим полем `isFavorite` і передає її для оновлення в репозиторій.
     */
    fun toggleFavoriteStatus() {
        viewModelScope.launch {
            val currentBook = book.value
            val updatedBook = currentBook.copy(
                isFavorite = !currentBook.isFavorite
            )
            repository.updateBook(updatedBook)
        }
    }
}