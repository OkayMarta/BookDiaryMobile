package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentReadBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Фрагмент для відображення списку прочитаних книг.
 *
 * Наслідує функціональність від [BaseBookListFragment], що надає спільну логіку
 * для відображення списків, сортування та пошуку. Цей клас лише надає
 * конкретну реалізацію для ViewModel, ViewBinding та навігації.
 */
@AndroidEntryPoint
class ReadFragment : BaseBookListFragment<FragmentReadBinding, ReadViewModel>(
    // Передаємо функцію для інфлейту відповідного ViewBinding у конструктор базового класу
    FragmentReadBinding::inflate
) {
    /**
     * Конкретна ViewModel для цього екрану, отримана за допомогою делегата `viewModels`.
     */
    override val viewModel: ReadViewModel by viewModels()

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
     * @param isEmpty `true`, якщо список прочитаних книг порожній.
     */
    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    /**
     * Реалізує навігацію до екрану деталей книги.
     * Використовує `Action` з Navigation Component, згенерований спеціально
     * для `ReadFragment`.
     *
     * @param bookId Ідентифікатор книги, до якої потрібно перейти.
     */
    override fun navigateToBookDetail(bookId: Int) {
        val action = ReadFragmentDirections.actionReadFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    /**
     * Реалізує навігацію до екрану вибору параметрів сортування.
     * Передає поточний порядок сортування та ідентифікатор поточного екрану ("READ")
     * як аргументи, щоб екран сортування міг відобразити відповідні опції.
     */
    override fun navigateToSortOptions() {
        val action = ReadFragmentDirections.actionReadFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "READ" // Ідентифікатор для налаштування списку сортувань
        )
        findNavController().navigate(action)
    }
}