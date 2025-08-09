package com.example.bookdiarymobile.ui

import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для екрану "Хочу прочитати" ([ToReadFragment]).
 *
 * Цей клас наслідує всю основну логіку для отримання, сортування та пошуку
 * книг від [BaseBookListViewModel]. Його головне завдання — надати конкретні
 * значення для абстрактних властивостей базового класу, а саме:
 * - `screenType` для фільтрації лише книг, запланованих до прочитання.
 * - `defaultSortOrder` для початкового сортування списку.
 *
 * @param repository Репозиторій для доступу до даних, що впроваджується через Hilt.
 */
@HiltViewModel
class ToReadViewModel @Inject constructor(
    repository: BookRepository
) : BaseBookListViewModel(repository) {

    /**
     * Визначає тип екрану як [ScreenType.TO_READ].
     * Це значення використовується в запиті до репозиторію для отримання
     * лише тих книг, у яких статус дорівнює [com.example.bookdiarymobile.data.BookStatus.TO_READ].
     */
    override val screenType: ScreenType = ScreenType.TO_READ

    /**
     * Встановлює порядок сортування за замовчуванням для списку "Хочу прочитати".
     * Тут вибрано [SortOrder.DATE_ADDED_DESC], що означає, що книги будуть
     * відсортовані за датою їх додавання до списку від новіших до старіших.
     */
    override val defaultSortOrder: SortOrder = SortOrder.DATE_ADDED_DESC
}