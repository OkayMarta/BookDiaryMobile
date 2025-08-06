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

/**
 * Абстрактна базова ViewModel для екранів, що відображають списки книг.
 * Містить спільну логіку для сортування та пошуку.
 */
abstract class BaseBookListViewModel(private val repository: BookRepository) : ViewModel() {

    protected abstract val screenType: ScreenType
    protected abstract val defaultSortOrder: SortOrder

    // --- Використовуємо by lazy ---
    // Це відкладає ініціалізацію до першого звернення, коли `defaultSortOrder` вже буде доступний.
    private val _sortOrder by lazy { MutableStateFlow(defaultSortOrder) }
    val sortOrder: StateFlow<SortOrder> by lazy { _sortOrder }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // --- Також робимо цей потік "лінивим" ---
    // оскільки він залежить від `_sortOrder`, який тепер теж "лінивий".
    val books: StateFlow<List<Book>> by lazy {
        combine(
            _sortOrder,
            _searchQuery
        ) { sort, query ->
            Pair(sort, query)
        }.flatMapLatest { (sort, query) ->
            repository.getFilteredAndSortedBooks(screenType, sort, query)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
    }

    /**
     * Оновлює поточний порядок сортування.
     */
    fun applySortOrder(newOrder: SortOrder) {
        _sortOrder.value = newOrder
    }

    /**
     * Оновлює поточний пошуковий запит.
     */
    fun applySearchQuery(query: String) {
        _searchQuery.value = query
    }
}