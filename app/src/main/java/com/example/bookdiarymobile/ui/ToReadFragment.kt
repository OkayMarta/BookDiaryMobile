package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentToReadBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Фрагмент для відображення списку книг, запланованих до прочитання ("Хочу прочитати").
 *
 * Наслідує функціональність від [BaseBookListFragment], що надає спільну логіку
 * для відображення списків, сортування та пошуку. Цей клас лише надає
 * конкретну реалізацію для ViewModel, ViewBinding та навігації.
 */
@AndroidEntryPoint
class ToReadFragment : BaseBookListFragment<FragmentToReadBinding, ToReadViewModel>(
    // Передаємо функцію для інфлейту відповідного ViewBinding у конструктор базового класу
    FragmentToReadBinding::inflate
) {
    /**
     * Конкретна ViewModel для цього екрану, отримана за допомогою делегата `viewModels`.
     */
    override val viewModel: ToReadViewModel by viewModels()

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
     * @param isEmpty `true`, якщо список книг "Хочу прочитати" порожній.
     */
    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    /**
     * Реалізує навігацію до екрану деталей книги.
     * Використовує `Action` з Navigation Component, згенерований спеціально
     * для `ToReadFragment`.
     *
     * @param bookId Ідентифікатор книги, до якої потрібно перейти.
     */
    override fun navigateToBookDetail(bookId: Int) {
        val action = ToReadFragmentDirections.actionToReadFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    /**
     * Реалізує навігацію до екрану вибору параметрів сортування.
     * Передає поточний порядок сортування та ідентифікатор поточного екрану ("TO_READ")
     * як аргументи, щоб екран сортування міг відобразити відповідні опції.
     */
    override fun navigateToSortOptions() {
        val action = ToReadFragmentDirections.actionToReadFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "TO_READ" // Ідентифікатор для налаштування списку сортувань
        )
        findNavController().navigate(action)
    }
}