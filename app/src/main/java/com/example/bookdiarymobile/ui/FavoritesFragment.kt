package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.SortOrder
import com.example.bookdiarymobile.databinding.FragmentFavoritesBinding
import com.example.bookdiarymobile.utils.getSerializableCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BookAdapter { clickedBook ->
            val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookDetailFragment(
                bookId = clickedBook.id
            )
            findNavController().navigate(action)
        }

        // Використовуємо binding для доступу до RecyclerView
        binding.recyclerViewFavorites.adapter = adapter

        setFragmentResultListener("SORT_REQUEST") { _, bundle ->
            val newSortOrder = bundle.getSerializableCompat<SortOrder>("SORT_ORDER")
            newSortOrder?.let { viewModel.applySortOrder(it) }
        }

        setupMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.books.collect { books ->
                    // Використовуємо isVisible для керування видимістю
                    binding.recyclerViewFavorites.isVisible = books.isNotEmpty()
                    binding.textViewEmptyStateFavorites.isVisible = books.isEmpty()

                    adapter.submitList(books)
                }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

                val currentQuery = viewModel.searchQuery.value
                if (currentQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView.setQuery(currentQuery, false)
                }

                searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.applySearchQuery(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sort -> {
                        val action = FavoritesFragmentDirections.actionFavoritesFragmentToSortOptionsFragment(
                            currentSortOrder = viewModel.sortOrder.value,
                            sourceScreen = "FAVORITES"
                        )
                        findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищуємо binding
    }
}