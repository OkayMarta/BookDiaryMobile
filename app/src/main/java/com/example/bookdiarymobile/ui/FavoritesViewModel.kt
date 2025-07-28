package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel для екрану "Вибране" (Favorites).
 * Відповідає за надання списку вибраних книг для відображення в інтерфейсі.
 */
class FavoritesViewModel(repository: BookRepository) : ViewModel() {

    /**
     * StateFlow, що містить актуальний список вибраних книг.
     * Він отримує дані з репозиторію та автоматично оновлюється при будь-яких
     * змінах у базі даних (наприклад, коли книга додається до вибраного).
     */
    val favoriteBooks: StateFlow<List<Book>> = repository.favoriteBooks
        .stateIn(
            scope = viewModelScope, // Життєвий цикл ViewModel
            // Починати збирати дані, лише коли UI (фрагмент) активний
            started = SharingStarted.WhileSubscribed(5000L),
            // Початкове значення, доки дані не завантажились - порожній список
            initialValue = emptyList()
        )
}