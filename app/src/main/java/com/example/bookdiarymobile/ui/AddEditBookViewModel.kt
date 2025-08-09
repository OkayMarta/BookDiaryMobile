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
 * ViewModel для екрану додавання/редагування книги ([AddEditBookFragment]).
 *
 * Відповідає за:
 * - Отримання навігаційних аргументів (`book_id`, `book_status`) через [SavedStateHandle].
 * - Завантаження даних існуючої книги для режиму редагування.
 * - Збереження нової або оновленої книги в [BookRepository].
 * - Керування станом UI через [StateFlow].
 *
 * @param repository Репозиторій для доступу до даних книг.
 * @param savedStateHandle Об'єкт, що надається Hilt для доступу до навігаційних аргументів
 *                         та збереження стану ViewModel.
 */
@HiltViewModel
class AddEditBookViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Ідентифікатор книги для редагування. -1, якщо створюється нова книга. */
    private val bookId: Int = savedStateHandle.get<Int>("book_id") ?: -1

    /** Статус для нової книги, переданий з попереднього екрану. */
    private val bookStatusForNew: BookStatus? = savedStateHandle.get<String>("book_status")?.let { BookStatus.valueOf(it) }

    /** Прапорець, що вказує, чи книга переноситься зі статусу "Хочу прочитати" у "Прочитано". */
    private val isTransitioningToRead: Boolean = savedStateHandle.get<Boolean>("is_transitioning_to_read") ?: false

    /**
     * Внутрішній, мутабельний [MutableStateFlow], що зберігає дані книги, яка редагується.
     * `null` означає, що створюється нова книга або дані ще не завантажені.
     */
    private val _uiState = MutableStateFlow<Book?>(null)

    /**
     * Публічний, незмінний [StateFlow], на який підписується UI для отримання
     * та відображення даних книги.
     */
    val uiState = _uiState.asStateFlow()

    init {
        // Якщо `bookId` не -1, ми перебуваємо в режимі редагування.
        // Запускаємо асинхронне завантаження даних книги з репозиторію.
        if (bookId != -1) {
            viewModelScope.launch {
                repository.getBookById(bookId).collect { existingBook ->
                    _uiState.value = existingBook
                }
            }
        }
    }

    /**
     * Універсальна функція для збереження книги. Вона обробляє як створення
     * нової книги, так і оновлення існуючої.
     *
     * @param title Назва книги.
     * @param author Автор книги.
     * @param description Опис книги.
     * @param newCoverPath Шлях до нового файлу обкладинки або `null`, якщо обкладинка не змінювалась.
     * @param dateRead Дата прочитання у форматі timestamp.
     * @param rating Рейтинг книги.
     * @param genre Жанр книги.
     */
    fun saveBook(
        title: String,
        author: String,
        description: String,
        newCoverPath: String?,
        dateRead: Long?,
        rating: Int?,
        genre: String
    ) {
        viewModelScope.launch {
            if (bookId == -1) {
                // Створення нової книги
                val newBook = Book(
                    title = title,
                    author = author,
                    genre = genre,
                    description = description,
                    coverImagePath = newCoverPath,
                    status = bookStatusForNew ?: BookStatus.TO_READ,
                    dateAdded = System.currentTimeMillis(),
                    dateRead = if (bookStatusForNew == BookStatus.READ) dateRead else null,
                    rating = if (bookStatusForNew == BookStatus.READ) rating else null,
                    isFavorite = false
                )
                repository.addBook(newBook)
            } else {
                // Оновлення існуючої книги
                val existingBook = uiState.value ?: return@launch

                // Якщо обкладинку змінено, видаляємо старий файл, щоб уникнути "сміття"
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
                    status = if (isTransitioningToRead) BookStatus.READ else existingBook.status,
                    dateRead = dateRead,
                    rating = rating
                )
                repository.updateBook(updatedBook)
            }
        }
    }

    /**
     * Допоміжна функція, яка перевіряє, чи повинна поточна книга
     * (нова або та, що редагується) відображатися з полями для статусу "Прочитано".
     *
     * @return `true`, якщо статус книги `READ`, інакше `false`.
     */
    fun isCurrentBookRead(): Boolean {
        // Для існуючої книги перевіряємо її поточний статус.
        if (bookId != -1) {
            return uiState.value?.status == BookStatus.READ
        }
        // Для нової книги перевіряємо статус, переданий через навігаційні аргументи.
        return bookStatusForNew == BookStatus.READ
    }
}