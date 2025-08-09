package com.example.bookdiarymobile.ui

import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для екрану "Прочитано" ([ReadFragment]).
 *
 * Цей клас наслідує всю основну логіку для отримання, сортування та пошуку
 * книг від [BaseBookListViewModel]. Його головне завдання — надати конкретні
 * значення для абстрактних властивостей базового класу, а саме:
 * - `screenType` для фільтрації лише прочитаних книг.
 * - `defaultSortOrder` для початкового сортування списку.
 *
 * @param repository Репозиторій для доступу до даних, що впроваджується через Hilt.
 */
@HiltViewModel
class ReadViewModel @Inject constructor(
    repository: BookRepository
) : BaseBookListViewModel(repository) {

    /**
     * Визначає тип екрану як [ScreenType.READ].
     * Це значення використовується в запиті до репозиторію для отримання
     * лише тих книг, у яких статус дорівнює [com.example.bookdiarymobile.data.BookStatus.READ].
     */
    override val screenType: ScreenType = ScreenType.READ

    /**
     * Встановлює порядок сортування за замовчуванням для списку прочитаних книг.
     * Тут вибрано [SortOrder.DATE_READ_DESC], що означає, що книги будуть
     * відсортовані за датою прочитання від новіших до старіших.
     */
    override val defaultSortOrder: SortOrder = SortOrder.DATE_READ_DESC
}