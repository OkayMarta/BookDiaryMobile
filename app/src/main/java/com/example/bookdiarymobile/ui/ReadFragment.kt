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

class ReadFragment : Fragment(R.layout.fragment_read) {

    // Створюємо ViewModel за допомогою нашої фабрики
    private val viewModel: ReadViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_read)
        // Створюємо екземпляр нашого адаптера
        val adapter = BookAdapter()
        // Призначаємо адаптер для RecyclerView
        recyclerView.adapter = adapter

        // Запускаємо спостереження за даними з ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Коли ViewModel надасть новий список книг,
                // ми передаємо його в адаптер
                viewModel.readBooks.collect { books ->
                    adapter.submitList(books)
                }
            }
        }
    }
}