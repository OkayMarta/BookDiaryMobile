package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class ReadViewModel(private val repository: BookRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_READ_DESC) // Стан сортування за замовчуванням
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    // `flatMapLatest` автоматично перемикається на новий Flow, коли змінюється _sortOrder
    val readBooks: StateFlow<List<Book>> = _sortOrder.flatMapLatest { order ->
        repository.getSortedBooks(order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun applySortOrder(newOrder: SortOrder) {
        _sortOrder.value = newOrder
    }
}