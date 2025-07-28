package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

// Data-клас для зберігання обраного періоду
data class SelectedPeriod(val year: Int, val month: Int)

// Новий, спрощений Data-клас для стану UI
data class StatsUiState(
    val totalBooks: Int = 0,
    val selectedPeriod: SelectedPeriod,
    val booksInMonth: Int = 0,
    val booksInYear: Int = 0,
    val isLoading: Boolean = true
)

class StatsViewModel(repository: BookRepository) : ViewModel() {

    // Потік, що зберігає обраний користувачем період (рік та місяць).
    // Ініціалізуємо його поточним місяцем та роком.
    private val _selectedPeriod = MutableStateFlow(
        SelectedPeriod(
            year = Calendar.getInstance().get(Calendar.YEAR),
            month = Calendar.getInstance().get(Calendar.MONTH)
        )
    )

    // Потік, що динамічно обчислює статистику на основі _selectedPeriod
    private val periodStats: StateFlow<Pair<Int, Int>> = _selectedPeriod.flatMapLatest { period ->
        // Обчислюємо часові рамки для місяця
        val (monthStart, monthEnd) = calculateMonthTimestamps(period.year, period.month)
        // Обчислюємо часові рамки для року
        val (yearStart, yearEnd) = calculateYearTimestamps(period.year)

        // Комбінуємо два запити до бази даних
        combine(
            repository.getBooksReadCountBetween(monthStart, monthEnd),
            repository.getBooksReadCountForYear(yearStart, yearEnd)
        ) { monthCount, yearCount ->
            Pair(monthCount, yearCount) // Повертаємо пару значень (книг за місяць, книг за рік)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Pair(0, 0))

    // Фінальний потік стану UI, який комбінує всі дані
    val stats: StateFlow<StatsUiState> = combine(
        repository.getTotalBooksRead(),
        periodStats,
        _selectedPeriod
    ) { total, periodCounts, period ->
        StatsUiState(
            totalBooks = total,
            selectedPeriod = period,
            booksInMonth = periodCounts.first,
            booksInYear = periodCounts.second,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StatsUiState(isLoading = true, selectedPeriod = _selectedPeriod.value)
    )

    /**
     * Публічний метод для оновлення обраного періоду з фрагмента.
     */
    fun updateSelectedPeriod(year: Int, month: Int) {
        _selectedPeriod.value = SelectedPeriod(year, month)
    }

    // --- Допоміжні функції для обчислення timestamp ---
    private fun calculateMonthTimestamps(year: Int, month: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
        }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    private fun calculateYearTimestamps(year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            clear()
            set(year, 0, 1) // 0 - це січень
        }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}