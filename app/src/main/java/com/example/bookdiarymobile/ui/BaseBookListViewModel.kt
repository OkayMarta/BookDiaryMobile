package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Абстрактна базова ViewModel для екранів, що відображають списки книг (наприклад, [ReadFragment], [ToReadFragment]).
 *
 * Інкапсулює спільну логіку для керування станом сортування та повнотекстового пошуку,
 * що дозволяє уникнути дублювання коду в дочірніх класах ViewModel.
 *
 * @property repository Репозиторій для доступу до даних.
 */
abstract class BaseBookListViewModel(
    private val repository: BookRepository
) : ViewModel() {

    /**
     * Абстрактна властивість, яку дочірній клас має перевизначити, щоб вказати,
     * який тип екрану він представляє ([ScreenType.READ], [ScreenType.TO_READ] або [ScreenType.FAVORITES]).
     * Це значення використовується для фільтрації книг у репозиторії.
     */
    protected abstract val screenType: ScreenType

    /**
     * Абстрактна властивість для визначення порядку сортування за замовчуванням
     * для конкретного списку.
     */
    protected abstract val defaultSortOrder: SortOrder

    /**
     * Внутрішній, мутабельний потік стану для поточного порядку сортування.
     * Ініціалізується ліниво (`by lazy`) значенням `defaultSortOrder`.
     */
    private val _sortOrder by lazy { MutableStateFlow(defaultSortOrder) }
    /**
     * Публічний, незмінний потік стану для порядку сортування, на який може підписатися UI.
     */
    val sortOrder: StateFlow<SortOrder> by lazy { _sortOrder }

    /**
     * Внутрішній, мутабельний потік стану для поточного пошукового запиту.
     */
    private val _searchQuery = MutableStateFlow("")
    /**
     * Публічний, незмінний потік стану для пошукового запиту.
     */
    val searchQuery: StateFlow<String> = _searchQuery

    /**
     * Основний реактивний потік, що надає відфільтрований та відсортований список книг для UI.
     *
     * Його логіка побудована на комбінації декількох потоків:
     * 1. `combine` - об'єднує останні значення з `_sortOrder` та `_searchQuery`.
     *    Він випромінює нову пару значень щоразу, коли змінюється або сортування, або пошуковий запит.
     * 2. `flatMapLatest` - отримує пару (sort, query) і запускає новий запит до
     *    `repository.getFilteredAndSortedBooks`. "Latest" означає, що якщо надійде
     *    новий запит до того, як попередній завершився, попередній буде скасовано.
     *    Це дуже ефективно при швидкому введенні тексту в полі пошуку.
     * 3. `stateIn` - перетворює "холодний" потік на "гарячий" [StateFlow], що кешує
     *    останнє значення.
     *    - `SharingStarted.WhileSubscribed(5000L)`: потік даних з БД активний, лише
     *      коли є хоча б один підписник (тобто коли UI видимий). 5000L - це час,
     *      протягом якого потік залишається активним після зникнення останнього підписника.
     *      Це запобігає повторним запитам при швидкій зміні конфігурації (напр., повороті екрану).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
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
     * Зміна значення `_sortOrder` автоматично запустить оновлення потоку `books`.
     *
     * @param newOrder Новий порядок сортування.
     */
    fun applySortOrder(newOrder: SortOrder) {
        _sortOrder.value = newOrder
    }

    /**
     * Оновлює поточний пошуковий запит.
     * Зміна значення `_searchQuery` автоматично запустить оновлення потоку `books`.
     *
     * @param query Новий пошуковий запит.
     */
    fun applySearchQuery(query: String) {
        _searchQuery.value = query
    }
}