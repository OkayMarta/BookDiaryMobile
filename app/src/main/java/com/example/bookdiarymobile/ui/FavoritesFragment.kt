package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentFavoritesBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Фрагмент для відображення списку улюблених книг.
 *
 * Наслідує функціональність від [BaseBookListFragment], що надає спільну логіку
 * для відображення списків, сортування та пошуку. Цей клас лише надає
 * конкретну реалізацію для ViewModel, ViewBinding та навігації.
 */
@AndroidEntryPoint
class FavoritesFragment : BaseBookListFragment<FragmentFavoritesBinding, FavoritesViewModel>(
    // Передаємо функцію для інфлейту відповідного ViewBinding у конструктор базового класу
    FragmentFavoritesBinding::inflate
) {
    /**
     * Конкретна ViewModel для цього екрану, отримана за допомогою делегата `viewModels`.
     */
    override val viewModel: FavoritesViewModel by viewModels()

    /**
     * Реалізує абстрактний метод базового класу для налаштування UI.
     * Встановлює [BookAdapter] для `RecyclerView`.
     *
     * @param adapter Адаптер, який потрібно встановити для RecyclerView.
     */
    override fun setupUI(adapter: BookAdapter) {
        binding.recyclerViewBooks.adapter = adapter
    }

    /**
     * Реалізує абстрактний метод для оновлення стану UI, коли список порожній.
     * Показує або приховує текстове повідомлення та `RecyclerView`.
     *
     * @param isEmpty `true`, якщо список улюблених книг порожній.
     */
    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    /**
     * Реалізує навігацію до екрану деталей книги.
     * Використовує `Action` з Navigation Component, згенерований спеціально
     * для `FavoritesFragment`.
     *
     * @param bookId Ідентифікатор книги, до якої потрібно перейти.
     */
    override fun navigateToBookDetail(bookId: Int) {
        val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    /**
     * Реалізує навігацію до екрану вибору параметрів сортування.
     * Передає поточний порядок сортування та ідентифікатор поточного екрану ("FAVORITES")
     * як аргументи, щоб екран сортування міг відобразити відповідні опції.
     */
    override fun navigateToSortOptions() {
        val action = FavoritesFragmentDirections.actionFavoritesFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "FAVORITES" // Ідентифікатор для налаштування списку сортувань
        )
        findNavController().navigate(action)
    }
}