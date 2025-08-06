package com.example.bookdiarymobile.ui

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.databinding.FragmentReadBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReadFragment : BaseBookListFragment<FragmentReadBinding, ReadViewModel>(
    FragmentReadBinding::inflate
) {
    override val viewModel: ReadViewModel by viewModels()

    override fun setupUI(adapter: BookAdapter) {
        binding.recyclerViewBooks.adapter = adapter
    }

    override fun updateEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.isVisible = isEmpty
        binding.recyclerViewBooks.isVisible = !isEmpty
    }

    override fun navigateToBookDetail(bookId: Int) {
        val action = ReadFragmentDirections.actionReadFragmentToBookDetailFragment(bookId)
        findNavController().navigate(action)
    }

    override fun navigateToSortOptions() {
        val action = ReadFragmentDirections.actionReadFragmentToSortOptionsFragment(
            currentSortOrder = viewModel.sortOrder.value,
            sourceScreen = "READ"
        )
        findNavController().navigate(action)
    }
}