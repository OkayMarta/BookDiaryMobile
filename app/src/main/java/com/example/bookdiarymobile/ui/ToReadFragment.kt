package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import kotlinx.coroutines.launch

class ToReadFragment : Fragment(R.layout.fragment_to_read) {

    // Створюємо ToReadViewModel за допомогою нашої фабрики
    private val viewModel: ToReadViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_to_read)
        // Створюємо екземпляр нашого адаптера (ми можемо перевикористовувати BookAdapter!)
        val adapter = BookAdapter()
        recyclerView.adapter = adapter

        // Запускаємо спостереження за даними з ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toReadBooks.collect { books ->
                    // Передаємо список книг в адаптер
                    adapter.submitList(books)
                }
            }
        }
    }
}