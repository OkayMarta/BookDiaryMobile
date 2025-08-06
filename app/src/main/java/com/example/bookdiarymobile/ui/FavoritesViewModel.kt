package com.example.bookdiarymobile.ui

import com.example.bookdiarymobile.data.BookRepository
import com.example.bookdiarymobile.data.ScreenType
import com.example.bookdiarymobile.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для екрану "Favorites".
 * Успадковує всю логіку сортування та пошуку від BaseBookListViewModel.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: BookRepository
) : BaseBookListViewModel(repository) {
    override val screenType: ScreenType = ScreenType.FAVORITES
    override val defaultSortOrder: SortOrder = SortOrder.DATE_READ_DESC
}