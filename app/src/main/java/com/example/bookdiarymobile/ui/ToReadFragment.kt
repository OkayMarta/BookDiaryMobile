package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.SortOrder
import com.example.bookdiarymobile.utils.getSerializableCompat
import kotlinx.coroutines.launch

class ToReadFragment : Fragment(R.layout.fragment_to_read) {

    private val viewModel: ToReadViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_to_read)
        val emptyStateTextView: TextView = view.findViewById(R.id.text_view_empty_state_to_read)

        // Створюємо адаптер, передаючи йому обробник кліку
        val adapter = BookAdapter { clickedBook ->
            // Створюємо дію для переходу, використовуючи згенерований клас ToReadFragmentDirections
            val action = ToReadFragmentDirections.actionToReadFragmentToBookDetailFragment(
                bookId = clickedBook.id
            )
            // Виконуємо перехід
            findNavController().navigate(action)
        }

        recyclerView.adapter = adapter

        setFragmentResultListener("SORT_REQUEST") { _, bundle ->
            val newSortOrder = bundle.getSerializableCompat<SortOrder>("SORT_ORDER")
            newSortOrder?.let { viewModel.applySortOrder(it) }
        }

        setupMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toReadBooks.collect { books ->
                    if (books.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyStateTextView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyStateTextView.visibility = View.GONE
                    }
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

                // Відновлюємо текст пошуку, якщо він був
                val currentQuery = viewModel.searchQuery.value
                if (currentQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView.setQuery(currentQuery, false)
                }

                searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true // Дія не потрібна, бо ми реагуємо на кожну зміну
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
                        val action = ToReadFragmentDirections.actionToReadFragmentToSortOptionsFragment(
                            currentSortOrder = viewModel.sortOrder.value,
                            sourceScreen = "TO_READ"
                        )
                        findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}