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

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Int = savedStateHandle.get<Int>("book_id")!!

    val book: StateFlow<Book> = repository.getBookById(bookId)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Book(
                id = -1, title = "Loading...", author = "", genre = "",
                description = "", coverImagePath = null,
                status = BookStatus.TO_READ, // Змінено на TO_READ для кращої заглушки
                dateAdded = 0, dateRead = null, rating = null
            )
        )

    fun deleteBook() {
        viewModelScope.launch {
            repository.deleteBook(book.value)
        }
    }

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