package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(private val repository: BookRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_READ_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val favoriteBooks: StateFlow<List<Book>> = combine(
        _sortOrder,
        _searchQuery
    ) { sort, query ->
        Pair(sort, query)
    }.flatMapLatest { (sort, query) ->
        repository.getFilteredAndSortedBooks(ScreenType.FAVORITES, sort, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun applySortOrder(newOrder: SortOrder) {
        _sortOrder.value = newOrder
    }

    fun applySearchQuery(query: String) {
        _searchQuery.value = query
    }
}