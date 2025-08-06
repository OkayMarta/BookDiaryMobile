package com.example.bookdiarymobile.ui

import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder

/**
 * ViewModel для екрану "To Read".
 * Успадковує всю логіку сортування та пошуку від BaseBookListViewModel.
 */
class ToReadViewModel(repository: BookRepository) : BaseBookListViewModel(repository) {
    // Визначаємо конкретні параметри для цього екрана
    override val screenType: ScreenType = ScreenType.TO_READ
    override val defaultSortOrder: SortOrder = SortOrder.DATE_ADDED_DESC
}