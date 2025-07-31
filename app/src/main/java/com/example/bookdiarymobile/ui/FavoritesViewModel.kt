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

class FavoritesViewModel(private val repository: BookRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_READ_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    val favoriteBooks: StateFlow<List<Book>> = _sortOrder.flatMapLatest { order ->
        repository.getSortedFavoriteBooks(order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun applySortOrder(newOrder: SortOrder) {
        _sortOrder.value = newOrder
    }
}