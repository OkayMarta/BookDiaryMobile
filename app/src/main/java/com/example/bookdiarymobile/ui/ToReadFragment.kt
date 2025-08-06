package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentToReadBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ToReadFragment : BaseBookListFragment<FragmentToReadBinding, ToReadViewModel>(
    FragmentToReadBinding::inflate
) {
    override val viewModel: ToReadViewModel by viewModels()

    override fun setupUI(adapter: BookAdapter) {
        binding.recyclerViewBooks.adapter = adapter
    }

    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    override fun navigateToBookDetail(bookId: Int) {
        val action = ToReadFragmentDirections.actionToReadFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    override fun navigateToSortOptions() {
        val action = ToReadFragmentDirections.actionToReadFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "TO_READ"
        )
        findNavController().navigate(action)
    }
}