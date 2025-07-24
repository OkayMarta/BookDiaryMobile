package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReadViewModel(repository: BookRepository) : ViewModel() {

    // Отримуємо потік (Flow) з прочитаними книгами з репозиторію
    // і перетворюємо його на StateFlow, щоб UI міг на нього підписатися.
    val readBooks: StateFlow<List<Book>> = repository.allReadBooks
        .stateIn(
            scope = viewModelScope, // Життєвий цикл ViewModel
            started = SharingStarted.WhileSubscribed(5000L), // Починати збирати дані, коли є підписник
            initialValue = emptyList() // Початкове значення - порожній список
        )
}