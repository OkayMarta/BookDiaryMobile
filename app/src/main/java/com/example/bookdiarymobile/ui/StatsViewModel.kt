package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * Data-клас для інкапсуляції обраного періоду (рік та місяць).
 * Використовується для керування станом у [StatsViewModel].
 *
 * @property year Вибраний рік.
 * @property month Вибраний місяць (0-11, відповідно до [Calendar]).
 */
data class SelectedPeriod(val year: Int, val month: Int)

/**
 * Представляє повний стан UI для екрану статистики ([StatsFragment]).
 * Це імутабельний data-клас, який містить всю інформацію, необхідну
 * для рендерингу екрану.
 *
 * @property totalBooks Загальна кількість прочитаних книг за весь час.
 * @property selectedPeriod Поточний вибраний період для детальної статистики.
 * @property booksInMonth Кількість книг, прочитаних у вибраному місяці та році.
 * @property booksInYear Кількість книг, прочитаних у вибраному році.
 * @property isLoading Прапорець, що вказує, чи триває завантаження даних.
 */
data class StatsUiState(
    val totalBooks: Int = 0,
    val selectedPeriod: SelectedPeriod,
    val booksInMonth: Int = 0,
    val booksInYear: Int = 0,
    val isLoading: Boolean = true
)

/**
 * ViewModel для екрану статистики ([StatsFragment]).
 *
 * Відповідає за:
 * - Керування вибраним періодом (місяць/рік).
 * - Реактивне отримання статистичних даних з [BookRepository] щоразу,
 *   коли змінюється вибраний період.
 * - Комбінування всіх даних у єдиний потік стану [StatsUiState] для UI.
 *
 * @param repository Репозиторій для доступу до даних, що впроваджується через Hilt.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(repository: BookRepository) : ViewModel() {

    /**
     * Внутрішній [MutableStateFlow], що є джерелом істини для вибраного користувачем періоду.
     * Ініціалізується поточним роком та місяцем.
     */
    private val _selectedPeriod = MutableStateFlow(
        SelectedPeriod(
            year = Calendar.getInstance().get(Calendar.YEAR),
            month = Calendar.getInstance().get(Calendar.MONTH)
        )
    )

    /**
     * Реактивний потік, що динамічно обчислює статистику для вибраного періоду.
     *
     * - `flatMapLatest`: Слухає зміни в `_selectedPeriod`. Коли період змінюється,
     *   він скасовує попередній запит до БД і запускає новий.
     * - `combine`: Виконує два запити до репозиторію одночасно (для місяця та року)
     *   і об'єднує їх результати в одну пару `(monthCount, yearCount)`.
     * - `stateIn`: Перетворює потік на "гарячий" [StateFlow], що кешує останній результат.
     */
    private val periodStats: StateFlow<Pair<Int, Int>> = _selectedPeriod.flatMapLatest { period ->
        val (monthStart, monthEnd) = calculateMonthTimestamps(period.year, period.month)
        val (yearStart, yearEnd) = calculateYearTimestamps(period.year)

        combine(
            repository.getBooksReadCountBetween(monthStart, monthEnd),
            repository.getBooksReadCountForYear(yearStart, yearEnd)
        ) { monthCount, yearCount ->
            Pair(monthCount, yearCount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Pair(0, 0))

    /**
     * Основний потік стану UI, на який підписується [StatsFragment].
     *
     * Він комбінує дані з трьох джерел:
     * 1. Загальна кількість прочитаних книг (`getTotalBooksRead`).
     * 2. Статистика за період (`periodStats`).
     * 3. Сам вибраний період (`_selectedPeriod`).
     *
     * Результатом є готовий об'єкт [StatsUiState], який містить усі дані для відображення.
     * `initialValue` встановлює `isLoading = true`, щоб UI міг показати індикатор завантаження.
     */
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
     * Публічний метод для оновлення вибраного періоду з UI (діалогу вибору дати).
     * Зміна значення `_selectedPeriod` автоматично запускає ланцюжок реактивних
     * оновлень у потоках `periodStats` та `stats`.
     *
     * @param year Новий вибраний рік.
     * @param month Новий вибраний місяць.
     */
    fun updateSelectedPeriod(year: Int, month: Int) {
        _selectedPeriod.value = SelectedPeriod(year, month)
    }

    /**
     * Допоміжна функція для обчислення початкової та кінцевої мітки часу (timestamp)
     * для заданого місяця та року.
     * @return Пара значень (початок місяця, кінець місяця) у мілісекундах.
     */
    private fun calculateMonthTimestamps(year: Int, month: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
        }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Допоміжна функція для обчислення початкової та кінцевої мітки часу (timestamp)
     * для заданого року.
     * @return Пара значень (початок року, кінець року) у мілісекундах.
     */
    private fun calculateYearTimestamps(year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            clear()
            set(year, Calendar.JANUARY, 1)
        }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}