package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.SortOrder
import com.example.bookdiarymobile.databinding.FragmentFavoritesBinding
import com.example.bookdiarymobile.databinding.FragmentReadBinding
import com.example.bookdiarymobile.databinding.FragmentToReadBinding
import com.example.bookdiarymobile.utils.getSerializableCompat
import kotlinx.coroutines.launch

// Тип для інфлейтера біндінга, щоб зробити код ще чистішим
typealias Inflater<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

/**
 * Абстрактний базовий фрагмент для екранів зі списками книг.
 * Містить спільну логіку для UI, меню, сортування та пошуку.
 *
 * @param VB Тип ViewBinding, що використовується у фрагменті.
 * @param VM Тип ViewModel, що успадковується від BaseBookListViewModel.
 */
abstract class BaseBookListFragment<VB : ViewBinding, VM : BaseBookListViewModel>(
    private val bindingInflater: Inflater<VB>
) : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    protected abstract val viewModel: VM

    // Абстрактні методи, які дочірні класи повинні реалізувати для навігації
    protected abstract fun navigateToBookDetail(bookId: Int)
    protected abstract fun navigateToSortOptions()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingInflater.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BookAdapter { clickedBook ->
            navigateToBookDetail(clickedBook.id)
        }

        setupUI(adapter)

        setFragmentResultListener("SORT_REQUEST") { _, bundle ->
            val newSortOrder = bundle.getSerializableCompat<SortOrder>("SORT_ORDER")
            newSortOrder?.let { viewModel.applySortOrder(it) }
        }

        setupMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.books.collect { books ->
                    updateEmptyState(books.isEmpty())
                    adapter.submitList(books)
                }
            }
        }
    }

    // Дочірні класи нададуть реалізацію цих методів
    abstract fun setupUI(adapter: BookAdapter)
    abstract fun updateEmptyState(isEmpty: Boolean)

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                val currentQuery = viewModel.searchQuery.value
                if (currentQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView.setQuery(currentQuery, false)
                    searchView.clearFocus()
                }

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = true
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.applySearchQuery(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return if (menuItem.itemId == R.id.action_sort) {
                    navigateToSortOptions()
                    true
                } else {
                    false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}