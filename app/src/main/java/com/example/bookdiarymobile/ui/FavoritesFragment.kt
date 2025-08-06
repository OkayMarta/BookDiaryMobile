package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentFavoritesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoritesFragment : BaseBookListFragment<FragmentFavoritesBinding, FavoritesViewModel>(
    FragmentFavoritesBinding::inflate
) {
    override val viewModel: FavoritesViewModel by viewModels()

    override fun setupUI(adapter: BookAdapter) {
        binding.recyclerViewBooks.adapter = adapter
    }

    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    override fun navigateToBookDetail(bookId: Int) {
        val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    override fun navigateToSortOptions() {
        val action = FavoritesFragmentDirections.actionFavoritesFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "FAVORITES"
        )
        findNavController().navigate(action)
    }
}