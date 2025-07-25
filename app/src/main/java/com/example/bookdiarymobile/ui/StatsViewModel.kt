package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

// Data-клас для зручного зберігання всієї статистики
data class StatsUiState(
    val totalBooks: Int = 0,
    val booksThisYear: Int = 0,
    val booksThisMonth: Int = 0
)

class StatsViewModel(repository: BookRepository) : ViewModel() {

    // Обчислюємо timestamp для початку поточного року
    private val startOfYear: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    // Обчислюємо timestamp для початку поточного місяця
    private val startOfMonth: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    // Використовуємо combine, щоб об'єднати результати трьох різних Flow в один
    val stats: StateFlow<StatsUiState> = combine(
        repository.getTotalBooksRead(),
        repository.getBooksReadCountSince(startOfYear),
        repository.getBooksReadCountSince(startOfMonth)
    ) { total, year, month ->
        // Коли будь-який з трьох потоків видає нове значення,
        // цей блок виконується і створює новий об'єкт StatsUiState
        StatsUiState(
            totalBooks = total,
            booksThisYear = year,
            booksThisMonth = month
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StatsUiState() // Початкове значення (всі нулі)
    )
}