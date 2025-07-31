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
}