package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import kotlinx.coroutines.launch

class ReadFragment : Fragment(R.layout.fragment_read) {

    private val viewModel: ReadViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_read)
        val emptyStateTextView: TextView = view.findViewById(R.id.text_view_empty_state_read)

        // 1. Створюємо адаптер і передаємо йому функцію для обробки кліку
        val adapter = BookAdapter { clickedBook ->
            // 2. При кліку створюємо "дію" для переходу, передаючи ID книги
            // Клас ReadFragmentDirections генерується автоматично на основі nav_graph.xml
            val action = ReadFragmentDirections.actionReadFragmentToBookDetailFragment(
                bookId = clickedBook.id
            )
            // 3. Виконуємо навігацію
            findNavController().navigate(action)
        }

        recyclerView.adapter = adapter

        // Код для спостереження за даними залишається без змін
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.readBooks.collect { books ->
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
}